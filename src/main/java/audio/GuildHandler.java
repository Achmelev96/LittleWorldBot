package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.entities.Guild;

public class GuildHandler {

    private final Guild guild;
    private final AudioPlayer player;
    private final TrackHandler scheduler;
    private final GuildAudioSendHandler audioSendHandler;

    public GuildHandler(Guild guild, AudioPlayer player) {
        this.guild = guild;
        this.player = player;
        this.scheduler = new TrackHandler(player);
        this.audioSendHandler = new GuildAudioSendHandler(player);
    }

    public TrackHandler getScheduler() {
        return scheduler;
    }

    public GuildAudioSendHandler getAudioSendHandler() {
        return audioSendHandler;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public Guild getGuild() {
        return guild;
    }
}
