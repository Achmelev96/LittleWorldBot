package commands;

import audio.MusicCore;
import interaction.CurrentStatus;
import net.dv8tion.jda.api.Permission;

public abstract class BaseMusicCommand implements SlashCommand {

    protected MusicCore core = MusicCore.getInstance();

    protected boolean isUserInVoice(CurrentStatus status) {
        var userChannel = status.voice().userChannel();
        if (userChannel == null) {
            return false;
        }
        return true;
    }

    protected boolean isBotInVoice(CurrentStatus status) {
        var botChannel = status.voice().botChannel();
        if (botChannel == null) {
            return false;
        }
        return true;
    }

    protected boolean isBotUserInSameChannel(CurrentStatus status) {
        var botChannel = status.voice().botChannel();
        var userChannel = status.voice().userChannel();
        if (botChannel == null || botChannel.getIdLong() != userChannel.getIdLong()) {
            return false;
        }
        return true;
    }

    protected boolean canBotJoin(CurrentStatus status) {
        var guild = status.guild();
        var self = guild != null ? guild.getSelfMember() : null;
        var userChannel = status.voice().userChannel();

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
        var userChannel = context.voice().userChannel();

        try {
            var audioManager = context.voice().audioManager();
            audioManager.setSelfDeafened(false);
            audioManager.openAudioConnection(userChannel);
            MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void disconnectFromVoice(CurrentStatus context) {
        var audioManager = context.voice().audioManager();
        audioManager.setSendingHandler(null);
        audioManager.closeAudioConnection();
        MusicCore.getInstance().cancelAfkDisconnect(context.guild().getIdLong());
    }
}
