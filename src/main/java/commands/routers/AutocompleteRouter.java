package commands.routers;

import commands.CommandRegistry;
import voice.CurrentStatus;
import localization.BotLanguage;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import settings.GuildLanguageService;

public class AutocompleteRouter {
    private final CommandRegistry registry;
    private final GuildLanguageService languageService;

    public AutocompleteRouter(CommandRegistry registry, GuildLanguageService languageService) {
        this.registry = registry;
        this.languageService = languageService;
    }

    public void route(CommandAutoCompleteInteractionEvent event) {
        var name = event.getName();
        var optionName = event.getFocusedOption().getName();
        BotLanguage language = event.getGuild() == null
                ? BotLanguage.ENGLISH
                : languageService.getLanguage(event.getGuild().getIdLong());
        var context = CurrentStatus.from(event, language);

        registry.findAutocomplete(name, optionName)
                .ifPresentOrElse(
                        provider -> provider.handle(event, context),
                        () -> event.replyChoices().queue()
                );
    }
}
