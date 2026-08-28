package musicpanel;

import audio.GuildHandler;
import audio.MusicCore;
import localization.BotLanguage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import settings.GuildLanguageService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicPanelService {
    private final MusicCore musicCore;
    private final GuildLanguageService languageService;
    private final MusicPanelRenderer renderer;
    private final MusicPanelRepository repository;
    private final Map<Long, MusicPanelState> states = new ConcurrentHashMap<>();

    public MusicPanelService(
            MusicCore musicCore,
            GuildLanguageService languageService,
            MusicPanelRenderer renderer,
            MusicPanelRepository repository
    ) {
        this.musicCore = musicCore;
        this.languageService = languageService;
        this.renderer = renderer;
        this.repository = repository;
        musicCore.addPlaybackStateListener(this::refresh);
    }

    public void showOrMove(Guild guild, MessageChannel channel) {
        if (guild == null || channel == null) return;
        MusicPanelState state = states.computeIfAbsent(guild.getIdLong(), ignored -> new MusicPanelState());
        synchronized (state) {
            state.desiredChannel = channel;
            if (state.message != null) {
                state.moveToBottomRequested = true;
            }
        }
        reconcile(guild, state);
    }

    public void refresh(Guild guild) {
        if (guild == null) return;
        MusicPanelState state = states.get(guild.getIdLong());
        if (state != null) reconcile(guild, state);
    }

    public boolean isActive(long guildId, String token, long messageId) {
        MusicPanelState state = states.get(guildId);
        if (state == null) return false;
        synchronized (state) {
            return !state.deleting
                    && state.message != null
                    && state.message.getIdLong() == messageId
                    && state.token.equals(token);
        }
    }

    public void cleanupPersistedPanels(JDA jda) {
        for (StoredMusicPanel panel : repository.findAll()) {
            repository.delete(panel.guildId());
            MessageChannel channel = jda.getChannelById(MessageChannel.class, panel.channelId());
            if (channel == null) continue;
            channel.retrieveMessageById(panel.messageId()).queue(
                    message -> message.delete().queue(),
                    ignored -> { }
            );
        }
    }

    private void reconcile(Guild guild, MusicPanelState state) {
        GuildHandler handler = musicCore.getGuildHandler(guild);
        boolean hasTrack = handler.getPlayer().getPlayingTrack() != null;
        synchronized (state) {
            if (!hasTrack) {
                state.desiredChannel = null;
                state.moveToBottomRequested = false;
                deleteCurrent(guild, state);
                return;
            }
            if (state.deleting || state.creating) return;
            if (state.moveToBottomRequested && state.message != null) {
                if (state.editing) {
                    state.refreshPending = true;
                    return;
                }
                state.moveToBottomRequested = false;
                deleteCurrent(guild, state);
                return;
            }
            if (state.message != null && state.desiredChannel != null
                    && state.message.getChannel().getIdLong() != state.desiredChannel.getIdLong()) {
                deleteCurrent(guild, state);
                return;
            }
            if (state.message == null) {
                create(guild, state, handler);
                return;
            }
            edit(guild, state, handler);
        }
    }

    private void create(Guild guild, MusicPanelState state, GuildHandler handler) {
        MessageChannel channel = state.desiredChannel;
        if (channel == null) return;
        state.creating = true;
        state.moveToBottomRequested = false;
        String token = state.token;
        BotLanguage language = languageService.getLanguage(guild.getIdLong());
        channel.sendMessageEmbeds(renderer.buildEmbed(handler, language).build())
                .setComponents(renderer.buildControls(handler, token))
                .queue(message -> onCreated(guild, state, message), error -> {
                    System.err.println("[MusicPanel] Could not create panel for guild " + guild.getId());
                    error.printStackTrace();
                    synchronized (state) {
                        state.creating = false;
                    }
                    states.remove(guild.getIdLong(), state);
                });
    }

    private void onCreated(Guild guild, MusicPanelState state, Message message) {
        synchronized (state) {
            state.creating = false;
            state.message = message;
            repository.save(new StoredMusicPanel(guild.getIdLong(), message.getChannel().getIdLong(), message.getIdLong()));
        }
        reconcile(guild, state);
    }

    private void edit(Guild guild, MusicPanelState state, GuildHandler handler) {
        if (state.message == null) return;
        if (state.editing) {
            state.refreshPending = true;
            return;
        }
        state.editing = true;
        Message message = state.message;
        String token = state.token;
        BotLanguage language = languageService.getLanguage(guild.getIdLong());
        message.editMessageEmbeds(renderer.buildEmbed(handler, language).build())
                .setComponents(renderer.buildControls(handler, token))
                .queue(ignored -> {
                    boolean refreshAgain;
                    synchronized (state) {
                        state.editing = false;
                        refreshAgain = state.refreshPending;
                        state.refreshPending = false;
                    }
                    if (refreshAgain) reconcile(guild, state);
                }, error -> {
                    System.err.println("[MusicPanel] Could not update panel for guild " + guild.getId());
                    error.printStackTrace();
                    synchronized (state) {
                        state.editing = false;
                        state.refreshPending = false;
                        state.message = null;
                        state.invalidate();
                        repository.delete(guild.getIdLong());
                    }
                    reconcile(guild, state);
                });
    }

    private void deleteCurrent(Guild guild, MusicPanelState state) {
        if (state.editing) {
            state.refreshPending = true;
            return;
        }
        if (state.message == null || state.deleting) {
            if (state.message == null && !state.creating && state.desiredChannel == null) {
                states.remove(guild.getIdLong(), state);
                repository.delete(guild.getIdLong());
            }
            return;
        }
        state.deleting = true;
        state.invalidate();
        Message message = state.message;
        message.delete().queue(
                ignored -> onDeleted(guild, state),
                ignored -> onDeleted(guild, state)
        );
    }

    private void onDeleted(Guild guild, MusicPanelState state) {
        synchronized (state) {
            state.deleting = false;
            state.editing = false;
            state.message = null;
            repository.delete(guild.getIdLong());
        }
        if (state.desiredChannel == null) {
            states.remove(guild.getIdLong(), state);
        } else {
            reconcile(guild, state);
        }
    }
}
