package audio.playback;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

final class PlaybackQueue {
    private final BlockingDeque<Entry> pending = new LinkedBlockingDeque<>();
    private volatile Entry current;
    private volatile RepeatMode repeatMode = RepeatMode.OFF;
    private int loadRetries;

    void add(AudioTrack track) {
        pending.addLast(new Entry(track, null, 0));
    }

    void addPlaylist(List<AudioTrack> tracks) {
        Playlist playlist = new Playlist(UUID.randomUUID().toString(), List.copyOf(tracks));
        for (int index = 0; index < tracks.size(); index++) {
            pending.addLast(new Entry(tracks.get(index), playlist, index));
        }
    }

    AudioTrack advance() {
        loadRetries = 0;
        current = pending.pollFirst();
        return current == null ? null : current.track();
    }

    RepeatMode repeatMode() {
        return repeatMode;
    }

    RepeatMode cycleRepeat(AudioTrack playing) {
        if (playing == null || !isCurrent(playing)) return null;
        repeatMode = repeatMode.next();
        return repeatMode;
    }

    AudioTrack finish() {
        if (current == null) return advance();
        if (repeatMode == RepeatMode.LOOP) {
            loadRetries = 0;
            return restartCurrent();
        }
        return advanceAfterCurrent();
    }

    AudioTrack skip() {
        if (repeatMode == RepeatMode.LOOP || current == null || current.playlist() == null) {
            repeatMode = RepeatMode.OFF;
        }
        return advanceAfterCurrent();
    }

    AudioTrack retryOrAdvance() {
        if (current == null) return advance();
        if (loadRetries == 0) {
            loadRetries++;
            return restartCurrent();
        }
        if (repeatMode == RepeatMode.LOOP || current.playlist() == null) {
            repeatMode = RepeatMode.OFF;
        }
        return advanceAfterCurrent();
    }

    private AudioTrack restartCurrent() {
        current = new Entry(current.track().makeClone(), current.playlist(), current.index());
        return current.track();
    }

    private AudioTrack advanceAfterCurrent() {
        Entry entry = current;
        if (repeatMode == RepeatMode.ONCE && entry != null
                && (entry.playlist() == null || entry.index() == entry.playlist().tracks().size() - 1)) {
            repeatMode = RepeatMode.OFF;
            if (entry.playlist() == null) {
                pending.addFirst(new Entry(entry.track().makeClone(), null, 0));
            } else {
                Playlist playlist = entry.playlist();
                for (int index = playlist.tracks().size() - 1; index >= 0; index--) {
                    pending.addFirst(new Entry(playlist.tracks().get(index).makeClone(), playlist, index));
                }
            }
        }
        return advance();
    }

    boolean isCurrent(AudioTrack track) {
        Entry entry = current;
        return entry != null && entry.track() == track;
    }

    boolean isPlaylistTrack(AudioTrack track) {
        Entry entry = current;
        return entry != null && entry.track() == track && entry.playlist() != null;
    }

    PlaylistWindow window(AudioTrack playing) {
        Entry entry = current;
        return entry != null && entry.track() == playing && entry.playlist() != null
                ? entry.playlist().window(entry.index()) : null;
    }

    AudioTrack jump(PlaylistWindow window, String playlistId, int target) {
        if (window == null || !window.playlistId().equals(playlistId)
                || target == window.currentIndex() || !window.contains(target)) {
            return null;
        }

        Entry entry = current;
        Playlist playlist = entry.playlist();
        if (target < entry.index()) {
            List<Entry> replay = new ArrayList<>();
            for (int index = target; index <= entry.index(); index++) {
                replay.add(new Entry(playlist.tracks().get(index).makeClone(), playlist, index));
            }
            for (Entry restored : replay.reversed()) {
                pending.addFirst(restored);
            }
        } else {
            for (int index = entry.index() + 1; index < target; index++) {
                pending.removeFirst();
            }
        }
        return advance();
    }

    boolean discardPlaylist(String playlistId, AudioTrack playing) {
        Entry entry = current;
        if (entry == null || entry.track() != playing || entry.playlist() == null
                || !entry.playlist().id().equals(playlistId)) {
            return false;
        }
        repeatMode = RepeatMode.OFF;
        pending.removeIf(item -> item.playlist() == entry.playlist());
        return true;
    }

    void clear() {
        pending.clear();
        current = null;
        repeatMode = RepeatMode.OFF;
        loadRetries = 0;
    }

    boolean isEmpty() {
        return pending.isEmpty();
    }

    int size() {
        return pending.size();
    }

    private record Entry(AudioTrack track, Playlist playlist, int index) {}

    private record Playlist(String id, List<AudioTrack> tracks) {
        PlaylistWindow window(int currentIndex) {
            int first = Math.max(0, currentIndex - 4);
            int end = Math.min(tracks.size(), currentIndex + 5);
            List<String> titles = tracks.subList(first, end).stream()
                    .map(track -> track.getInfo().title == null ? "" : track.getInfo().title)
                    .toList();
            return new PlaylistWindow(id, first, currentIndex, titles);
        }
    }
}
