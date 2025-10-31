package commands;

import interaction.InteractionContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class JoinHandler implements SlashCommand {

    @Override
    public String name() {
        return "join";
    }

    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {
        event.deferReply(true).queue();

        var guild = context.guild();
        var self = guild != null ? guild.getSelfMember() : null;
        var userChannel = context.voice().userChannel();
        if (userChannel == null) {
            event.getHook().editOriginal("А куда зайти?").queue();
            return;
        }

        var botChannel = context.voice().botChannel();
        if (botChannel != null && botChannel.getIdLong() == userChannel.getIdLong()) {
            event.getHook().editOriginal("Уже тут").queue();
            return;
        }

        if (self != null && !self.hasPermission(userChannel, Permission.VOICE_CONNECT)) {
            event.getHook().editOriginal("У меня нету прав на подключение к этому каналу").queue();
            return;
        }

        try {
            var audioManager = context.voice().audioManager();
            audioManager.setSelfDeafened(false);
            audioManager.openAudioConnection(userChannel);
            event.getHook().editOriginal("Захожу к тебе").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Не могу подключиться к тебе").queue();
            e.printStackTrace();
        }
    }

}
