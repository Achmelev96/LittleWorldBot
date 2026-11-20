package audio.playback;

import audio.GuildHandler;
import audio.MusicCore;

public final class PlayerControlService {

    private PlayerControlService() {
    }

    public static TrackHandler.SkipResult skip(GuildHandler handler) {
        var result = handler.getScheduler().skip(handler.getPlayer().getPlayingTrack());
        if (!result.skipped()) return result;

        if (result.current() == null) {
            MusicCore.getInstance().scheduleAfkDisconnect(handler.getGuild().getIdLong());
        } else {
            MusicCore.getInstance().cancelAfkDisconnect(handler.getGuild().getIdLong());
        }

        return result;
    }

    public static boolean togglePause(GuildHandler handler) {
        var player = handler.getPlayer();
        boolean paused = !player.isPaused();
        player.setPaused(paused);
        if (paused) {
            MusicCore.getInstance().scheduleAfkDisconnect(handler.getGuild().getIdLong());
        } else {
            MusicCore.getInstance().cancelAfkDisconnect(handler.getGuild().getIdLong());
        }
        return paused;
    }

}
