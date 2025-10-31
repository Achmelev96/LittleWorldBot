package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

public class MusicCore {

    private static final MusicCore INSTANCE = new MusicCore();
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildHandler> guildHandlers = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> afkTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService afkScheduler = Executors.newSingleThreadScheduledExecutor(run -> {
        Thread thread = new Thread(run, "afk-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private MusicCore(){
        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        this.playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.MEDIUM);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
    }

    public static MusicCore getInstance(){
        return INSTANCE;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public GuildHandler getGuildHandler(Guild guild) {
        return guildHandlers.computeIfAbsent(guild.getIdLong(),id -> {
            AudioPlayer player = playerManager.createPlayer();
            player.setVolume(100);
            GuildHandler handler = new GuildHandler(guild, player);
            player.addListener(handler.getScheduler());
            return handler;
        });
    }

    public void cancelAfkDisconnect(long guildId) {
        ScheduledFuture<?> afk = afkTasks.remove(guildId);
        if (afk != null) {
            afk.cancel(true);
        }
    }

    public void scheduleAfkDisconnect(long guildId, Duration timeout) {
        cancelAfkDisconnect(guildId);
        ScheduledFuture<?> future = afkScheduler.schedule(() -> {
            tryAfkLeave(guildId);
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        afkTasks.put(guildId, future);
    }

    public void cancelAfkDisconnectByPlayer(AudioPlayer p) {
        findGuildIdByPlayer(p).ifPresent(this::cancelAfkDisconnect);
    }

    public void scheduleAfkDisconnectByPlayer(AudioPlayer p, Duration timeout) {
        findGuildIdByPlayer(p).ifPresent(id -> scheduleAfkDisconnect(id, timeout));
    }

    private Optional<Long> findGuildIdByPlayer(AudioPlayer p) {
        for (Map.Entry<Long, GuildHandler> entry : guildHandlers.entrySet()) {
            GuildHandler guildHandler = entry.getValue();
            if (guildHandler != null && Objects.equals(guildHandler.getPlayer(), p)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean isIdle(long guildId) {
        GuildHandler guildHandler = guildHandlers.get(guildId);
        if (guildHandler == null) return true;
        boolean noPlaying = guildHandler.getPlayer().getPlayingTrack() == null;
        boolean queueEmpty = guildHandler.getScheduler().isQueueEmpty();
        return noPlaying && queueEmpty;
    }

    private void tryAfkLeave(long guildId) {
        afkTasks.remove(guildId);

        GuildHandler guildHandler = guildHandlers.get(guildId);
        if (guildHandler == null) return;

        if (!isIdle(guildId)) return;

        try {
            guildHandler.getScheduler().stopAll();
            guildHandler.getGuild().getAudioManager().setSendingHandler(null);
            guildHandler.getGuild().getAudioManager().closeAudioConnection();
        } catch (Exception ignored) {
        }
    }
}