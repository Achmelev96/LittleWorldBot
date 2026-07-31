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
import settings.SettingsAccessPolicy;
import audio.MusicCore;
import musicpanel.MusicPanelInteractionHandler;
import musicpanel.MusicPanelRenderer;
import musicpanel.MusicPanelService;
import musicpanel.SqliteMusicPanelRepository;

import java.nio.file.Path;

public class BotLauncher {
    public static void main(String[] args) {

        String token = Config.get("DISCORD_TOKEN");
        var messages = new MessageCatalog();
        Path databasePath = Path.of(Config.getOrDefault("DATABASE_PATH", "data/littleworldbot.db"));
        var settingsRepository = new SqliteGuildSettingsRepository(databasePath);
        var languageService = new GuildLanguageService(settingsRepository);
        var musicCore = MusicCore.getInstance();
        var panelRepository = new SqliteMusicPanelRepository(databasePath);
        var panelRenderer = new MusicPanelRenderer(messages);
        var panelService = new MusicPanelService(musicCore, languageService, panelRenderer, panelRepository);
        var settingsAccess = new SettingsAccessPolicy(parseLong(Config.getOrDefault("BOT_OWNER_ID", "0")));
        var panelInteractions = new MusicPanelInteractionHandler(
                musicCore, panelService, languageService, settingsAccess, messages
        );

        var registry = CommandPublisher.buildRegistry(messages, panelService);
        var slashRouter = new SlashCommandRouter(registry, languageService, messages);
        var autoCompleteRouter = new AutocompleteRouter(registry, languageService);
        BotListener listener = new BotListener(
                slashRouter, autoCompleteRouter, messages, panelInteractions, panelService
        );

        JDABuilder.createDefault(token, GatewayIntent.GUILD_VOICE_STATES)
                .setStatus(OnlineStatus.ONLINE)
                .setAudioModuleConfig(new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory()))
                .addEventListeners(listener)
                .setActivity(Activity.listening("/play"))
                .build();
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
