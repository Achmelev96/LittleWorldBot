package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class TrackHandler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    public TrackHandler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingDeque<>();
    }

    public synchronized void queue(AudioTrack track) {
        if (player.getPlayingTrack() == null && player.isPaused()) {
            player.setPaused(false);
        }
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        } else {
            System.out.println("[DEBUG] start: " + track.getInfo().title);
        }
    }

    public synchronized void clearQueue() {
        queue.clear();
    }

    public synchronized void stopAll() {
        queue.clear();
        player.stopTrack();
        player.setPaused(false);
    }

    // for skip
    public AudioTrack nextTrack() {
        var next = queue.poll();
        if (next == null) {
            player.stopTrack();
            return null;
        }
        player.startTrack(next, false);
        return next;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        System.out.println("[TrackHandler][end] reason=" + endReason
                + ", title=" + track.getInfo().title
                + ", uri=" + track.getInfo().uri);
        if (endReason.mayStartNext) {
            var started = nextTrack();
            if (started == null) {
                audio.MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
            }
        } else {
            if (player.getPlayingTrack() == null && queue.isEmpty()) {
                audio.MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
            }
        }
        audio.MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        System.err.println("[TrackHandler][exception] severity=" + exception.severity
                + ", title=" + track.getInfo().title
                + ", uri=" + track.getInfo().uri
                + ", position=" + track.getPosition());
        exception.printStackTrace();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        System.err.println("[TrackHandler][stuck] thresholdMs=" + thresholdMs
                + ", title=" + track.getInfo().title
                + ", uri=" + track.getInfo().uri
                + ", position=" + track.getPosition());
    }

    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        audio.MusicCore.getInstance().cancelAfkDisconnectByPlayer(player);
        audio.MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }
    // little change
}
