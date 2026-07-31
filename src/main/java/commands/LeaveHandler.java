package commands;

import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class LeaveHandler extends BaseMusicCommand {
    public LeaveHandler(MessageCatalog messages) {
        super(messages);
    }

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(true).queue();

        if (!isBotInVoice(status)) {
            event.getHook().editOriginal(messages.get(status.language(), "leave.not_connected")).queue();
            return;
        }

        try {
            core.getGuildHandler(status.guild()).getScheduler().stopAll();
            disconnectFromVoice(status);
            event.getHook().editOriginal(messages.get(status.language(), "leave.success")).queue();
        } catch (Exception error) {
            event.getHook().editOriginal(messages.get(status.language(), "common.error")).queue();
            error.printStackTrace();
        }
    }
}
