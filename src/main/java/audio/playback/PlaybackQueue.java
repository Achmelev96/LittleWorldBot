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
        current = pending.pollFirst();
        return current == null ? null : current.track();
    }

    boolean isCurrent(AudioTrack track) {
        Entry entry = current;
        return entry != null && entry.track() == track;
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

    void clear() {
        pending.clear();
        current = null;
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
