package musicpanel;

import audio.playback.PlaylistWindow;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;

final class PlaylistMenu {
    private static final int MAX_LABEL_LENGTH = 100;

    private PlaylistMenu() {}

    static ActionRow build(String componentId, PlaylistWindow window, BotLanguage language, MessageCatalog messages) {
        StringSelectMenu.Builder menu = StringSelectMenu.create(componentId)
                .setPlaceholder(messages.get(language, "panel.playlist_placeholder"));
        for (int offset = 0; offset < window.titles().size(); offset++) {
            int index = window.firstIndex() + offset;
            boolean current = index == window.currentIndex();
            String title = window.titles().get(offset);
            if (title.isBlank()) title = messages.get(language, "panel.unknown_track");
            String label = (current ? "🟡 " : "") + (index + 1) + ". " + title;
            if (label.length() > MAX_LABEL_LENGTH) {
                int end = MAX_LABEL_LENGTH - 1;
                if (Character.isHighSurrogate(label.charAt(end - 1))) end--;
                label = label.substring(0, end) + "…";
            }

            String value = window.playlistId() + ":" + index;
            if (current) {
                menu.addOption(label, value, messages.get(language, "panel.playlist_current"));
                menu.setDefaultValues(value);
            } else {
                menu.addOption(label, value);
            }
        }
        return ActionRow.of(menu.build());
    }
}
