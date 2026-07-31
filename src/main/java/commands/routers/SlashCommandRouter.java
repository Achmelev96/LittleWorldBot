package commands.routers;

import commands.CommandRegistry;
import voice.CurrentStatus;
import localization.BotLanguage;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import settings.GuildLanguageService;

public class SlashCommandRouter {
    private final CommandRegistry registry;
    private final GuildLanguageService languageService;
    private final MessageCatalog messages;

    public SlashCommandRouter(
            CommandRegistry registry,
            GuildLanguageService languageService,
            MessageCatalog messages
    ) {
        this.registry = registry;
        this.languageService = languageService;
        this.messages = messages;
    }

    public void route(SlashCommandInteractionEvent event) {
        var name = event.getName();
        BotLanguage language = event.getGuild() == null
                ? BotLanguage.ENGLISH
                : languageService.getLanguage(event.getGuild().getIdLong());
        var context = CurrentStatus.from(event, language);

        registry.getSlash(name).ifPresentOrElse(
                command -> command.handle(event, context),
                () -> event.reply(messages.get(language, "command.unknown", name))
                        .setEphemeral(true)
                        .queue()
        );
    }
}
