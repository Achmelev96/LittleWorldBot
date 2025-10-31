package audio;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
// import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
// import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MusicCore {

    private static final MusicCore INSTANCE = new MusicCore();
    private final AudioPlayerManager playerManager;
    private Map<Long, GuildHandler> guildHandlers = new ConcurrentHashMap<>();

    private MusicCore(){
        this.playerManager = new DefaultAudioPlayerManager();

        this.playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        this.playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.MEDIUM);

        //AudioSourceManagers.registerRemoteSources(this.playerManager);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(true));
    }

    public static MusicCore getInstance(){
        return INSTANCE;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public GuildHandler getGuildHandler(Guild guild) {
        return guildHandlers.computeIfAbsent(guild.getIdLong(),id -> {
            AudioPlayer player = playerManager.createPlayer();
            player.setVolume(100);
            GuildHandler handler = new GuildHandler(guild, player);
            player.addListener(handler.getScheduler());
            return handler;
        });
    }
}