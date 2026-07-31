package voice;

import interaction.CurrentStatus;
import net.dv8tion.jda.api.Permission;

public final class VoiceStateValidator {

    public VoiceValidationResult requireUserInVoice(CurrentStatus context) {
        if (context.guild() == null) return VoiceValidationResult.GUILD_UNAVAILABLE;
        if (context.userChannel() == null) return VoiceValidationResult.USER_NOT_IN_VOICE;
        return VoiceValidationResult.OK;
    }

    public VoiceValidationResult requireBotInVoice(CurrentStatus context) {
        if (context.guild() == null) return VoiceValidationResult.GUILD_UNAVAILABLE;
        if (context.botChannel() == null) return VoiceValidationResult.BOT_NOT_IN_VOICE;
        return VoiceValidationResult.OK;
    }

    public VoiceValidationResult requireSameChannel(CurrentStatus context) {
        VoiceValidationResult userState = requireUserInVoice(context);
        if (userState != VoiceValidationResult.OK) return userState;

        VoiceValidationResult botState = requireBotInVoice(context);
        if (botState != VoiceValidationResult.OK) return botState;

        if (context.userChannel().getIdLong() != context.botChannel().getIdLong()) {
            return VoiceValidationResult.DIFFERENT_CHANNEL;
        }
        return VoiceValidationResult.OK;
    }

    public VoiceValidationResult requireJoinPermissions(CurrentStatus context) {
        VoiceValidationResult userState = requireUserInVoice(context);
        if (userState != VoiceValidationResult.OK) return userState;

        var permissions = context.guild().getSelfMember().getPermissions(context.userChannel());
        if (!permissions.contains(Permission.VOICE_CONNECT)
                || !permissions.contains(Permission.VOICE_SPEAK)) {
            return VoiceValidationResult.MISSING_PERMISSIONS;
        }
        return VoiceValidationResult.OK;
    }
}
