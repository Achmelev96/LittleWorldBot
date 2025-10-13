package app;

import events.CommandListener;
import events.CommandRegister;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Bot extends ListenerAdapter {


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        CommandRegister.registerCommand(event.getJDA());
        System.out.println("On Ready is completed");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "play" -> CommandListener.handlePlay(event);
            case "pause" -> CommandListener.handlePause(event);
            case "join" -> CommandListener.handleJoin(event);
            case "leave" -> CommandListener.handleLeave(event);
            default -> event.reply("Something went wrong").queue();
        }
    }
}
