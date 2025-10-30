package app;

import autocomplete.AutocompleteRouter;
import autocomplete.PlayQueryAutocomplete;
import commands.CommandRegistry;
import commands.PlayHandler;
import commands.SlashCommandRouter;
import config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");

        var registry = new CommandRegistry();
        registry.registerSlash("play", new PlayHandler());
        registry.register("play", "query", new PlayQueryAutocomplete());

        var slashRouter = new SlashCommandRouter(registry);
        var autoCompleteRouter = new AutocompleteRouter(registry);

        BotListener listener = new BotListener(slashRouter, autoCompleteRouter);

        //System.setProperty("lavaplayer.youtube.country", "US");
        //System.setProperty("http.agent", "Mozilla/5.0");

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(listener)
                .build();
    }
}