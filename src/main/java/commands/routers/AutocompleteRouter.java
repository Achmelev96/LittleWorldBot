package commands.routers;

import commands.CommandRegistry;
import interaction.CurrentStatus;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;


public class AutocompleteRouter {

    private final CommandRegistry registry;

    public AutocompleteRouter(CommandRegistry registry){
        this.registry = registry;
    }

    public void route(CommandAutoCompleteInteractionEvent event) {
        var name = event.getName();
        var focused = event.getFocusedOption();
        var optionName = focused.getName();
        var context = CurrentStatus.from(event);

        registry.findAutocomplete(name, optionName)
                .ifPresentOrElse(
                        provider -> provider.handle(event, context),
                        () -> event.replyChoices().queue()
                );
    }
}
