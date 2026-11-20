package audio.playback;

import audio.MusicCore;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TrackHandler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final PlaybackQueue queue = new PlaybackQueue();

    public TrackHandler(AudioPlayer player) {
        this.player = player;
    }

    public synchronized void queue(AudioTrack track) {
        queue.add(track);
        startIfIdle();
    }

    public synchronized void queuePlaylist(List<AudioTrack> tracks) {
        queue.addPlaylist(tracks);
        startIfIdle();
    }

    private void startIfIdle() {
        if (player.getPlayingTrack() == null && !queue.isEmpty()) {
            if (player.isPaused()) player.setPaused(false);
            nextTrack();
        }
    }

    public synchronized void clearQueue() {
        queue.clear();
    }

    public synchronized void stopAll() {
        clearQueue();
        player.stopTrack();
        player.setPaused(false);
    }

    public synchronized AudioTrack nextTrack() {
        AudioTrack next = queue.advance();
        player.startTrack(next, false);
        return next;
    }

    public synchronized SkipResult skip(AudioTrack expected) {
        AudioTrack previous = expected;
        if (previous == null || !queue.isCurrent(previous)) {
            return new SkipResult(null, null);
        }

        AudioTrack next = queue.skip();
        player.startTrack(next, false);
        return new SkipResult(previous, next);
    }

    public synchronized boolean skipPlaylist(String playlistId) {
        if (!queue.discardPlaylist(playlistId, player.getPlayingTrack())) return false;

        nextTrack();
        if (player.isPaused()) player.setPaused(false);
        return true;
    }

    public RepeatMode getRepeatMode() {
        return queue.repeatMode();
    }

    public synchronized RepeatMode cycleRepeat() {
        return queue.cycleRepeat(player.getPlayingTrack());
    }

    public record SkipResult(AudioTrack previous, AudioTrack current) {
        public boolean skipped() {
            return previous != null;
        }
    }

    public PlaylistWindow playlistWindow(AudioTrack playing) {
        return queue.window(playing);
    }

    public synchronized boolean jumpToPlaylistTrack(String playlistId, int index) {
        AudioTrack playing = player.getPlayingTrack();
        PlaylistWindow window = queue.window(playing);
        if (window != null && window.playlistId().equals(playlistId) && index == window.currentIndex()) {
            return true;
        }

        AudioTrack next = queue.jump(window, playlistId, index);
        if (next == null) return false;

        player.startTrack(next, false);
        if (player.isPaused()) player.setPaused(false);
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        System.out.println("[TrackHandler][end] reason=" + endReason
                + ", title=" + track.getInfo().title
                + ", uri=" + track.getInfo().uri);
        if (endReason.mayStartNext) {
            // LavaPlayer can dispatch this event while holding its track switch lock.
            boolean failed = failedToLoad(track, endReason);
            CompletableFuture.runAsync(() -> advanceAfterEnd(track, failed));
            return;
        }
        if (player.getPlayingTrack() == null && queue.isEmpty()) {
            MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
        }
        MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }

    private synchronized void advanceAfterEnd(AudioTrack ended, boolean failed) {
        if (player.getPlayingTrack() != null) return;

        if (queue.isCurrent(ended)) {
            AudioTrack next = failed
                    ? queue.retryOrAdvance() : queue.finish();
            player.startTrack(next, false);
            if (next != null) return;
        }

        if (queue.isEmpty()) {
            MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
        }
        MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }

    private boolean failedToLoad(AudioTrack track, AudioTrackEndReason reason) {
        if (reason == AudioTrackEndReason.LOAD_FAILED) return true;
        if (track instanceof InternalAudioTrack internal && internal.getActiveExecutor() != null) {
            // LavaPlayer can publish FINISHED before reporting a loading exception.
            var buffer = internal.getActiveExecutor().getAudioBuffer();
            return buffer != null && !buffer.hasReceivedFrames();
        }
        return false;
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

    public int getQueueSize() {
        return queue.size();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        MusicCore.getInstance().cancelAfkDisconnectByPlayer(player);
        MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }
    // little change
}
