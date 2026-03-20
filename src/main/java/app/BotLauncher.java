package app;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import commands.publisher.CommandPublisher;
import commands.routers.AutocompleteRouter;
import commands.routers.SlashCommandRouter;
import config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");

        var registry = CommandPublisher.buildRegistry();
        var slashRouter = new SlashCommandRouter(registry);
        var autoCompleteRouter = new AutocompleteRouter(registry);

        BotListener listener = new BotListener(slashRouter, autoCompleteRouter);

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(listener)
                .setActivity(Activity.listening("/play"))
                .build();
    }
}