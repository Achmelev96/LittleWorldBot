package musicpanel;

import audio.GuildHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.awt.Color;

public final class MusicPanelRenderer {
    private static final Color PANEL_COLOR = new Color(88, 101, 242);
    private final MessageCatalog messages;

    public MusicPanelRenderer(MessageCatalog messages) {
        this.messages = messages;
    }

    public EmbedBuilder buildEmbed(GuildHandler handler, BotLanguage language) {
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        AudioTrackInfo info = track.getInfo();
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(PANEL_COLOR)
                .setTitle(safeTitle(info.title, language));

        if (info.author != null && !info.author.isBlank()) {
            embed.setAuthor(info.author);
        }
        String thumbnail = thumbnailUrl(info.uri);
        if (thumbnail != null) {
            embed.setThumbnail(thumbnail);
        }
        return embed;
    }

    public ActionRow buildControls(GuildHandler handler, String token) {
        String prefix = "music:" + token + ":";
        Button playPause = handler.getPlayer().isPaused()
                ? Button.success(prefix + "toggle", "▶ Play")
                : Button.primary(prefix + "toggle", "⏸ Pause");
        return ActionRow.of(
                playPause,
                Button.secondary(prefix + "skip", "⏭ Skip"),
                Button.secondary(prefix + "menu", "☰")
        );
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
