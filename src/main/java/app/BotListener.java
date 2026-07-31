package app;

import commands.routers.AutocompleteRouter;
import commands.routers.SlashCommandRouter;
import commands.publisher.CommandPublisher;
import commands.MusicPanelHandler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
//import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import localization.MessageCatalog;

public class BotListener extends ListenerAdapter {

    private final SlashCommandRouter slashCommandRouter;
    private final AutocompleteRouter autocompleteRouter;
    private final MessageCatalog messages;

    public BotListener(
            SlashCommandRouter slashCommandRouter,
            AutocompleteRouter autocompleteRouter,
            MessageCatalog messages
    ) {
        this.slashCommandRouter = slashCommandRouter;
        this.autocompleteRouter = autocompleteRouter;
        this.messages = messages;
    }

    /*public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
    }*/

    public void onReady(ReadyEvent event) {
        CommandPublisher.publish(event.getJDA(), messages);
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        slashCommandRouter.route(event);
    }

    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        autocompleteRouter.route(event);
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        MusicPanelHandler.getInstance().handle(event);
    }
}
