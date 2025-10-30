package interaction;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;

public final class VoiceContext {
    private final Guild guild;
    private final AudioManager audioManager;
    private final AudioChannelUnion userChannel;
    private final AudioChannelUnion botChannel;

    public VoiceContext(Guild guild,
                        AudioManager audioManager,
                        AudioChannelUnion userChannel,
                        AudioChannelUnion botChannel) {
        this.guild = guild;
        this.audioManager = audioManager;
        this.userChannel = userChannel;
        this.botChannel = botChannel;
    }

    public Guild guild() { return guild; }
    public AudioManager audioManager() { return audioManager; }
    public AudioChannelUnion userChannel() { return userChannel; }
    public AudioChannelUnion botChannel() { return botChannel; }

    public boolean isBotConnected() { return botChannel != null; }
    public boolean isUserInVoice() { return userChannel != null; }

    public boolean isSameChannel() {
        return userChannel != null
                && botChannel != null
                && userChannel.getIdLong() == botChannel.getIdLong();
    }
}
