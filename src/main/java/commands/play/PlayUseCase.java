package commands.play;

import audio.MusicCore;
import audio.TrackUtils;
import audio.YtDlpResolvedTrack;
import audio.YtDlpResolver;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import commands.urlBuild.IdentifierBuilder;
import interaction.CurrentStatus;
import voice.VoiceConnectionService;
import voice.VoiceStateValidator;
import voice.VoiceValidationResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PlayUseCase {
    private final MusicCore musicCore;
    private final VoiceStateValidator voiceValidator;
    private final VoiceConnectionService voiceConnection;
    private final YtDlpResolver ytDlpResolver;

    public PlayUseCase(
            MusicCore musicCore,
            VoiceStateValidator voiceValidator,
            VoiceConnectionService voiceConnection,
            YtDlpResolver ytDlpResolver
    ) {
        this.musicCore = musicCore;
        this.voiceValidator = voiceValidator;
        this.voiceConnection = voiceConnection;
        this.ytDlpResolver = ytDlpResolver;
    }

    public CompletionStage<PlayResult> execute(CurrentStatus context, String rawQuery) {
        String identifier = IdentifierBuilder.build(rawQuery == null ? "" : rawQuery);
        if (identifier == null || identifier.isBlank()) {
            return CompletableFuture.completedFuture(
                    new PlayResult.Failure(PlayResult.FailureReason.EMPTY_QUERY)
            );
        }

        VoiceValidationResult userState = voiceValidator.requireUserInVoice(context);
        if (userState == VoiceValidationResult.GUILD_UNAVAILABLE) {
            return failure(PlayResult.FailureReason.GUILD_UNAVAILABLE);
        }
        if (userState != VoiceValidationResult.OK) {
            return failure(PlayResult.FailureReason.USER_NOT_IN_VOICE);
        }

        VoiceValidationResult permissionState = voiceValidator.requireJoinPermissions(context);
        if (permissionState == VoiceValidationResult.MISSING_PERMISSIONS) {
            return failure(PlayResult.FailureReason.MISSING_PERMISSIONS);
        }
        if (permissionState != VoiceValidationResult.OK) {
            return failure(PlayResult.FailureReason.GUILD_UNAVAILABLE);
        }

        var guild = context.guild();
        var guildHandler = musicCore.getGuildHandler(guild);
        voiceConnection.prepareAudioSender(context, guildHandler);

        if (voiceValidator.requireSameChannel(context) != VoiceValidationResult.OK
                && !voiceConnection.connectToUser(context)) {
            return failure(PlayResult.FailureReason.CONNECTION_FAILED);
        }

        if (ytDlpResolver.isConfigured() && isYoutubeIdentifier(identifier)) {
            return ytDlpResolver.resolve(identifier)
                    .thenCompose(resolved -> loadResolvedTrack(guild, guildHandler, resolved))
                    .exceptionally(error -> {
                        Throwable cause = unwrap(error);
                        System.err.println("[PlayUseCase][yt-dlp] " + cause.getMessage());
                        cause.printStackTrace();
                        scheduleAfkIfIdle(guild.getIdLong());
                        return new PlayResult.Failure(
                                PlayResult.FailureReason.LOAD_FAILED,
                                cause.getMessage()
                        );
                    });
        }
        return loadIdentifier(guild, guildHandler, identifier);
    }

    private CompletionStage<PlayResult> loadResolvedTrack(
            net.dv8tion.jda.api.entities.Guild guild,
            audio.GuildHandler guildHandler,
            YtDlpResolvedTrack resolved
    ) {
        AudioReference reference = new AudioReference(resolved.streamUrl(), resolved.title());
        return loadItem(guild, guildHandler, reference, resolved);
    }

    private CompletionStage<PlayResult> loadIdentifier(
            net.dv8tion.jda.api.entities.Guild guild,
            audio.GuildHandler guildHandler,
            String identifier
    ) {
        return loadItem(guild, guildHandler, identifier, null);
    }

    private CompletionStage<PlayResult> loadItem(
            net.dv8tion.jda.api.entities.Guild guild,
            audio.GuildHandler guildHandler,
            Object reference,
            YtDlpResolvedTrack resolved
    ) {
        CompletableFuture<PlayResult> result = new CompletableFuture<>();
        AudioLoadResultHandler handler = new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if (resolved != null) {
                    track.setUserData(resolved);
                }
                guildHandler.getScheduler().queue(track);
                musicCore.cancelAfkDisconnect(guild.getIdLong());
                String title = resolved == null ? track.getInfo().title : resolved.title();
                long duration = resolved == null ? track.getInfo().length : resolved.durationMs();
                result.complete(new PlayResult.TrackQueued(
                        title,
                        TrackUtils.formatDuration(duration)
                ));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    result.complete(new PlayResult.Failure(PlayResult.FailureReason.NO_MATCHES));
                    return;
                }

                if (playlist.isSearchResult()) {
                    AudioTrack first = playlist.getTracks().getFirst();
                    guildHandler.getScheduler().queue(first);
                    musicCore.cancelAfkDisconnect(guild.getIdLong());
                    result.complete(new PlayResult.SearchResultQueued(first.getInfo().title));
                    return;
                }

                int trackCount = 0;
                for (AudioTrack track : playlist.getTracks()) {
                    guildHandler.getScheduler().queue(track);
                    trackCount++;
                }

                musicCore.cancelAfkDisconnect(guild.getIdLong());
                result.complete(new PlayResult.PlaylistQueued(playlist.getName(), trackCount));
            }

            @Override
            public void noMatches() {
                result.complete(new PlayResult.Failure(PlayResult.FailureReason.NO_MATCHES));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.err.println("[PlayUseCase][loadFailed] severity=" + exception.severity);
                exception.printStackTrace();
                result.complete(new PlayResult.Failure(
                        PlayResult.FailureReason.LOAD_FAILED,
                        exception.getMessage()
                ));
            }
        };
        if (reference instanceof AudioReference audioReference) {
            musicCore.getPlayerManager().loadItemOrdered(guild, audioReference, handler);
        } else {
            musicCore.getPlayerManager().loadItemOrdered(guild, reference.toString(), handler);
        }
        return result;
    }

    private boolean isYoutubeIdentifier(String identifier) {
        return identifier.startsWith("ytsearch:")
                || identifier.contains("youtube.com/")
                || identifier.contains("youtu.be/");
    }

    private Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current;
    }

    private CompletionStage<PlayResult> failure(PlayResult.FailureReason reason) {
        return CompletableFuture.completedFuture(new PlayResult.Failure(reason));
    }
}
