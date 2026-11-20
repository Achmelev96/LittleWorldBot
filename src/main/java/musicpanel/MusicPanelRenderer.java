package musicpanel;

import audio.playback.PlaylistWindow;
import audio.youTube.YtDlpResolvedTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class MusicPanelRenderer {
    private static final Color PANEL_COLOR = new Color(88, 101, 242);
    private final MessageCatalog messages;

    public MusicPanelRenderer(MessageCatalog messages) {
        this.messages = messages;
    }

    public EmbedBuilder buildEmbed(AudioTrack track, BotLanguage language) {
        AudioTrackInfo info = track.getInfo();
        YtDlpResolvedTrack resolved = track.getUserData(YtDlpResolvedTrack.class);
        String title = resolved == null ? info.title : resolved.title();
        String author = resolved == null ? info.author : resolved.author();
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(PANEL_COLOR)
                .setTitle(safeTitle(title, language));

        if (author != null && !author.isBlank()) {
            embed.setAuthor(author);
        }
        String thumbnail = resolved == null ? null : resolved.thumbnailUrl();
        if (thumbnail == null && resolved != null) {
            thumbnail = thumbnailUrl(resolved.webpageUrl());
        }
        if (thumbnail == null) {
            thumbnail = thumbnailUrl(info.uri);
        }
        if (thumbnail != null) {
            embed.setThumbnail(thumbnail);
        }
        return embed;
    }

    public List<ActionRow> buildControls(
            boolean paused, String token, PlaylistWindow playlist, String openPlaylistId, BotLanguage language
    ) {
        String prefix = "music:" + token + ":";
        List<Button> buttons = new ArrayList<>();
        buttons.add(paused
                ? Button.success(prefix + "toggle", "▶ Play")
                : Button.primary(prefix + "toggle", "⏸ Pause"));
        buttons.add(Button.secondary(prefix + "skip", "⏭ Skip"));
        if (playlist != null) {
            buttons.add(Button.secondary(prefix + "list", "List"));
        }
        buttons.add(Button.secondary(prefix + "menu", "☰"));

        ActionRow controls = ActionRow.of(buttons);
        if (playlist != null && playlist.playlistId().equals(openPlaylistId)) {
            return List.of(
                    controls,
                    PlaylistMenu.build(prefix + "playlist", playlist, language, messages),
                    ActionRow.of(Button.secondary(prefix + "skip-playlist:" + playlist.playlistId(), "Skip playlist"))
            );
        }
        return List.of(controls);
    }

    private String safeTitle(String title, BotLanguage language) {
        return title == null || title.isBlank()
                ? messages.get(language, "panel.unknown_track")
                : title;
    }

    private String thumbnailUrl(String uri) {
        if (uri == null) return null;
        String id = null;
        int watch = uri.indexOf("watch?v=");
        if (watch >= 0) {
            id = uri.substring(watch + 8).split("[&?]")[0];
        } else if (uri.contains("youtu.be/")) {
            id = uri.substring(uri.indexOf("youtu.be/") + 9).split("[&?]")[0];
        }
        return id == null || id.isBlank() ? null : "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
    }
}
