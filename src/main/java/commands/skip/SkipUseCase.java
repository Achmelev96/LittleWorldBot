package commands.skip;

import audio.MusicCore;
import audio.PlayerControlService;
import audio.TrackUtils;
import interaction.CurrentStatus;
import voice.VoiceStateValidator;
import voice.VoiceValidationResult;

public final class SkipUseCase {
    private final MusicCore musicCore;
    private final VoiceStateValidator voiceValidator;

    public SkipUseCase(MusicCore musicCore, VoiceStateValidator voiceValidator) {
        this.musicCore = musicCore;
        this.voiceValidator = voiceValidator;
    }

    public SkipResult execute(CurrentStatus context) {
        if (voiceValidator.requireSameChannel(context) != VoiceValidationResult.OK) {
            return new SkipResult.Failure(SkipResult.FailureReason.NOT_IN_SAME_CHANNEL);
        }

        var handler = musicCore.getGuildHandler(context.guild());
        var skipResult = PlayerControlService.skip(handler);
        var previousTrack = skipResult.previous();
        if (previousTrack == null) {
            return new SkipResult.Failure(SkipResult.FailureReason.NOTHING_PLAYING);
        }

        String previousTitle = TrackUtils.safeTitle(previousTrack);
        var currentTrack = skipResult.current();
        if (currentTrack == null) {
            return new SkipResult.SkippedQueueEmpty(previousTitle);
        }

        return new SkipResult.SkippedNowPlaying(
                previousTitle,
                currentTrack.getInfo().title,
                TrackUtils.formatDuration(currentTrack.getInfo().length)
        );
    }
}
