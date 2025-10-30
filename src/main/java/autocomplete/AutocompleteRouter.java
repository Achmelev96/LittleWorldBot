package autocomplete;

import commands.CommandRegistry;
import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public class AutocompleteRouter {

    private final CommandRegistry registry;

    public AutocompleteRouter(CommandRegistry registry) {
        this.registry = registry;
    }

    public void route(CommandAutoCompleteInteractionEvent event) {
        if (event.getFocusedOption() == null) {
            event.replyChoices().queue();
            return;
        }

        String commandName = event.getName();
        String optionName  = event.getFocusedOption().getName();

        var providerOpt = registry.findAutocomplete(commandName, optionName);
        if (providerOpt.isEmpty()) {
            event.replyChoices().queue();
            return;
        }

        var context = InteractionContext.from(event);

        try {
            providerOpt.get().handle(event, context);
        } catch (Exception e) {
            event.replyChoices().queue();
        }
    }
}
