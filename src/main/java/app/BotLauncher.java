package app;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import commands.publisher.CommandPublisher;
import commands.routers.AutocompleteRouter;
import commands.routers.SlashCommandRouter;
import config.Config;
import localization.MessageCatalog;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import settings.GuildLanguageService;
import settings.SqliteGuildSettingsRepository;

import java.nio.file.Path;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");
        var messages = new MessageCatalog();
        var settingsRepository = new SqliteGuildSettingsRepository(
                Path.of(Config.getOrDefault("DATABASE_PATH", "data/littleworldbot.db"))
        );
        var languageService = new GuildLanguageService(settingsRepository);

        var registry = CommandPublisher.buildRegistry(messages);
        var slashRouter = new SlashCommandRouter(registry, languageService, messages);
        var autoCompleteRouter = new AutocompleteRouter(registry, languageService);
        commands.MusicPanelHandler.getInstance().configure(messages, languageService);

        BotListener listener = new BotListener(slashRouter, autoCompleteRouter, messages);

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(listener)
                .setActivity(Activity.listening("/play"))
                .build();
    }
}