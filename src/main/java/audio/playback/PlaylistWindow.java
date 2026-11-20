package audio.playback;

import java.util.List;

public record PlaylistWindow(String playlistId, int firstIndex, int currentIndex, List<String> titles) {
    boolean contains(int index) {
        return index >= firstIndex && index - firstIndex < titles.size();
    }
}
