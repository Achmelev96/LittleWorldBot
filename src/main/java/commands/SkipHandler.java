package commands;

import audio.PlayerControlService;
import audio.TrackUtils;
import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class SkipHandler extends BaseMusicCommand {
    public SkipHandler(MessageCatalog messages) {
        super(messages);
    }

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus status) {
        event.deferReply(false).queue();

        if (!isBotUserInSameChannel(status)) {
            event.getHook().editOriginal(messages.get(status.language(), "skip.same_channel_required")).queue();
            return;
        }

        var handler = core.getGuildHandler(status.guild());
        var result = PlayerControlService.skip(handler);
        var previousTrack = result.previous();
        if (previousTrack == null) {
            event.getHook().editOriginal(messages.get(status.language(), "skip.nothing_playing")).queue();
            return;
        }

        String previousTitle = TrackUtils.safeTitle(previousTrack);
        var currentTrack = result.current();
        MusicPanelHandler.getInstance().showOrUpdate(status.guild());

        if (currentTrack == null) {
            event.getHook().editOriginal(messages.get(
                    status.language(), "skip.queue_empty", previousTitle
            )).queue();
        } else {
            event.getHook().editOriginal(messages.get(
                    status.language(),
                    "skip.now_playing",
                    previousTitle,
                    currentTrack.getInfo().title,
                    TrackUtils.formatDuration(currentTrack.getInfo().length)
            )).queue();
        }
    }
}
