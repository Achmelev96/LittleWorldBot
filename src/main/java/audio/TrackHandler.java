package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
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
        if (endReason.mayStartNext) {
            var started = nextTrack();
            if (started == null) {
                audio.MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player, java.time.Duration.ofHours(1));
            }
        } else {
            if (player.getPlayingTrack() == null && queue.isEmpty()) {
                audio.MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player, java.time.Duration.ofHours(1));
            }
        }
    }

    public boolean isQueueEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        audio.MusicCore.getInstance().cancelAfkDisconnectByPlayer(player);
    }


}
