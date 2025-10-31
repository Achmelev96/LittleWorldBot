package commands;

import audio.MusicCore;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import interaction.InteractionContext;
import commands.urlBuild.IdentifierBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PlayHandler implements SlashCommand{

    @Override
    public String name() {
        return "play";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {
        event.deferReply(false).queue();

        var userChannel = context.voice().userChannel();
        if(userChannel == null) {
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
        var identifier = IdentifierBuilder.build(rawQuery);
        if (identifier == null || identifier.isBlank()) {
            event.getHook().editOriginal("Пустой запрос").queue();
            return;
        }

        core.getPlayerManager().loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildHandler.getScheduler().queue(track);
                event.getHook().editOriginal("Добавил в очередь: " +  track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack selected = playlist.getSelectedTrack();
                if (selected != null || !playlist.getTracks().isEmpty()) {
                    selected = playlist.getTracks().get(0);
                }

                if (selected != null) {
                    guildHandler.getScheduler().queue(selected);
                    event.getHook().editOriginal("Добавил из плейлиста: " + selected.getInfo().title).queue();
                } else {
                    for (AudioTrack track : playlist.getTracks()) {
                        guildHandler.getScheduler().queue(track);
                    }
                    event.getHook().editOriginal(
                            "Добавил плейлись: " + playlist.getName() + " (" + playlist.getTracks().size() + " треков)"
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
