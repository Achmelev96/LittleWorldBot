package commands;

import audio.TrackUtils;
import audio.PlayerControlService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import interaction.CurrentStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import audio.MusicCore;

public final class SkipHandler extends BaseMusicCommand {

    public String name(){
        return "skip";
    }

    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(false).queue();

        if (!isBotUserInSameChannel(status)) {
            event.getHook().editOriginal("Команды доступны только из голосового канала").queue();
            return;
        }

        var guild     = status.guild();
        var core      = MusicCore.getInstance();
        var handler   = core.getGuildHandler(guild);

        PlayerControlService.SkipResult result = PlayerControlService.skip(handler);
        AudioTrack currentTrack = result.previous();
        if (currentTrack == null) {
            event.getHook().editOriginal("Сейчас ничего не играет").queue();
            return;
        }

        String prevTitle = TrackUtils.safeTitle(currentTrack);
        AudioTrack queuedTrack = result.current();
        String duration = null;
        if (queuedTrack != null) {
            duration = TrackUtils.formatDuration(queuedTrack.getInfo().length);
        }

        MusicPanelHandler.getInstance().showOrUpdate(status.guild());

        if (queuedTrack == null) {
            event.getHook().editOriginal("Пропущен: " + prevTitle + ". Очередь пуста").queue();
        } else {
            event.getHook().editOriginal("Пропущен: " + prevTitle + ". → теперь играет: " +
                    queuedTrack.getInfo().title + "** `" + duration + "`").queue();
        }
    }
}
