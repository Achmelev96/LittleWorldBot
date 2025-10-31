package autocomplete;

import interaction.InteractionContext;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import audio.MusicCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayQueryAutocomplete implements AutocompleteProvider {

    private static final int MAX_CHOICES = 25;
    private static final int MAX_LABEL   = 100;

    @Override
    public boolean supports(String commandName, String optionName) {
        return "play".equals(commandName) && "query".equals(optionName);
    }

    @Override
    public void handle(CommandAutoCompleteInteractionEvent event, InteractionContext context) {
        final String input = Optional.of(event.getFocusedOption().getValue())
                .map(String::trim)
                .orElse("");

        if (input.isEmpty()) {
            event.replyChoices().queue();
            return;
        }

        final AudioPlayerManager manager = MusicCore.getInstance().getPlayerManager();

        final String query = "ytsearch:" + input;

        manager.loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                var info = track.getInfo();
                String label = makeLabel(info.title, info.author);
                String value = info.uri != null ? info.uri : ("ytsearch:" + input);

                event.replyChoices(new Command.Choice(limit(label, MAX_LABEL), value)).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                var tracks = playlist.getTracks();
                if (tracks == null || tracks.isEmpty()) {
                    event.replyChoices(new Command.Choice(limit(input, MAX_LABEL), input)).queue();
                    return;
                }

                List<Command.Choice> choices = new ArrayList<>(Math.min(MAX_CHOICES, tracks.size()));
                for (int i = 0; i < Math.min(MAX_CHOICES, tracks.size()); i++) {
                    AudioTrack t = tracks.get(i);
                    var info = t.getInfo();

                    String label = makeLabel(info.title, info.author);
                    String value = info.uri != null ? info.uri : ("ytsearch:" + safeConcat(info.title, info.author));

                    choices.add(new Command.Choice(limit(label, MAX_LABEL), value));
                }

                event.replyChoices(choices).queue();
            }

            @Override
            public void noMatches() {
                event.replyChoices(new Command.Choice(limit(input, MAX_LABEL), input)).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.replyChoices(new Command.Choice(limit(input, MAX_LABEL), input)).queue();
            }
        });
    }

    private static String makeLabel(String title, String author) {
        String t = title != null && !title.isBlank() ? title : "Unknown";
        String a = author != null && !author.isBlank() ? author : "";
        return a.isEmpty() ? t : (t + " — " + a);
    }

    private static String safeConcat(String title, String author) {
        String titleName = title != null ? title : "";
        String authorName = author != null ? author : "";
        String line = (titleName + " " + authorName).trim();
        return line.isEmpty() ? "track" : line;
    }

    private static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
