package commands.publisher;

import commands.CommandRegistry;
import commands.autocomplete.PlayQueryAutocomplete;
import commands.leave.LeaveCommandHandler;
import commands.leave.LeaveUseCase;
import commands.play.PlayCommandHandler;
import commands.play.PlayUseCase;
import commands.skip.SkipCommandHandler;
import commands.skip.SkipUseCase;
import audio.MusicCore;
import localization.MessageCatalog;
import localization.BotLanguage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import voice.VoiceConnectionService;
import voice.VoiceStateValidator;

public class CommandPublisher {

    public static CommandRegistry buildRegistry(MessageCatalog messages) {
        var musicCore = MusicCore.getInstance();
        var voiceValidator = new VoiceStateValidator();
        var voiceConnection = new VoiceConnectionService(musicCore);

        var playUseCase = new PlayUseCase(musicCore, voiceValidator, voiceConnection);
        var skipUseCase = new SkipUseCase(musicCore, voiceValidator);
        var leaveUseCase = new LeaveUseCase(musicCore, voiceValidator, voiceConnection);

        var registry = new CommandRegistry();
        registry.registerSlash("play", new PlayCommandHandler(playUseCase, messages));
        registry.registerSlash("leave", new LeaveCommandHandler(leaveUseCase, messages));
        registry.registerSlash("skip", new SkipCommandHandler(skipUseCase, messages));
        registry.register("play", "query", new PlayQueryAutocomplete(messages));
        return registry;
    }
    public static void publish(JDA jda, MessageCatalog messages) {
        var queryOption = new OptionData(
                OptionType.STRING,
                "query",
                messages.get(BotLanguage.ENGLISH, "command.play.query.description"),
                true
        )
                .setAutoComplete(true)
                .setDescriptionLocalization(
                        DiscordLocale.RUSSIAN,
                        messages.get(BotLanguage.RUSSIAN, "command.play.query.description")
                );

        jda.updateCommands().addCommands(
                Commands.slash("play", messages.get(BotLanguage.ENGLISH, "command.play.description"))
                        .setDescriptionLocalization(
                                DiscordLocale.RUSSIAN,
                                messages.get(BotLanguage.RUSSIAN, "command.play.description")
                        )
                        .addOptions(queryOption),
                Commands.slash("skip", messages.get(BotLanguage.ENGLISH, "command.skip.description"))
                        .setDescriptionLocalization(
                                DiscordLocale.RUSSIAN,
                                messages.get(BotLanguage.RUSSIAN, "command.skip.description")
                        ),
                Commands.slash("leave", messages.get(BotLanguage.ENGLISH, "command.leave.description"))
                        .setDescriptionLocalization(
                                DiscordLocale.RUSSIAN,
                                messages.get(BotLanguage.RUSSIAN, "command.leave.description")
                        )
        ).queue();
    }
}
