package commands.repeat;

import audio.MusicCore;
import voice.CurrentStatus;
import voice.VoiceStateValidator;
import voice.VoiceValidationResult;

public final class RepeatUseCase {
    private final MusicCore musicCore;
    private final VoiceStateValidator voiceValidator;

    public RepeatUseCase(MusicCore musicCore, VoiceStateValidator voiceValidator) {
        this.musicCore = musicCore;
        this.voiceValidator = voiceValidator;
    }

    public RepeatResult execute(CurrentStatus context) {
        if (voiceValidator.requireSameChannel(context) != VoiceValidationResult.OK) {
            return new RepeatResult.Failure(RepeatResult.FailureReason.NOT_IN_SAME_CHANNEL);
        }
        var mode = musicCore.getGuildHandler(context.guild()).getScheduler().cycleRepeat();
        return mode == null
                ? new RepeatResult.Failure(RepeatResult.FailureReason.NOTHING_PLAYING)
                : new RepeatResult.Changed(mode);
    }
}