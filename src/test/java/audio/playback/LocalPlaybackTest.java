package audio.playback;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;

public class LocalPlaybackTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private DefaultAudioPlayerManager manager;
    private AudioPlayer player;
    private TrackHandler handler;
    private final List<AudioTrack> starts = new CopyOnWriteArrayList<>();
    private final List<AudioTrackEndReason> ends = new CopyOnWriteArrayList<>();
    private final List<FriendlyException> errors = new CopyOnWriteArrayList<>();
    private int frames;

    @Before
    public void setup() {
        manager = new DefaultAudioPlayerManager();
        manager.registerSourceManager(new LocalAudioSourceManager());
        player = manager.createPlayer();
        handler = new TrackHandler(player);
        player.addListener(handler);
        player.addListener(new AudioEventAdapter() {
            @Override public void onTrackStart(AudioPlayer player, AudioTrack track) { starts.add(track); }
            @Override public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason reason) { ends.add(reason); }
            @Override public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException error) { errors.add(error); }
        });
    }

    @After
    public void cleanup() {
        if (handler != null) handler.stopAll();
        if (player != null) player.destroy();
        if (manager != null) manager.shutdown();
    }

    private AudioTrack wav(String name) throws Exception {
        var file = temporary.newFile(name + ".wav");
        int sampleFrames = 5760;
        byte[] pcm = new byte[sampleFrames * 4];
        for (int i = 0; i < sampleFrames; i++) {
            short sample = (short) (Math.sin(2 * Math.PI * 440 * i / 48000) * 6000);
            for (int channel = 0; channel < 2; channel++) {
                int offset = i * 4 + channel * 2;
                pcm[offset] = (byte) sample;
                pcm[offset + 1] = (byte) (sample >> 8);
            }
        }
        var format = new AudioFormat(48000, 16, 2, true, false);
        try (var input = new AudioInputStream(new ByteArrayInputStream(pcm), format, sampleFrames)) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, file);
        }
        var item = manager.loadItemSync(new AudioReference(file.getAbsolutePath(), name));
        assertTrue("Local WAV must load", item instanceof AudioTrack);
        return (AudioTrack) item;
    }

    private List<String> names() {
        return starts.stream().map(track -> Path.of(track.getIdentifier()).getFileName().toString()).toList();
    }

    private void drainUntil(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            var frame = player.provide();
            if (frame != null && !frame.isTerminator()) {
                assertTrue("Encoded frame has audio data", frame.getDataLength() > 0);
                frames++;
            }
            Thread.sleep(2);
        }
        assertTrue("Playback did not reach expected state; starts=" + names() + ", ends=" + ends + ", errors=" + errors,
                condition.getAsBoolean());
    }

    private void finished(int startCount) throws Exception {
        drainUntil(() -> starts.size() == startCount && player.getPlayingTrack() == null && handler.isQueueEmpty());
        assertTrue("Actual audio frames must be decoded and encoded", frames > 0);
        assertTrue("No unexpected audio errors: " + errors, errors.isEmpty());
    }

    @Test(timeout = 10000)
    public void singleRepeatDecodesRealAudioTwiceThenPlaysNext() throws Exception {
        handler.queue(wav("single")); handler.queue(wav("next")); handler.cycleRepeat();
        finished(3);
        assertEquals(List.of("single.wav", "single.wav", "next.wav"), names());
        assertNotSame(starts.get(0), starts.get(1));
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
    }

    @Test(timeout = 10000)
    public void playlistRepeatFromMiddlePreservesWholePlaylistAndTail() throws Exception {
        handler.queuePlaylist(List.of(wav("1"), wav("2"), wav("3"), wav("4"), wav("5")));
        handler.queue(wav("next"));
        var id = handler.playlistWindow(player.getPlayingTrack()).playlistId();
        assertTrue(handler.jumpToPlaylistTrack(id, 2)); handler.cycleRepeat();
        drainUntil(() -> starts.size() == 5);
        assertEquals(List.of("1.wav", "3.wav", "4.wav", "5.wav", "1.wav"), names());
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        finished(10);
        assertEquals(List.of("1.wav", "3.wav", "4.wav", "5.wav", "1.wav", "2.wav", "3.wav", "4.wav", "5.wav", "next.wav"), names());
    }

    @Test(timeout = 10000)
    public void infiniteRepeatLoopsOnlySelectedTrackAndDisablingContinuesPlaylist() throws Exception {
        handler.queuePlaylist(List.of(wav("1"), wav("2"), wav("3")));
        var id = handler.playlistWindow(player.getPlayingTrack()).playlistId();
        assertTrue(handler.jumpToPlaylistTrack(id, 1)); handler.cycleRepeat(); handler.cycleRepeat();
        drainUntil(() -> starts.size() >= 5);
        assertEquals(List.of("1.wav", "2.wav", "2.wav", "2.wav", "2.wav"), names());
        assertEquals(RepeatMode.LOOP, handler.getRepeatMode());
        handler.cycleRepeat(); finished(6);
        assertEquals("3.wav", names().getLast());
    }

    @Test(timeout = 10000)
    public void pauseNavigationSkipAndSkipPlaylistWorkWithRealPlayer() throws Exception {
        handler.queuePlaylist(List.of(wav("1"), wav("2"), wav("3"))); handler.queue(wav("next"));
        handler.cycleRepeat(); handler.cycleRepeat(); player.setPaused(true);
        for (int i = 0; i < 20; i++) { assertNull(player.provide()); Thread.sleep(2); }
        assertEquals(1, starts.size()); assertEquals(RepeatMode.LOOP, handler.getRepeatMode());
        var id = handler.playlistWindow(player.getPlayingTrack()).playlistId();
        assertTrue(handler.jumpToPlaylistTrack(id, 1));
        assertFalse(player.isPaused()); assertEquals(RepeatMode.LOOP, handler.getRepeatMode());
        assertTrue(handler.skip(player.getPlayingTrack()).skipped());
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        handler.cycleRepeat(); assertTrue(handler.skipPlaylist(id));
        assertEquals(RepeatMode.OFF, handler.getRepeatMode()); finished(4);
        assertEquals(List.of("1.wav", "2.wav", "3.wav", "next.wav"), names());
    }

    @Test(timeout = 10000)
    public void realExecutorFailureRetriesExactlyOnceThenExitsLoop() throws Exception {
        var gate = new CountDownLatch(1); var attempts = new AtomicInteger();
        handler.queuePlaylist(List.of(new FailingTrack(gate, attempts), wav("next")));
        handler.cycleRepeat(); handler.cycleRepeat(); gate.countDown();
        drainUntil(() -> starts.size() == 3 && player.getPlayingTrack() == null && handler.isQueueEmpty());
        assertEquals(List.of("broken", "broken", "next.wav"), names());
        assertEquals(2, attempts.get()); assertEquals(2, errors.size());
        assertEquals(3, ends.size());
        assertEquals(RepeatMode.OFF, handler.getRepeatMode()); assertTrue(frames > 0);
    }

    @Test(timeout = 10000)
    public void stoppingLoopDoesNotRestartFromLateCallbacks() throws Exception {
        handler.queue(wav("single")); handler.cycleRepeat(); handler.cycleRepeat();
        handler.stopAll();
        for (int i = 0; i < 30; i++) { player.provide(); Thread.sleep(2); }
        assertEquals(1, starts.size()); assertNull(player.getPlayingTrack());
        assertEquals(RepeatMode.OFF, handler.getRepeatMode()); assertTrue(handler.isQueueEmpty());
    }

    @Test(timeout = 10000)
    public void emptyFinishedAudioCannotLoopForeverOrResetRetryBudget() throws Exception {
        var attempts = new AtomicInteger();
        handler.queue(new EmptyTrack(attempts)); handler.queue(wav("next"));
        handler.cycleRepeat(); handler.cycleRepeat();
        finished(3);
        assertEquals(List.of("empty", "empty", "next.wav"), names());
        assertEquals(2, attempts.get()); assertEquals(RepeatMode.OFF, handler.getRepeatMode());
    }

    private static final class EmptyTrack extends BaseAudioTrack {
        private final AtomicInteger attempts;
        EmptyTrack(AtomicInteger attempts) {
            super(new AudioTrackInfo("empty", "test", 1000, "empty", false, "local-test:empty"));
            this.attempts = attempts;
        }
        @Override public void process(LocalAudioTrackExecutor executor) throws Exception { attempts.incrementAndGet(); executor.waitOnEnd(); }
        @Override protected AudioTrack makeShallowClone() { return new EmptyTrack(attempts); }
    }
    private static final class FailingTrack extends BaseAudioTrack {
        private final CountDownLatch gate;
        private final AtomicInteger attempts;
        FailingTrack(CountDownLatch gate, AtomicInteger attempts) {
            super(new AudioTrackInfo("broken", "test", 1000, "broken", false, "local-test:broken"));
            this.gate = gate; this.attempts = attempts;
        }
        @Override public void process(LocalAudioTrackExecutor executor) throws Exception {
            gate.await(2, TimeUnit.SECONDS); attempts.incrementAndGet();
            throw new IOException("Deliberate local loading failure");
        }
        @Override protected AudioTrack makeShallowClone() { return new FailingTrack(gate, attempts); }
    }
}