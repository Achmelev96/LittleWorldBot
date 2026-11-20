package musicpanel;

import audio.playback.PlaylistWindow;
import audio.playback.RepeatMode;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MusicPanelRendererTest {
    private final MusicPanelRenderer renderer = new MusicPanelRenderer(new MessageCatalog());
    private final PlaylistWindow playlist = new PlaylistWindow("playlist-id", 0, 0, List.of("track"));

    private Button button(ActionRow row, String action) {
        return row.getComponents().stream().map(Button.class::cast)
                .filter(button -> button.getCustomId().endsWith(":" + action)).findFirst().orElseThrow();
    }

    @Test
    public void repeatUsesSameTextIconWithThreeDifferentColors() {
        for (RepeatMode mode : RepeatMode.values()) {
            var rows = renderer.buildControls(false, "token", playlist, null, mode, BotLanguage.ENGLISH);
            var repeat = button(rows.getFirst(), "repeat");
            assertEquals("↻", repeat.getLabel()); assertNull(repeat.getEmoji());
            assertEquals(switch (mode) {
                case OFF -> ButtonStyle.SECONDARY;
                case ONCE -> ButtonStyle.PRIMARY;
                case LOOP -> ButtonStyle.SUCCESS;
            }, repeat.getStyle());
        }
    }

    @Test
    public void listOpensNavigationAndSkipPlaylistTogetherAndChangesTriangle() {
        var closed = renderer.buildControls(false, "token", playlist, null, RepeatMode.OFF, BotLanguage.ENGLISH);
        assertEquals(1, closed.size());
        assertEquals(5, closed.getFirst().getComponents().size());
        assertEquals("▾", button(closed.getFirst(), "list").getLabel());
        var open = renderer.buildControls(false, "token", playlist, playlist.playlistId(), RepeatMode.ONCE, BotLanguage.RUSSIAN);
        assertEquals(3, open.size());
        assertEquals("▴", button(open.getFirst(), "list").getLabel());
        var skip = (Button) open.get(2).getComponents().getFirst();
        assertEquals("Skip playlist", skip.getLabel());
        assertTrue(skip.getCustomId().endsWith(playlist.playlistId()));
    }

    @Test
    public void onlyPlayPauseIconsChangeAndSinglesHaveNoPlaylistControls() {
        var playing = renderer.buildControls(false, "token", null, null, RepeatMode.LOOP, BotLanguage.ENGLISH);
        var paused = renderer.buildControls(true, "token", null, null, RepeatMode.LOOP, BotLanguage.ENGLISH);
        assertEquals(1, playing.size()); assertEquals(4, playing.getFirst().getComponents().size());
        for (String action : List.of("toggle", "skip", "repeat")) {
            assertFalse(button(playing.getFirst(), action).getLabel().isBlank());
            assertNull(button(playing.getFirst(), action).getEmoji());
        }
        assertEquals("⏸\uFE0E", button(playing.getFirst(), "toggle").getLabel());
        assertEquals("▶\uFE0E", button(paused.getFirst(), "toggle").getLabel());
        assertEquals("⏭\uFE0E", button(playing.getFirst(), "skip").getLabel());
        assertEquals("☰", button(playing.getFirst(), "menu").getLabel());
    }
}