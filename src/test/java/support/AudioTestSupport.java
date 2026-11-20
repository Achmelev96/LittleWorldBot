package support;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AudioTestSupport {
    private AudioTestSupport() {}

    public static AudioTrack track(String name) {
        return (AudioTrack) Proxy.newProxyInstance(AudioTrack.class.getClassLoader(), new Class<?>[]{AudioTrack.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getInfo" -> new AudioTrackInfo(name, "artist", 1000, name, false, "https://example.com/" + name);
                    case "makeClone" -> track(name);
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> name;
                    default -> null;
                });
    }

    public static final class FakePlayer {
        public final AtomicReference<AudioTrack> playing = new AtomicReference<>();
        public final AtomicBoolean paused = new AtomicBoolean();
        public final BlockingQueue<Start> starts = new LinkedBlockingQueue<>();
        public final AudioPlayer player = (AudioPlayer) Proxy.newProxyInstance(
                AudioPlayer.class.getClassLoader(), new Class<?>[]{AudioPlayer.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getPlayingTrack" -> playing.get();
                    case "startTrack" -> {
                        AudioTrack track = (AudioTrack) args[0];
                        playing.set(track);
                        starts.add(new Start(track));
                        yield true;
                    }
                    case "stopTrack" -> { playing.set(null); yield null; }
                    case "isPaused" -> paused.get();
                    case "setPaused" -> { paused.set((boolean) args[0]); yield null; }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> null;
                });

        public AudioTrack awaitStart() throws InterruptedException {
            Start start = starts.poll(3, TimeUnit.SECONDS);
            if (start == null) throw new AssertionError("No playback transition received");
            return start.track();
        }
    }

    public record Start(AudioTrack track) {}
}