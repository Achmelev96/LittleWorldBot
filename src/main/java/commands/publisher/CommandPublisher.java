package commands.publisher;

import commands.CommandRegistry;
import commands.LeaveHandler;
import commands.PlayHandler;
import commands.SkipHandler;
import commands.autocomplete.PlayQueryAutocomplete;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandPublisher {

    public static CommandRegistry buildRegistry() {
        var registry = new CommandRegistry();
        registry.registerSlash("play", new PlayHandler());
        registry.registerSlash("leave", new LeaveHandler());
        registry.registerSlash("skip", new SkipHandler());
        registry.register("play", "query", new PlayQueryAutocomplete());
        return registry;
    }
    public static void publish(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("play", "Queue a track to play")
                        .addOption(OptionType.STRING, "query", "Song name, URL or Playlist", true, true),
                Commands.slash("skip", "instant transition to the next track in queue"),
                Commands.slash("leave", "Leave the voice channel")
        ).queue();
    }
}