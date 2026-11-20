package commands.repeat;

import audio.playback.RepeatMode;

public sealed interface RepeatResult {
    record Changed(RepeatMode mode) implements RepeatResult {}
    record Failure(FailureReason reason) implements RepeatResult {}

    enum FailureReason {
        NOT_IN_SAME_CHANNEL,
        NOTHING_PLAYING
    }
}