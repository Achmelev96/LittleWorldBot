package events;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandRegister {

    public static void registerCommand(JDA jda) {

        jda.updateCommands().addCommands(
                Commands.slash("play", "Queue a track to play")
                        .setGuildOnly(true),
                Commands.slash("pause", "Pause the current track")
                        .setGuildOnly(true),
                Commands.slash("join", "Join the voice channel")
                        .setGuildOnly(true),
                Commands.slash("leave", "Leave the voice channel")
                        .setGuildOnly(true)
        ).queue();
    }
}