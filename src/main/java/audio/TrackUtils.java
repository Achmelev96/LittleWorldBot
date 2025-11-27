package audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class TrackUtils {

    public static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0)
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%d:%02d", minutes, seconds);
    }

    public static String safeTitle(AudioTrack t) {
        try {
            var info = t.getInfo();
            return info != null && info.title != null ? info.title : "неизвестный трек";
        } catch (Exception e) {
            return "неизвестный трек";
        }
    }
}
