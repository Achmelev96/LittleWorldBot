package commands.play;

import commands.SlashCommand;
import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PlayCommandHandler implements SlashCommand {
    private final PlayUseCase playUseCase;
    private final MessageCatalog messages;

    public PlayCommandHandler(PlayUseCase playUseCase, MessageCatalog messages) {
        this.playUseCase = playUseCase;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "play";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event, CurrentStatus context) {
        event.deferReply(false).queue();
        var queryOption = event.getOption("query");
        String query = queryOption == null ? "" : queryOption.getAsString();

        playUseCase.execute(context, query).whenComplete((result, error) -> {
            if (error != null) {
                error.printStackTrace();
                event.getHook().editOriginal(messages.get(context.language(), "common.error")).queue();
                return;
            }
            event.getHook().editOriginal(messageFor(context, result)).queue();
        });
    }

    private String messageFor(CurrentStatus context, PlayResult result) {
        return switch (result) {
            case PlayResult.TrackQueued track -> messages.get(
                    context.language(), "play.track_queued", track.title(), track.duration()
            );
            case PlayResult.SearchResultQueued track -> messages.get(
                    context.language(), "play.search_found", track.title()
            );
            case PlayResult.PlaylistQueued playlist -> messages.get(
                    context.language(), "play.playlist_queued", playlist.name(), playlist.trackCount()
            );
            case PlayResult.Failure failure -> failureMessage(context, failure);
        };
    }

    private String failureMessage(CurrentStatus context, PlayResult.Failure failure) {
        return switch (failure.reason()) {
            case USER_NOT_IN_VOICE -> messages.get(context.language(), "voice.user_not_connected");
            case MISSING_PERMISSIONS -> messages.get(context.language(), "voice.missing_permissions");
            case CONNECTION_FAILED -> messages.get(context.language(), "voice.connection_failed");
            case EMPTY_QUERY -> messages.get(context.language(), "play.empty_query");
            case NO_MATCHES -> messages.get(context.language(), "play.no_matches");
            case LOAD_FAILED -> messages.get(
                    context.language(), "play.load_failed", failure.details() == null ? "" : failure.details()
            );
            case GUILD_UNAVAILABLE -> messages.get(context.language(), "common.error");
        };
    }
}
