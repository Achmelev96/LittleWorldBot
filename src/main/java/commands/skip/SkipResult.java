package commands.skip;

public sealed interface SkipResult {

    record SkippedQueueEmpty(String previousTitle) implements SkipResult {}

    record SkippedNowPlaying(
            String previousTitle,
            String currentTitle,
            String currentDuration
    ) implements SkipResult {}

    record Failure(FailureReason reason) implements SkipResult {}

    enum FailureReason {
        NOT_IN_SAME_CHANNEL,
        NOTHING_PLAYING
    }
}
