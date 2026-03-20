package commands;

import audio.MusicCore;
import interaction.CurrentStatus;
import net.dv8tion.jda.api.Permission;

public abstract class BaseMusicCommand implements SlashCommand {

    protected MusicCore core = MusicCore.getInstance();

    protected boolean isUserInVoice(CurrentStatus status) {
        var userChannel = status.userChannel();
        if (userChannel == null) {
            return false;
        }
        return true;
    }

    protected boolean isBotInVoice(CurrentStatus status) {
        var botChannel = status.botChannel();
        if (botChannel == null) {
            return false;
        }
        return true;
    }

    protected boolean isBotUserInSameChannel(CurrentStatus status) {
        var botChannel = status.botChannel();
        var userChannel = status.userChannel();
        if (botChannel == null || botChannel.getIdLong() != userChannel.getIdLong()) {
            return false;
        }
        return true;
    }

    protected boolean canBotJoin(CurrentStatus status) {
        var guild = status.guild();
        var self = guild != null ? guild.getSelfMember() : null;
        var userChannel = status.userChannel();

        if (guild == null || self == null || userChannel == null) {
            return false;
        }

        var perms = guild.getSelfMember().getPermissions(userChannel);
        if (!perms.contains(Permission.VOICE_CONNECT) || !perms.contains(Permission.VOICE_SPEAK)) {
            return true;
        }
        return false;
    }

    protected boolean connectToUserVoice(CurrentStatus context) {
        var userChannel = context.userChannel();
        if (userChannel == null) {
            return false;
        }

        try {
            var audioManager = context.audioManager();
            var connectedChannel = audioManager.getConnectedChannel();
            if (connectedChannel != null && connectedChannel.equals(userChannel)) {
                MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
                return true;
            }

            audioManager.setSelfDeafened(true);
            audioManager.openAudioConnection(userChannel);
            MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    protected void disconnectFromVoice(CurrentStatus context) {
        var audioManager = context.audioManager();
        audioManager.setSendingHandler(null);
        audioManager.closeAudioConnection();
        MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
    }
}
