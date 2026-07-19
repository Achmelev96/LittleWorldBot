package audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.time.Duration;

public final class PlayerControlService {

    private static final Duration AFK_TIMEOUT = Duration.ofHours(1);

    private PlayerControlService() {
    }

    public static SkipResult skip(GuildHandler handler) {
        var player = handler.getPlayer();
        var scheduler = handler.getScheduler();

        AudioTrack previous = player.getPlayingTrack();
        if (previous == null) {
            return new SkipResult(null, null);
        }

        player.stopTrack();
        AudioTrack next = scheduler.nextTrack();

        if (next == null) {
            MusicCore.getInstance().scheduleAfkDisconnect(handler.getGuild().getIdLong(), AFK_TIMEOUT);
        } else {
            MusicCore.getInstance().cancelAfkDisconnect(handler.getGuild().getIdLong());
        }

        return new SkipResult(previous, next);
    }

    public static boolean togglePause(GuildHandler handler) {
        var player = handler.getPlayer();
        boolean paused = !player.isPaused();
        player.setPaused(paused);
        return paused;
    }

    public record SkipResult(AudioTrack previous, AudioTrack current) {
        public boolean skipped() {
            return previous != null;
        }
    }
}