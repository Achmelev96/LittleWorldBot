package commands.autocomplete;

import interaction.CurrentStatus;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public interface AutocompleteProvider {
    boolean supports(String commandName, String optionName);
    void handle(CommandAutoCompleteInteractionEvent event, CurrentStatus context);
}
