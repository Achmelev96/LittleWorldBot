package app;

import audio.MusicCore;
import commands.publisher.CommandPublisher;
import commands.publisher.CommandRegistryFactory;
import commands.routers.AutocompleteRouter;
import commands.routers.SlashCommandRouter;
import config.Config;
import localization.MessageCatalog;
import musicpanel.MusicPanelInteractionHandler;
import musicpanel.MusicPanelRenderer;
import musicpanel.MusicPanelService;
import musicpanel.SqliteMusicPanelRepository;
import settings.GuildLanguageService;
import settings.SettingsAccessPolicy;
import settings.SqliteGuildSettingsRepository;

import java.nio.file.Path;

public final class BotBootstrap {
    private BotBootstrap() {
    }

    public static BotListener createListener() {
        Path databasePath = Path.of(Config.getOrDefault("DATABASE_PATH", "data/littleworldbot.db"));

        var messages = new MessageCatalog();
        var languageService = new GuildLanguageService(
                new SqliteGuildSettingsRepository(databasePath)
        );
        var musicCore = MusicCore.getInstance();
        var panelService = new MusicPanelService(
                musicCore,
                languageService,
                new MusicPanelRenderer(messages),
                new SqliteMusicPanelRepository(databasePath)
        );
        var panelInteractions = new MusicPanelInteractionHandler(
                musicCore,
                panelService,
                languageService,
                new SettingsAccessPolicy(Config.getLongOrDefault("BOT_OWNER_ID", 0L)),
                messages
        );

        var registry = CommandRegistryFactory.create(musicCore, messages, panelService);
        return new BotListener(
                new SlashCommandRouter(registry, languageService, messages),
                new AutocompleteRouter(registry, languageService),
                new CommandPublisher(messages),
                panelInteractions,
                panelService
        );
    }
}
