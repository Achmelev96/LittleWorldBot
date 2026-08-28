package commands.leave;

import audio.MusicCore;
import voice.CurrentStatus;
import voice.VoiceConnectionService;
import voice.VoiceStateValidator;
import voice.VoiceValidationResult;

public final class LeaveUseCase {
    private final MusicCore musicCore;
    private final VoiceStateValidator voiceValidator;
    private final VoiceConnectionService voiceConnection;

    public LeaveUseCase(
            MusicCore musicCore,
            VoiceStateValidator voiceValidator,
            VoiceConnectionService voiceConnection
    ) {
        this.musicCore = musicCore;
        this.voiceValidator = voiceValidator;
        this.voiceConnection = voiceConnection;
    }

    public LeaveResult execute(CurrentStatus context) {
        if (voiceValidator.requireBotInVoice(context) != VoiceValidationResult.OK) {
            return LeaveResult.BOT_NOT_IN_VOICE;
        }

        try {
            musicCore.getGuildHandler(context.guild()).getScheduler().stopAll();
            voiceConnection.disconnect(context);
            return LeaveResult.SUCCESS;
        } catch (RuntimeException error) {
            error.printStackTrace();
            return LeaveResult.FAILED;
        }
    }
}
