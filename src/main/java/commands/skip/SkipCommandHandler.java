package commands.skip;

import commands.SlashCommand;
import voice.CurrentStatus;
import localization.MessageCatalog;
import musicpanel.MusicPanelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class SkipCommandHandler implements SlashCommand {
    private final SkipUseCase skipUseCase;
    private final MessageCatalog messages;
    private final MusicPanelService panelService;

    public SkipCommandHandler(
            SkipUseCase skipUseCase,
            MessageCatalog messages,
            MusicPanelService panelService
    ) {
        this.skipUseCase = skipUseCase;
        this.messages = messages;
        this.panelService = panelService;
    }

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus context) {
        event.deferReply(true).queue(ignored -> execute(event, context));
    }

    private void execute(SlashCommandInteractionEvent event, CurrentStatus context) {
        SkipResult result = skipUseCase.execute(context);
        event.getHook().editOriginal(messageFor(context, result)).queue();
        if (!(result instanceof SkipResult.Failure)) {
            panelService.refresh(context.guild());
        }
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
