package commands;

import audio.MusicCore;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import interaction.InteractionContext;
import commands.urlBuild.IdentifierBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.Duration;

public final class PlayHandler implements SlashCommand {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlayHandler.class);

    @Override
    public String name() {
        return "play";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {
        event.deferReply(false).queue();

        var userChannel = context.voice().userChannel();
        if (userChannel == null) {
            event.getHook().editOriginal("А куда мне зайти?").queue();
            return;
        }

        var core = MusicCore.getInstance();
        var guild = context.guild();
        var guildHandler = core.getGuildHandler(guild);

        var audioManager = context.voice().audioManager();
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(guildHandler.getAudioSendHandler());
        }

        var botChannel = context.voice().botChannel();
        if (botChannel == null || botChannel.getIdLong() != userChannel.getIdLong()) {
            try {
                audioManager.setSelfDeafened(false);
                audioManager.openAudioConnection(userChannel);
            } catch (Exception e) {
                event.getHook().editOriginal("Не удалось подключиться к голосовому каналу").queue();
                e.printStackTrace();
                return;
            }
        }

        var rawQuery = event.getOption("query").getAsString();

        //log
        log.info("[play] rawQuery='{}' user={} guild={}", rawQuery, context.user().getId(), context.guild().getId());

        var identifier = IdentifierBuilder.build(rawQuery);

        //log
        log.info("[play] identifier='{}' (built from rawQuery)", identifier);

        if (identifier == null || identifier.isBlank()) {
            event.getHook().editOriginal("Пустой запрос").queue();
            return;
        }

        // +log
        log.info("[play] loadItemOrdered guild={} identifier='{}'", guild.getId(), identifier);
        core.getPlayerManager().loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                //log
                var info = track.getInfo();
                log.info("[play][result] TrackLoaded title='{}' author='{}' uri='{}' lengthMs={}",
                        info.title, info.author, info.uri, info.length);

                guildHandler.getScheduler().queue(track);
                audio.MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
                event.getHook().editOriginal("Добавил в очередь: " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                //log
                log.info("[play][result] PlaylistLoaded name='{}' tracks={}", playlist.getName(), playlist.getTracks().size());

                AudioTrack selected = playlist.getSelectedTrack();
                if (selected == null && !playlist.getTracks().isEmpty()) {
                    selected = playlist.getTracks().get(0);
                }

                if (selected != null) {
                    //log
                    var info = selected.getInfo();
                    log.info("[play][result] PlaylistSelected title='{}' uri='{}'", info.title, info.uri);

                    guildHandler.getScheduler().queue(selected);
                    audio.MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
                    event.getHook().editOriginal("Добавил из плейлиста: " + selected.getInfo().title).queue();
                } else {
                    for (AudioTrack track : playlist.getTracks()) {
                        guildHandler.getScheduler().queue(track);
                    }
                    audio.MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
                    event.getHook().editOriginal(
                            "Добавил плейлист: " + playlist.getName() + " (" + playlist.getTracks().size() + " треков)"
                    ).queue();
                }
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal("Ничего не нашел по твоему запросу").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal("Ошибка загрузки: " + exception.getMessage()).queue();
                System.err.println("[PlayHandler][loadFailed] severity=" + exception.severity);
                System.err.println("[PlayHandler][loadFailed] message=" + exception.getMessage());
                if (exception.getCause() != null) {
                    System.err.println("[PlayHandler][loadFailed] cause=" +
                            exception.getCause().getClass().getName() + " : " + exception.getCause().getMessage());
                }
                exception.printStackTrace();
            }
        });
    }
}