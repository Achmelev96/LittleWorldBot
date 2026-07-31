package commands.play;

public sealed interface PlayResult {

    record TrackQueued(String title, String duration) implements PlayResult {}

    record SearchResultQueued(String title) implements PlayResult {}

    record PlaylistQueued(String name, int trackCount) implements PlayResult {}

    record Failure(FailureReason reason, String details) implements PlayResult {
        public Failure(FailureReason reason) {
            this(reason, null);
        }
    }

    enum FailureReason {
        GUILD_UNAVAILABLE,
        USER_NOT_IN_VOICE,
        MISSING_PERMISSIONS,
        CONNECTION_FAILED,
        EMPTY_QUERY,
        NO_MATCHES,
        LOAD_FAILED
    }
}
