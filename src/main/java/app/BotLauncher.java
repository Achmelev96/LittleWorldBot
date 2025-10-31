package app;

import commands.routers.AutocompleteRouter;
import commands.autocomplete.PlayQueryAutocomplete;
import commands.*;
import commands.routers.SlashCommandRouter;
import config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import commands.autocomplete.AutocompleteProvider;

import java.util.List;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");

        var registry = new CommandRegistry();
        registry.registerSlash("play", new PlayHandler());
        registry.registerSlash("leave", new LeaveHandler());
        registry.registerSlash("join", new JoinHandler());
        registry.registerSlash("skip", new SkipHandler());
        registry.register("play", "query", new PlayQueryAutocomplete());

        var slashRouter = new SlashCommandRouter(registry);
        List<AutocompleteProvider> providers = List.of(
                new PlayQueryAutocomplete());
        var autoCompleteRouter = new AutocompleteRouter(registry, providers);

        BotListener listener = new BotListener(slashRouter, autoCompleteRouter);

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(listener)
                .setActivity(Activity.watching("за своим манямирком"))
                .build();
    }
}