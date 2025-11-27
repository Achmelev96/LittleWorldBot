package commands;

import interaction.CurrentStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class JoinHandler extends BaseMusicCommand {

    @Override
    public String name() {
        return "join";
    }

    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(true).queue();

        //var guild = status.guild();
        //var self = guild != null ? guild.getSelfMember() : null;
        //var userChannel = status.voice().userChannel();
        if (!isUserInVoice(status)) {
            event.getHook().editOriginal("А куда зайти?").queue();
            return;
        }

        //var botChannel = status.voice().botChannel();
        if (isBotUserInSameChannel(status)) {
            event.getHook().editOriginal("Уже тут").queue();
            return;
        }

        if (canBotJoin(status)) {
            event.getHook().editOriginal("У меня нету прав на подключение к этому каналу").queue();
            return;
        }

        if  (!connectToUserVoice(status)) {
            event.getHook().editOriginal("Не могу подключиться к тебе").queue();
            return;
        } else {
            event.getHook().editOriginal("Захожу к тебе").queue();
        }
        /*
        try {
            var audioManager = status.voice().audioManager();
            audioManager.setSelfDeafened(false);
            audioManager.openAudioConnection(userChannel);
            audio.MusicCore.getInstance().cancelAfkDisconnect(status.guild().getIdLong());
            event.getHook().editOriginal("Захожу к тебе").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Не могу подключиться к тебе").queue();
            e.printStackTrace();
        }*/
    }
}
