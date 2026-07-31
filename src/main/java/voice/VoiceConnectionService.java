package voice;

import audio.GuildHandler;
import audio.MusicCore;
import interaction.CurrentStatus;

public final class VoiceConnectionService {
    private final MusicCore musicCore;

    public VoiceConnectionService(MusicCore musicCore) {
        this.musicCore = musicCore;
    }

    public void prepareAudioSender(CurrentStatus context, GuildHandler guildHandler) {
        var audioManager = context.audioManager();
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(guildHandler.getAudioSendHandler());
        }
    }

    public boolean connectToUser(CurrentStatus context) {
        var userChannel = context.userChannel();
        if (context.guild() == null || context.audioManager() == null || userChannel == null) {
            return false;
        }

        try {
            var audioManager = context.audioManager();
            if (userChannel.equals(audioManager.getConnectedChannel())) {
                musicCore.cancelAfkDisconnect(context.guild().getIdLong());
                return true;
            }

            audioManager.setSelfDeafened(true);
            audioManager.openAudioConnection(userChannel);
            musicCore.cancelAfkDisconnect(context.guild().getIdLong());
            return true;
        } catch (RuntimeException error) {
            error.printStackTrace();
            return false;
        }
    }

    public void disconnect(CurrentStatus context) {
        var audioManager = context.audioManager();
        audioManager.setSendingHandler(null);
        audioManager.closeAudioConnection();
        musicCore.cancelAfkDisconnect(context.guild().getIdLong());
    }
}
