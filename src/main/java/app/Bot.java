package app;

import events.CommandRegister;
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
        System.out.println("On Ready выполнен");
    }
}
