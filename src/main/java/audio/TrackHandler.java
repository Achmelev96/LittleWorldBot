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

    /*public synchronized void pause() {
        queue.stop();
    }*/

    // for skip
    public synchronized void nextTrack() {
        AudioTrack track = queue.poll();
        player.startTrack(track, false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }
}
