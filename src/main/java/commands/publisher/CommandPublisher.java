package commands.publisher;

import localization.MessageCatalog;
import localization.BotLanguage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public final class CommandPublisher {
    private final MessageCatalog messages;

    public CommandPublisher(MessageCatalog messages) {
        this.messages = messages;
    }

    public void publish(JDA jda) {
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
        ).queue(
                commands -> System.out.println("[Commands] Published " + commands.size() + " global commands"),
                error -> {
                    System.err.println("[Commands] Could not publish global commands");
                    error.printStackTrace();
                }
        );
    }
}
