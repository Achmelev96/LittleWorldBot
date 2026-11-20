package audio.playback;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.junit.Test;
import support.AudioTestSupport.FakePlayer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static support.AudioTestSupport.track;

public class TrackHandlerTest {
    private final FakePlayer fake = new FakePlayer();
    private final TrackHandler handler = new TrackHandler(fake.player);

    private AudioTrack end(AudioTrackEndReason reason) throws InterruptedException {
        var ended = fake.playing.getAndSet(null);
        handler.onTrackEnd(fake.player, ended, reason);
        return fake.awaitStart();
    }

    @Test
    public void naturalEndRepeatsAndThenContinuesQueue() throws Exception {
        handler.queue(track("1")); fake.awaitStart(); handler.queue(track("next"));
        assertEquals(RepeatMode.ONCE, handler.cycleRepeat());
        assertEquals("1", end(AudioTrackEndReason.FINISHED).getInfo().title);
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        assertEquals("next", end(AudioTrackEndReason.FINISHED).getInfo().title);
    }

    @Test
    public void loadFailureRetriesOnceThenAdvancesThroughRealEndCallbacks() throws Exception {
        handler.queuePlaylist(List.of(track("broken"), track("next"))); fake.awaitStart();
        handler.cycleRepeat(); handler.cycleRepeat();
        assertEquals("broken", end(AudioTrackEndReason.LOAD_FAILED).getInfo().title);
        assertEquals("next", end(AudioTrackEndReason.LOAD_FAILED).getInfo().title);
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
    }

    @Test
    public void manualSkipExitsLoopAndStaleEndCannotSkipNewTrack() throws Exception {
        handler.queuePlaylist(List.of(track("1"), track("2"), track("3")));
        var original = fake.awaitStart(); handler.cycleRepeat(); handler.cycleRepeat();
        assertEquals("2", handler.skip(original).current().getInfo().title); fake.awaitStart();
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        fake.playing.set(null);
        handler.onTrackEnd(fake.player, original, AudioTrackEndReason.FINISHED);
        assertNull(fake.starts.poll(150, TimeUnit.MILLISECONDS));
        assertEquals(1, handler.getQueueSize());
    }

    @Test
    public void skipPlaylistAndStopResetMode() throws Exception {
        handler.queuePlaylist(List.of(track("1"), track("2"))); fake.awaitStart();
        handler.queue(track("next")); handler.cycleRepeat();
        var id = handler.playlistWindow(fake.playing.get()).playlistId();
        assertTrue(handler.skipPlaylist(id));
        assertEquals("next", fake.awaitStart().getInfo().title);
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        handler.cycleRepeat(); fake.paused.set(true); handler.stopAll();
        assertNull(fake.playing.get()); assertFalse(fake.paused.get());
        assertEquals(RepeatMode.OFF, handler.getRepeatMode());
        assertTrue(handler.isQueueEmpty()); assertNull(handler.cycleRepeat());
    }

    @Test
    public void navigationPreservesLoopAndResumesSelectedTrack() throws Exception {
        handler.queuePlaylist(List.of(track("1"), track("2"))); fake.awaitStart();
        handler.cycleRepeat(); handler.cycleRepeat(); fake.paused.set(true);
        var id = handler.playlistWindow(fake.playing.get()).playlistId();
        assertTrue(handler.jumpToPlaylistTrack(id, 1)); fake.awaitStart();
        assertFalse(fake.paused.get()); assertEquals(RepeatMode.LOOP, handler.getRepeatMode());
        assertEquals("2", end(AudioTrackEndReason.FINISHED).getInfo().title);
        handler.clearQueue(); assertEquals(RepeatMode.OFF, handler.getRepeatMode());
    }
}