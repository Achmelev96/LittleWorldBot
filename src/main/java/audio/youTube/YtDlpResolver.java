package audio.youTube;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class YtDlpResolver {
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final String PRINT_TEMPLATE = String.join(
            FIELD_SEPARATOR,
            "%(track)s",
            "%(artist)s",
            "%(title)s",
            "%(uploader)s",
            "%(duration)s",
            "%(webpage_url)s",
            "%(thumbnail)s",
            "%(url)s"
    );

    private final Path executable;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "yt-dlp-resolver");
        thread.setDaemon(true);
        return thread;
    });

    public YtDlpResolver(String executablePath, Duration timeout) {
        this.executable = executablePath == null || executablePath.isBlank()
                ? null
                : Path.of(executablePath.trim()).toAbsolutePath().normalize();
        this.timeout = timeout;
    }

    public boolean isConfigured() {
        return executable != null;
    }

    public CompletionStage<YtDlpResolvedTrack> resolve(String identifier) {
        return CompletableFuture.supplyAsync(() -> resolveBlocking(identifier), executor);
    }

    public CompletionStage<AudioPlaylist> resolvePlaylist(String identifier, AudioPlayerManager playerManager) {
        return CompletableFuture.supplyAsync(() -> {
            validateExecutable();
            String output = run(List.of(
                    executable.toString(),
                    "--encoding", "utf-8",
                    "--yes-playlist",
                    "--flat-playlist",
                    "--skip-download",
                    "--no-warnings",
                    "--no-progress",
                    "--dump-single-json",
                    "--", identifier
            ));
            DataObject playlist = DataObject.fromJson(output);
            var entries = playlist.getArray("entries");
            List<AudioTrack> tracks = new ArrayList<>();
            for (int index = 0; index < entries.length(); index++) {
                if (entries.isNull(index)) continue;

                DataObject entry = entries.getObject(index);
                String id = entry.getString("id", "");
                if (!id.matches("[A-Za-z0-9_-]{11}")) continue;

                String url = "https://youtube.com/watch?v=" + id;
                AudioTrackInfo info = new AudioTrackInfo(
                        entry.getString("title", id),
                        entry.getString("uploader", "Unknown artist"),
                        Math.round(entry.getDouble("duration", 0) * 1_000),
                        url, false, url
                );
                tracks.add(new YtDlpPlaylistTrack(info, this, playerManager));
            }
            return new BasicAudioPlaylist(playlist.getString("title", "YouTube"), tracks, null, false);
        }, executor);
    }

    YtDlpResolvedTrack resolveBlocking(String identifier) {
        validateExecutable();
        String ytDlpIdentifier = identifier.startsWith("ytsearch:")
                ? "ytsearch1:" + identifier.substring("ytsearch:".length())
                : identifier;
        List<String> command = List.of(
                executable.toString(),
                "--encoding", "utf-8",
                "--no-playlist",
                "--no-warnings",
                "--no-progress",
                "--format", "bestaudio[acodec=opus]/bestaudio",
                "--print", PRINT_TEMPLATE,
                "--", ytDlpIdentifier
        );

        return parseOutput(run(command));
    }

    private String run(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException error) {
            throw new YtDlpException("Could not start yt-dlp", error);
        }

        CompletableFuture<String> stdout = readOutput(process.getInputStream());
        CompletableFuture<String> stderr = readOutput(process.getErrorStream());
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new YtDlpException("yt-dlp timed out after " + timeout.toSeconds() + " seconds");
            }
            String standardOutput = stdout.join().trim();
            String errorOutput = stderr.join().trim();
            if (process.exitValue() != 0) {
                throw new YtDlpException("yt-dlp exited with code " + process.exitValue()
                        + formatDetails(errorOutput));
            }
            return standardOutput;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new YtDlpException("yt-dlp was interrupted", error);
        }
    }

    private CompletableFuture<String> readOutput(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException error) {
                throw new YtDlpException("Could not read yt-dlp output", error);
            }
        }, executor);
    }

    private YtDlpResolvedTrack parseOutput(String output) {
        String[] fields = output.split(FIELD_SEPARATOR, 8);
        if (fields.length != 8 || fields[7].isBlank()) {
            throw new YtDlpException("yt-dlp returned an unexpected response");
        }
        String title = firstAvailable(fields[0], fields[2], "Unknown track");
        String author = firstAvailable(fields[1], fields[3], null);
        long durationMs = parseDuration(fields[4]);
        return new YtDlpResolvedTrack(
                title,
                author,
                durationMs,
                nullIfUnavailable(fields[5]),
                nullIfUnavailable(fields[6]),
                fields[7]
        );
    }

    private String firstAvailable(String primary, String fallback, String defaultValue) {
        String selected = nullIfUnavailable(primary);
        if (selected != null) return selected;
        selected = nullIfUnavailable(fallback);
        return selected == null ? defaultValue : selected;
    }

    private String nullIfUnavailable(String value) {
        return value == null || value.isBlank() || "NA".equals(value) ? null : value;
    }

    private long parseDuration(String seconds) {
        try {
            return Math.round(Double.parseDouble(seconds) * 1000.0);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void validateExecutable() {
        if (executable == null) {
            throw new YtDlpException("YT_DLP_PATH is not configured");
        }
        if (!Files.isRegularFile(executable)) {
            throw new YtDlpException("yt-dlp executable was not found at " + executable);
        }
    }

    private String formatDetails(String details) {
        return details.isBlank() ? "" : ": " + details;
    }

    public static final class YtDlpException extends RuntimeException {
        public YtDlpException(String message) {
            super(message);
        }

        public YtDlpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
