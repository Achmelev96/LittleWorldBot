package commands.publisher;

import audio.MusicCore;
import audio.YtDlpResolver;
import commands.CommandRegistry;
import commands.autocomplete.PlayQueryAutocomplete;
import commands.leave.LeaveCommandHandler;
import commands.leave.LeaveUseCase;
import commands.play.PlayCommandHandler;
import commands.play.PlayUseCase;
import commands.skip.SkipCommandHandler;
import commands.skip.SkipUseCase;
import localization.MessageCatalog;
import musicpanel.MusicPanelService;
import voice.VoiceConnectionService;
import voice.VoiceStateValidator;
import config.Config;

import java.time.Duration;

public final class CommandRegistryFactory {
    private CommandRegistryFactory() {
    }

    public static CommandRegistry create(
            MusicCore musicCore,
            MessageCatalog messages,
            MusicPanelService panelService
    ) {
        var voiceValidator = new VoiceStateValidator();
        var voiceConnection = new VoiceConnectionService(musicCore);
        var ytDlpResolver = new YtDlpResolver(
                Config.get("YT_DLP_PATH"),
                Duration.ofSeconds(45)
        );

        var registry = new CommandRegistry();
        registry.registerSlash("play", new PlayCommandHandler(
                new PlayUseCase(musicCore, voiceValidator, voiceConnection, ytDlpResolver),
                messages,
                panelService
        ));
        registry.registerSlash("leave", new LeaveCommandHandler(
                new LeaveUseCase(musicCore, voiceValidator, voiceConnection),
                messages
        ));
        registry.registerSlash("skip", new SkipCommandHandler(
                new SkipUseCase(musicCore, voiceValidator),
                messages,
                panelService
        ));
        registry.register("play", "query", new PlayQueryAutocomplete(messages));
        return registry;
    }
}
