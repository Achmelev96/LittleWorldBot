package audio.playback;

public enum RepeatMode {
    OFF, ONCE, LOOP;

    public RepeatMode next() {
        return switch (this) {
            case OFF -> ONCE;
            case ONCE -> LOOP;
            case LOOP -> OFF;
        };
    }
}