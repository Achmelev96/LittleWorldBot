package events;


import audio.AudioHandler;
import audio.TrackHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.net.URL;

public class CommandListener {

    public static void handleJoin(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        var context = buildVoiceContext(event);

        if (context.userChannel == null) {
            event.getHook().editOriginal("А куда зайти?").queue();
            return;
        }

        if (context.botChannel() != null) {
            if (context.botChannel().getIdLong() == context.userChannel().getIdLong()) {
                event.getHook().editOriginal("Уже тут").queue();
                return;
            }
        }

        try {
            context.audioManager.openAudioConnection(context.userChannel);
            event.getHook().editOriginal("Захожу").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Не могу подключиться к каналу").queue();
            e.printStackTrace();
        }
    }

    public static void handleLeave(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        var context = buildVoiceContext(event);

        if (context.botChannel == null) {
            event.getHook().editOriginal("Меня там и нет").queue();
            return;
        }

        try {
            context.audioManager.closeAudioConnection();
            event.getHook().editOriginal("Ладно, ухожу").queue();
        } catch (Exception e) {
            event.getHook().editOriginal("Что-то пошло не так").queue();
            e.printStackTrace();
        }
    }

    public static void handlePlay(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        var context = buildVoiceContext(event);
        var guild = event.getGuild();

        if (context.userChannel == null) {
            event.getHook().editOriginal("А куда зайти?").queue();
            return;
        }

        var core = AudioHandler.getInstance();
        var guildHandler = core.getGuildHandler(guild);

        if (context.audioManager.getSendingHandler() == null) {
            context.audioManager.setSendingHandler(guildHandler.getAudioSendHandler());
        }

        try {
            var botChannel = context.botChannel(); // может быть null
            if (botChannel == null) {
                context.audioManager.openAudioConnection(context.userChannel);
            } else if (botChannel.getIdLong() != context.userChannel.getIdLong()) {
                context.audioManager.openAudioConnection(context.userChannel);
            }
        } catch (Exception e) {
            event.getHook().editOriginal("Что-то пошло не так").queue();
            e.printStackTrace();
            return;
        }

        String identifier = buildIdentifier(event);
        if (identifier == null || identifier.isBlank()) {
            event.getHook().editOriginal("Пустой запрос").queue();
            return;
        }

        core.getPlayerManager().loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildHandler.getScheduler().queue(track);
                event.getHook().editOriginal("Добавил в очередь: <<" + track.getInfo().title + ">>").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && !playlist.getTracks().isEmpty()) {
                    track = playlist.getTracks().get(0);
                }
                if (track != null) {
                    guildHandler.getScheduler().queue(track);
                    event.getHook().editOriginal("Добавил из плейлиста: <<" + track.getInfo().title + ">>").queue();
                } else {
                    for (var t : playlist.getTracks()) {
                        guildHandler.getScheduler().queue(t);
                    }
                    event.getHook().editOriginal("Добавил плейлист: <<" + playlist.getName()
                            + ">> на " + playlist.getTracks().size() + " треков").queue();
                }
            }

            @Override
            public void noMatches() {
                event.getHook().editOriginal("Ничего не найдено по: `" + identifier + "`").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().editOriginal("Ошибка загрузки: " + exception.getMessage()).queue();
                System.err.println("[LavaPlayer][loadFailed] severity=" + exception.severity);
                System.err.println("[LavaPlayer][loadFailed] message=" + exception.getMessage());
                if (exception.getCause() != null) {
                    System.err.println("[LavaPlayer][loadFailed] cause=" + exception.getCause().getClass().getName()
                            + " : " + exception.getCause().getMessage());
                }
                exception.printStackTrace();
            }
        });
    }

    public static void handlePause(SlashCommandInteractionEvent event) {
        var context = buildVoiceContext(event);
    }

    private static String buildIdentifier(SlashCommandInteractionEvent event) {
        String raw = event.getOption("query").getAsString().trim();

        java.net.URI uri = tryParse(raw);
        if (uri == null || uri.getScheme() == null) {
            return "ytmsearch: " + raw;
        }

        String host = String.valueOf(uri.getHost()).toLowerCase();
        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            return normalizeYoutubeUrl(uri);
        }
        return raw;
    }

    private static String normalizeYoutubeUrl(java.net.URI uri) {
        String host = String.valueOf(uri.getHost()).toLowerCase();
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.contains("youtu.be")) {
            String id = firstSeg(path);
            if (id != null) return "https://music.youtube.com/watch?v=" + id;
            return uri.toString();
        }

        if (path.startsWith("/shorts/")) {
            String id = firstSeg(path.substring("/shorts/".length()));
            if (id != null) return "https://music.youtube.com/watch?v=" + id;
            return uri.toString();
        }

        if ("/watch".equals(path)) {
            java.util.Map<String,String> q = parseQuery(uri.getRawQuery());
            String v = q.get("v");
            if (v == null || v.isBlank()) return uri.toString();

            String list = q.get("list");
            boolean isMix = list != null && list.startsWith("RD");

            StringBuilder sb = new StringBuilder("https://music.youtube.com/watch?v=").append(v);

            if (!isMix && list != null && !list.isBlank() && !list.startsWith("RD")) {
                sb.append("&list=").append(list);
                if (q.containsKey("index")) sb.append("&index=").append(q.get("index"));
            }
            return sb.toString();
        }

        return uri.toString();
    }

    private static String firstSeg(String p) {
        if (p == null) return null;
        String s = p.startsWith("/") ? p.substring(1) : p;
        int i = s.indexOf('/');
        String seg = (i >= 0) ? s.substring(0, i) : s;
        return seg.isBlank() ? null : seg;
    }

    private static java.util.Map<String,String> parseQuery(String raw) {
        java.util.Map<String,String> out = new java.util.HashMap<>();
        if (raw == null || raw.isBlank()) return out;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = java.net.URLDecoder.decode(eq >= 0 ? pair.substring(0, eq) : pair,
                    java.nio.charset.StandardCharsets.UTF_8);
            String v = java.net.URLDecoder.decode(eq >= 0 ? pair.substring(eq + 1) : "",
                    java.nio.charset.StandardCharsets.UTF_8);
            out.put(k, v);
        }
        out.remove("pp"); out.remove("feature"); out.remove("si");
        out.remove("t");  out.remove("start_radio");
        return out;
    }

    private static java.net.URI tryParse(String s) {
        try { return java.net.URI.create(s); } catch (Exception e) { return null; }
    }

    private static VoiceContext buildVoiceContext(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            throw new IllegalStateException("Ошибка в VoiceContext");
        }

        AudioManager audioManager = guild.getAudioManager();
        AudioChannel userChannel = member.getVoiceState() != null ? member.getVoiceState().getChannel() : null;
        AudioChannel botChannel = audioManager.getConnectedChannel();

        return new VoiceContext(guild, audioManager, member, userChannel, botChannel);
    }

    private record VoiceContext(
            Guild guild,
            AudioManager audioManager,
            Member member,
            AudioChannel userChannel,
            AudioChannel botChannel
    ) {}

}
