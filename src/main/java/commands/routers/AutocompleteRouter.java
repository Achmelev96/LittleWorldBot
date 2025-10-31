package commands.routers;

import commands.CommandRegistry;
import commands.autocomplete.AutocompleteProvider;
import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.List;

public class AutocompleteRouter {

    private final CommandRegistry registry;
    private final List<AutocompleteProvider> providers;

    public AutocompleteRouter(CommandRegistry registry, List<AutocompleteProvider> providers) {
        this.registry = registry;
        this.providers = providers;
    }

    public void route(CommandAutoCompleteInteractionEvent event) {
        var name = event.getName();
        var focused = event.getFocusedOption();
        var optionName = focused.getName();
        var context = InteractionContext.from(event);

        for (var entry : providers) {
            if (entry.supports(name, optionName)) {
                entry.handle(event, context);
                return;
            }
        }

        event.replyChoices().queue();
    }
}
