package commands;

import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SkipHandler implements SlashCommand {

    public String name(){
        return "skip";
    }
    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {

    };
}
