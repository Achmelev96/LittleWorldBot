package commands;

import audio.MusicCore;
import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class LeaveHandler implements SlashCommand {

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, InteractionContext context) {
        event.deferReply(true).queue();

        if (context.voice().botChannel() == null) {
            event.getHook().editOriginal("Меня там и нет").queue();
            return;
        }

        try {
            var core = MusicCore.getInstance();
            var guildHandler = core.getGuildHandler(event.getGuild());

            guildHandler.getScheduler().stopAll();
            context.voice().audioManager().setSendingHandler(null);
            context.voice().audioManager().closeAudioConnection();
            audio.MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());

            event.getHook().editOriginal("Ладно, ухожу").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Что-то пошло не так").queue();
            e.printStackTrace();
        }
    }
}
