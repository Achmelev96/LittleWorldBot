package audio.playback;

import audio.MusicCore;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TrackHandler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final PlaybackQueue queue = new PlaybackQueue();

    private AudioTrack skipTarget;
    private AudioTrack failedSkipTarget;

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
        skipTarget = null;
        failedSkipTarget = null;
    }

    public synchronized void stopAll() {
        clearQueue();
        player.stopTrack();
        player.setPaused(false);
    }

    public synchronized AudioTrack nextTrack() {
        skipTarget = null;
        failedSkipTarget = null;
        AudioTrack next = queue.advance();
        player.startTrack(next, false);
        return next;
    }

    public synchronized SkipResult skip(AudioTrack expected) {
        AudioTrack previous = expected == null ? failedSkipTarget : expected;
        if (previous == null || !queue.isCurrent(previous)) {
            return new SkipResult(null, null);
        }

        AudioTrack next = queue.advance();
        failedSkipTarget = null;
        skipTarget = queue.isPlaylistTrack(next) ? next : null;
        player.startTrack(next, false);
        return new SkipResult(previous, next);
    }

    public synchronized boolean skipPlaylist(String playlistId) {
        if (!queue.discardPlaylist(playlistId, player.getPlayingTrack())) return false;

        nextTrack();
        if (player.isPaused()) player.setPaused(false);
        return true;
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

        skipTarget = null;
        failedSkipTarget = null;
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
            CompletableFuture.runAsync(() -> advanceAfterEnd(track, endReason));
            return;
        }
        if (player.getPlayingTrack() == null && queue.isEmpty()) {
            MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
        }
        MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
    }

    private synchronized void advanceAfterEnd(AudioTrack ended, AudioTrackEndReason reason) {
        if (player.getPlayingTrack() != null) return;

        if (queue.isCurrent(ended)) {
            if (ended == skipTarget && reason == AudioTrackEndReason.LOAD_FAILED) {
                // A manual skip should attempt just one playlist entry.
                skipTarget = null;
                failedSkipTarget = ended;
            } else if (nextTrack() != null) {
                return;
            }
        }

        if (queue.isEmpty()) {
            MusicCore.getInstance().scheduleAfkDisconnectByPlayer(player);
        }
        MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);
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
