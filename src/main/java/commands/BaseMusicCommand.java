package commands;

import audio.MusicCore;
import interaction.CurrentStatus;
import localization.MessageCatalog;
import net.dv8tion.jda.api.Permission;

public abstract class BaseMusicCommand implements SlashCommand {
    protected final MusicCore core = MusicCore.getInstance();
    protected final MessageCatalog messages;

    protected BaseMusicCommand(MessageCatalog messages) {
        this.messages = messages;
    }

    protected boolean isUserInVoice(CurrentStatus status) {
        return status.userChannel() != null;
    }

    protected boolean isBotInVoice(CurrentStatus status) {
        return status.botChannel() != null;
    }

    protected boolean isBotUserInSameChannel(CurrentStatus status) {
        var botChannel = status.botChannel();
        var userChannel = status.userChannel();
        return botChannel != null
                && userChannel != null
                && botChannel.getIdLong() == userChannel.getIdLong();
    }

    protected boolean lacksVoicePermissions(CurrentStatus status) {
        var guild = status.guild();
        var userChannel = status.userChannel();
        if (guild == null || userChannel == null) return true;

        var permissions = guild.getSelfMember().getPermissions(userChannel);
        return !permissions.contains(Permission.VOICE_CONNECT)
                || !permissions.contains(Permission.VOICE_SPEAK);
    }

    protected boolean connectToUserVoice(CurrentStatus context) {
        var userChannel = context.userChannel();
        if (userChannel == null) return false;

        try {
            var audioManager = context.audioManager();
            var connectedChannel = audioManager.getConnectedChannel();
            if (userChannel.equals(connectedChannel)) {
                core.cancelAfkDisconnect(context.guild().getIdLong());
                return true;
            }

            audioManager.setSelfDeafened(true);
            audioManager.openAudioConnection(userChannel);
            core.cancelAfkDisconnect(context.guild().getIdLong());
            return true;
        } catch (Exception error) {
            error.printStackTrace();
            return false;
        }
    }

    protected void disconnectFromVoice(CurrentStatus context) {
        var audioManager = context.audioManager();
        audioManager.setSendingHandler(null);
        audioManager.closeAudioConnection();
        core.cancelAfkDisconnect(context.guild().getIdLong());
    }
}
