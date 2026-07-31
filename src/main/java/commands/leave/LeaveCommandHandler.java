package commands.leave;

import commands.SlashCommand;
import voice.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class LeaveCommandHandler implements SlashCommand {
    private final LeaveUseCase leaveUseCase;
    private final MessageCatalog messages;

    public LeaveCommandHandler(LeaveUseCase leaveUseCase, MessageCatalog messages) {
        this.leaveUseCase = leaveUseCase;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus context) {
        event.deferReply(true).queue(ignored -> execute(event, context));
    }

    private void execute(SlashCommandInteractionEvent event, CurrentStatus context) {
        LeaveResult result = leaveUseCase.execute(context);
        String message = switch (result) {
            case SUCCESS -> messages.get(context.language(), "leave.success");
            case BOT_NOT_IN_VOICE -> messages.get(context.language(), "leave.not_connected");
            case FAILED -> messages.get(context.language(), "common.error");
        };
        event.getHook().editOriginal(message).queue();
    }
}
