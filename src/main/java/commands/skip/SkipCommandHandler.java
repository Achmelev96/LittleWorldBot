package commands.skip;

import commands.SlashCommand;
import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class SkipCommandHandler implements SlashCommand {
    private final SkipUseCase skipUseCase;
    private final MessageCatalog messages;

    public SkipCommandHandler(SkipUseCase skipUseCase, MessageCatalog messages) {
        this.skipUseCase = skipUseCase;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus context) {
        event.deferReply(false).queue();
        SkipResult result = skipUseCase.execute(context);
        event.getHook().editOriginal(messageFor(context, result)).queue();
    }

    private String messageFor(CurrentStatus context, SkipResult result) {
        return switch (result) {
            case SkipResult.SkippedQueueEmpty skipped -> messages.get(
                    context.language(), "skip.queue_empty", skipped.previousTitle()
            );
            case SkipResult.SkippedNowPlaying skipped -> messages.get(
                    context.language(),
                    "skip.now_playing",
                    skipped.previousTitle(),
                    skipped.currentTitle(),
                    skipped.currentDuration()
            );
            case SkipResult.Failure failure -> switch (failure.reason()) {
                case NOT_IN_SAME_CHANNEL -> messages.get(
                        context.language(), "skip.same_channel_required"
                );
                case NOTHING_PLAYING -> messages.get(context.language(), "skip.nothing_playing");
            };
        };
    }
}
