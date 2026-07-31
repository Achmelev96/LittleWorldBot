package commands;

import audio.TrackUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import commands.urlBuild.IdentifierBuilder;
import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PlayHandler extends BaseMusicCommand {
    public PlayHandler(MessageCatalog messages) {
        super(messages);
    }

    @Override
    public String name() {
        return "play";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(false).queue();

        if (!isUserInVoice(status)) {
            event.getHook().editOriginal(messages.get(status.language(), "voice.user_not_connected")).queue();
            return;
        }

        if (lacksVoicePermissions(status)) {
            event.getHook().editOriginal(messages.get(status.language(), "voice.missing_permissions")).queue();
            return;
        }

        var guild = status.guild();
        var guildHandler = core.getGuildHandler(guild);
        var audioManager = status.audioManager();
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(guildHandler.getAudioSendHandler());
        }

        if (!isBotUserInSameChannel(status) && !connectToUserVoice(status)) {
            event.getHook().editOriginal(messages.get(status.language(), "voice.connection_failed")).queue();
            return;
        }

        MusicPanelHandler.getInstance().rememberChannel(guild, event.getChannel());

        var queryOption = event.getOption("query");
        var rawQuery = queryOption == null ? "" : queryOption.getAsString();
        var identifier = IdentifierBuilder.build(rawQuery);
        if (identifier == null || identifier.isBlank()) {
            event.getHook().editOriginal(messages.get(status.language(), "play.empty_query")).queue();
            return;
        }

        core.getPlayerManager().loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildHandler.getScheduler().queue(track);
                core.cancelAfkDisconnect(guild.getIdLong());
                MusicPanelHandler.getInstance().showOrUpdate(guild);

                event.getHook().editOriginal(messages.get(
                        status.language(),
                        "play.track_queued",
                        track.getInfo().title,
                        TrackUtils.formatDuration(track.getInfo().length)
                )).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack first = playlist.getTracks().getFirst();
                    guildHandler.getScheduler().queue(first);
                    core.cancelAfkDisconnect(guild.getIdLong());
                    MusicPanelHandler.getInstance().showOrUpdate(guild);
                    event.getHook().editOriginal(messages.get(
                            status.language(), "play.search_found", first.getInfo().title
                    )).queue();
                    return;
                }

                int trackCount = 0;
                for (AudioTrack track : playlist.getTracks()) {
                    guildHandler.getScheduler().queue(track);
                    trackCount++;
                }

                core.cancelAfkDisconnect(guild.getIdLong());
                MusicPanelHandler.getInstance().showOrUpdate(guild);
                event.getHook().editOriginal(messages.get(
                        status.language(), "play.playlist_queued", playlist.getName(), trackCount
                )).queue();
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal(messages.get(status.language(), "play.no_matches")).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal(messages.get(
                        status.language(), "play.load_failed", exception.getMessage()
                )).queue();
                System.err.println("[PlayHandler][loadFailed] severity=" + exception.severity);
                exception.printStackTrace();
            }
        });
    }
}
