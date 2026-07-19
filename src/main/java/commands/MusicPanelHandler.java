package commands;

import audio.GuildHandler;
import audio.MusicCore;
import audio.PlayerControlService;
import audio.TrackUtils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MusicPanelHandler {

    private static final MusicPanelHandler INSTANCE = new MusicPanelHandler();
    private static final String PLAY_PAUSE_ID = "music:play_pause";
    private static final String SKIP_ID = "music:skip";
    private static final int BAR_LENGTH = 18;

    private final Map<Long, PanelState> panels = new ConcurrentHashMap<>();
    private final ScheduledExecutorService updater = Executors.newSingleThreadScheduledExecutor(run -> {
        Thread thread = new Thread(run, "music-panel-updater");
        thread.setDaemon(true);
        return thread;
    });

    private MusicPanelHandler() {
        updater.scheduleAtFixedRate(this::refreshPanels, 1, 1, TimeUnit.SECONDS);
    }

    public static MusicPanelHandler getInstance() {
        return INSTANCE;
    }

    public void rememberChannel(Guild guild, MessageChannel channel) {
        if (guild == null || channel == null) return;
        panels.compute(guild.getIdLong(), (id, state) -> {
            if (state == null) return new PanelState(channel, null);
            state.channel = channel;
            return state;
        });
    }

    public void showOrUpdate(Guild guild) {
        if (guild == null) return;
        PanelState state = panels.get(guild.getIdLong());
        if (state == null || state.channel == null) return;

        GuildHandler handler = MusicCore.getInstance().getGuildHandler(guild);
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        if (track == null) {
            deleteExisting(guild.getIdLong(), state);
            return;
        }

        if (state.message == null) {
            if (state.creating) return;
            state.creating = true;
            state.channel.sendMessageEmbeds(buildEmbed(handler).build())
                    .setComponents(ActionRow.of(
                            buildPlayPauseButton(handler),
                            Button.secondary(SKIP_ID, "⏭ Skip")
                    ))
                    .queue(message -> {
                        state.message = message;
                        state.creating = false;
                    }, error -> {
                        state.creating = false;
                        panels.remove(guild.getIdLong());
                    });
        } else {
            editExisting(guild.getIdLong(), state, handler);
        }
    }

    public void handle(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!PLAY_PAUSE_ID.equals(componentId) && !SKIP_ID.equals(componentId)) return;

        event.deferEdit().queue();

        Guild guild = event.getGuild();
        if (guild == null) return;

        GuildHandler handler = MusicCore.getInstance().getGuildHandler(guild);
        if (PLAY_PAUSE_ID.equals(componentId)) {
            if (handler.getPlayer().getPlayingTrack() != null) {
                PlayerControlService.togglePause(handler);
            }
        } else {
            PlayerControlService.skip(handler);
        }
        showOrUpdate(guild);
    }

    private void refreshPanels() {
        panels.forEach((guildId, state) -> {
            if (state.message == null) return;
            Guild guild = state.message.getGuild();
            GuildHandler handler = MusicCore.getInstance().getGuildHandler(guild);
            if (handler.getPlayer().getPlayingTrack() == null) {
                deleteExisting(guildId, state);
                return;
            }
            editExisting(guildId, state, handler);
        });
    }

    private void editExisting(long guildId, PanelState state, GuildHandler handler) {
        if (state.message == null) return;
        state.message.editMessageEmbeds(buildEmbed(handler).build())
                .setComponents(ActionRow.of(
                        buildPlayPauseButton(handler),
                        Button.secondary(SKIP_ID, "⏭ Skip")
                ))
                .queue(null, error -> panels.remove(guildId));
    }

    private void deleteExisting(long guildId, PanelState state) {
        if (state.message == null) return;
        Message message = state.message;
        state.message = null;
        state.creating = false;
        message.delete().queue(null, error -> panels.remove(guildId));
    }

    private Button buildPlayPauseButton(GuildHandler handler) {
        if (handler.getPlayer().isPaused()) {
            return Button.success(PLAY_PAUSE_ID, "▶ Play");
        }
        return Button.primary(PLAY_PAUSE_ID, "⏸ Pause");
    }

    private EmbedBuilder buildEmbed(GuildHandler handler) {
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        AudioTrackInfo info = track.getInfo();
        long position = track.getPosition();
        long length = info.length;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(88, 101, 242))
                .setTitle(safe(info.title))
                .setDescription(buildDescription(info, position, length));

        if (info.author != null && !info.author.isBlank()) {
            embed.setAuthor(info.author);
        }
        String thumbnail = thumbnailUrl(info);
        if (thumbnail != null) {
            embed.setThumbnail(thumbnail);
        }
        return embed;
    }

    private String buildDescription(AudioTrackInfo info, long position, long length) {
        return "`" + TrackUtils.formatDuration(position) + "` " + progressBar(position, length) + " `" + TrackUtils.formatDuration(length) + "`";
    }

    private String progressBar(long position, long length) {
        if (length <= 0) return "●" + "─".repeat(BAR_LENGTH - 1);
        int marker = (int) Math.min(BAR_LENGTH - 1, Math.max(0, Math.round((position / (float) length) * (BAR_LENGTH - 1))));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar.append(i == marker ? '●' : (i < marker ? '━' : '─'));
        }
        return bar.toString();
    }

    private String thumbnailUrl(AudioTrackInfo info) {
        String uri = info.uri;
        if (uri == null) return null;
        String id = null;
        int watch = uri.indexOf("watch?v=");
        if (watch >= 0) {
            id = uri.substring(watch + 8).split("[&?]")[0];
        } else if (uri.contains("youtu.be/")) {
            id = uri.substring(uri.indexOf("youtu.be/") + 9).split("[&?]")[0];
        }
        if (id == null || id.isBlank()) return null;
        return "https://img.youtube.com/vi/" + id + "/hqdefault.jpg";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Неизвестный трек" : value;
    }

    private static final class PanelState {
        private MessageChannel channel;
        private Message message;
        private boolean creating;

        private PanelState(MessageChannel channel, Message message) {
            this.channel = channel;
            this.message = message;
        }
    }
}
