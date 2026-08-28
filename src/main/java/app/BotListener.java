package app;

import commands.routers.AutocompleteRouter;
import commands.routers.SlashCommandRouter;
import commands.publisher.CommandPublisher;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import musicpanel.MusicPanelInteractionHandler;
import musicpanel.MusicPanelService;

public class BotListener extends ListenerAdapter {

    private final SlashCommandRouter slashCommandRouter;
    private final AutocompleteRouter autocompleteRouter;
    private final CommandPublisher commandPublisher;
    private final MusicPanelInteractionHandler panelInteractionHandler;
    private final MusicPanelService panelService;

    public BotListener(
            SlashCommandRouter slashCommandRouter,
            AutocompleteRouter autocompleteRouter,
            CommandPublisher commandPublisher,
            MusicPanelInteractionHandler panelInteractionHandler,
            MusicPanelService panelService
    ) {
        this.slashCommandRouter = slashCommandRouter;
        this.autocompleteRouter = autocompleteRouter;
        this.commandPublisher = commandPublisher;
        this.panelInteractionHandler = panelInteractionHandler;
        this.panelService = panelService;
    }

    public void onReady(ReadyEvent event) {
        panelService.cleanupPersistedPanels(event.getJDA());
        commandPublisher.publish(event.getJDA());
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        slashCommandRouter.route(event);
    }

    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        autocompleteRouter.route(event);
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        panelInteractionHandler.handle(event);
    }

    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        panelInteractionHandler.handle(event);
    }
}
