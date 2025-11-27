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

        if (!isUserInVoice(status)) {
            event.getHook().editOriginal("А куда зайти?").queue();
            return;
        }

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
    }
}
