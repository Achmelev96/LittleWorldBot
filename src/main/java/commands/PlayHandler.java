package commands;

import audio.MusicCore;
import audio.TrackUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import interaction.CurrentStatus;
import commands.urlBuild.IdentifierBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PlayHandler extends BaseMusicCommand {

    @Override
    public String name() {
        return "play";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(false).queue();

        if (!isUserInVoice(status)) {
            event.getHook().editOriginal("А куда мне зайти?").queue();
            return;
        }

        if (canBotJoin(status)) {
            event.getHook().editOriginal("У меня нет прав, чтобы зайти в голосовой канал").queue();
            return;
        }

        var core = MusicCore.getInstance();
        var guild = status.guild();
        var guildHandler = core.getGuildHandler(guild);
        var audioManager = status.voice().audioManager();
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(guildHandler.getAudioSendHandler());
        }

        //var userChannel = status.voice().userChannel();
        //var botChannel = status.voice().botChannel();
        if (!isBotUserInSameChannel(status)) {
            if (!connectToUserVoice(status)) {
                event.getHook().editOriginal("Не удалось подключиться к голосовому каналу").queue();
            }
            /*try {
                audioManager.setSelfDeafened(false);
                audioManager.openAudioConnection(userChannel);
            } catch (Exception e) {
                event.getHook().editOriginal("Не удалось подключиться к голосовому каналу").queue();
                e.printStackTrace();
                return;
            }*/
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
                audio.MusicCore.getInstance().cancelAfkDisconnect(status.guild().getIdLong());

                String title = track.getInfo().title;
                String duration = TrackUtils.formatDuration(track.getInfo().length);
                event.getHook().editOriginal("Добавил в очередь: " + title + "** `" + duration + "`").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    AudioTrack first = playlist.getTracks().get(0);
                    guildHandler.getScheduler().queue(first);
                    audio.MusicCore.getInstance().cancelAfkDisconnect(status.guild().getIdLong());
                    event.getHook().editOriginal("Нашел: " + first.getInfo().title).queue();
                    return;
                }

                int trackCounter = 0;
                for (AudioTrack track : playlist.getTracks()) {
                    guildHandler.getScheduler().queue(track);
                    trackCounter++;
                }

                audio.MusicCore.getInstance().cancelAfkDisconnect(status.guild().getIdLong());
                event.getHook().editOriginal(
                        "Добавил плейлист: " + playlist.getName() + " (" + trackCounter + " треков)"
                ).queue();
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