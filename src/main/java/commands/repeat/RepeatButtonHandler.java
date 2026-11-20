package commands.repeat;

import localization.MessageCatalog;
import musicpanel.MusicPanelService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import voice.CurrentStatus;

public final class RepeatButtonHandler {
    private final RepeatUseCase repeatUseCase;
    private final MessageCatalog messages;
    private final MusicPanelService panelService;

    public RepeatButtonHandler(RepeatUseCase repeatUseCase, MessageCatalog messages, MusicPanelService panelService) {
        this.repeatUseCase = repeatUseCase;
        this.messages = messages;
        this.panelService = panelService;
    }

    public void handle(ButtonInteractionEvent event, CurrentStatus context) {
        event.deferEdit().queue(ignored -> {
            RepeatResult result = repeatUseCase.execute(context);
            if (result instanceof RepeatResult.Failure failure) {
                String key = switch (failure.reason()) {
                    case NOT_IN_SAME_CHANNEL -> "panel.same_channel_required";
                    case NOTHING_PLAYING -> "repeat.nothing_playing";
                };
                event.getHook().sendMessage(messages.get(context.language(), key)).setEphemeral(true).queue();
            }
            panelService.refresh(context.guild());
        });
    }
}