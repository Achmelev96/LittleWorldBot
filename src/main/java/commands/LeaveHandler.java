package commands;

import audio.MusicCore;
import interaction.CurrentStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class LeaveHandler extends BaseMusicCommand {

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(true).queue();

        if (!isBotInVoice(status)) {
            event.getHook().editOriginal("Меня там и нет").queue();
            return;
        }

        try {
            /*var core = MusicCore.getInstance();
            var guildHandler = core.getGuildHandler(event.getGuild());

            guildHandler.getScheduler().stopAll();
            context.voice().audioManager().setSendingHandler(null);
            context.voice().audioManager().closeAudioConnection();
            audio.MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
*/
            var guildHandler = core.getGuildHandler(status.guild());
            guildHandler.getScheduler().stopAll();
            disconnectFromVoice(status);

            event.getHook().editOriginal("Ладно, ухожу").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Что-то пошло не так").queue();
            e.printStackTrace();
        }
    }
}
