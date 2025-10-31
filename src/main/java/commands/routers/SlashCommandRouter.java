package commands.routers;

import commands.CommandRegistry;
import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SlashCommandRouter {
    private final CommandRegistry registry;

    public SlashCommandRouter(CommandRegistry registry) {
        this.registry = registry;
    }

    public void route(SlashCommandInteractionEvent event) {
        var name = event.getName();
        var context = InteractionContext.from(event);

        var opt = registry.getSlash(name);
        if (opt.isPresent()) {
            opt.get().handle(event, context);
        } else {
            event.reply("Неизвестная команда: " + name).setEphemeral(true).queue();
        }
    }
}
