package commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import audio.MusicCore;

public final class SkipHandler implements SlashCommand {

    public String name(){
        return "skip";
    }
    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {
        event.deferReply(true).queue();

        var botChannel = context.voice().botChannel();
        var userChannel = context.voice().userChannel();

        if (botChannel == null) {
            event.getHook().editOriginal("Я не в голосовом канале").queue();
            return;
        }

        if (userChannel == null) {
            event.getHook().editOriginal("Эту команду можно использовать только находять в голосовом канале").queue();
            return;
        }

        if (botChannel == null || botChannel.getIdLong() != userChannel.getIdLong()) {
            event.getHook().editOriginal("Для этого тебе нужно находиться со мной в одном канале").queue();
            return;
        }

        var guild     = context.guild();
        var core      = MusicCore.getInstance();
        var handler   = core.getGuildHandler(guild);
        var player    = handler.getPlayer();
        var scheduler = handler.getScheduler();

        AudioTrack currentTrack = player.getPlayingTrack();
        if (currentTrack == null) {
            event.getHook().editOriginal("Сейчас ничего не играет").queue();
            return;
        }

        String prevTitle = safeTitle(currentTrack);
        player.stopTrack();
        scheduler.nextTrack();

        AudioTrack queuedTrack = player.getPlayingTrack();
        if (queuedTrack == null) {
            MusicCore.getInstance().scheduleAfkDisconnect(context.guild().getIdLong(), java.time.Duration.ofHours(1));
            event.getHook().editOriginal("Пропущен: " + prevTitle + ". Очередь пуста").queue();
        } else {
            MusicCore.getInstance().scheduleAfkDisconnect(context.guild().getIdLong(), java.time.Duration.ofHours(1));
            event.getHook().editOriginal("Пропущен: " + prevTitle + ". → теперь играет: " +  queuedTrack.getInfo().title).queue();
        }
    }

    private static String safeTitle(AudioTrack t) {
        try {
            var info = t.getInfo();
            return info != null && info.title != null ? info.title : "неизвестный трек";
        } catch (Exception e) {
            return "неизвестный трек";
        }
    }
}
