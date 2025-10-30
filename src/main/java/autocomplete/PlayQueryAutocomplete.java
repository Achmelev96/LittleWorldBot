package autocomplete;

import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class PlayQueryAutocomplete implements AutocompleteProvider {

    private static final int MAX_CHOICES = 25;

    @Override
    public boolean supports(String commandName, String optionName) {
        return "play".equalsIgnoreCase(commandName)
                && "query".equalsIgnoreCase(optionName);
    }

    @Override
    public void handle(CommandAutoCompleteInteractionEvent event, InteractionContext context) {
        String typed = Optional.ofNullable(event.getFocusedOption())
                .map(opt -> opt.getValue())
                .map(String::trim)
                .orElse("");

        List<String> base = new ArrayList<>(3);
        if (!typed.isEmpty()) {
            base.add(typed);
            base.add("ytsearch: " + typed);
            base.add("scsearch: " + typed);
        }

        var unique = new LinkedHashSet<String>(base);

        if (unique.isEmpty()) {
            event.replyChoices().queue();
            return;
        }

        List<String> limited = unique.stream().limit(MAX_CHOICES).toList();

        event.replyChoiceStrings(limited).queue();
    }
}
