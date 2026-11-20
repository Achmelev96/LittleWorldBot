package audio.playback;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static support.AudioTestSupport.track;

public class PlaybackQueueTest {
    @Test
    public void singleTrackRepeatsOnceBeforeRemainingQueue() {
        var queue = new PlaybackQueue();
        var original = track("a"); var next = track("next");
        queue.add(original); queue.add(next); queue.advance();
        assertEquals(RepeatMode.ONCE, queue.cycleRepeat(original));
        AudioTrack repeated = queue.finish();
        assertNotSame(original, repeated);
        assertEquals("a", repeated.getInfo().title);
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertSame(next, queue.finish());
        assertNull(queue.finish());
    }

    @Test
    public void repeatFromThirdTrackRestartsWholePlaylistExactlyOnce() {
        var queue = new PlaybackQueue();
        queue.addPlaylist(List.of(track("1"), track("2"), track("3"), track("4"), track("5")));
        var next = track("next"); queue.add(next);
        queue.advance(); queue.finish(); var third = queue.finish();
        String playlistId = queue.window(third).playlistId();
        queue.cycleRepeat(third);
        assertEquals("4", queue.finish().getInfo().title);
        assertEquals("5", queue.finish().getInfo().title);
        assertEquals(RepeatMode.ONCE, queue.repeatMode());
        var first = queue.finish();
        assertEquals("1", first.getInfo().title);
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertEquals(playlistId, queue.window(first).playlistId());
        for (int i = 2; i <= 5; i++) assertEquals(Integer.toString(i), queue.finish().getInfo().title);
        assertSame(next, queue.finish());
        assertNull(queue.finish());
    }

    @Test
    public void loopRepeatsOnlyCurrentPlaylistTrackUntilDisabled() {
        var queue = new PlaybackQueue();
        var first = track("1"); var second = track("2");
        queue.addPlaylist(List.of(first, second)); queue.advance();
        queue.cycleRepeat(first); queue.cycleRepeat(first);
        AudioTrack playing = first;
        for (int i = 0; i < 10; i++) {
            var repeated = queue.finish();
            assertNotSame(playing, repeated);
            assertEquals("1", repeated.getInfo().title);
            assertEquals(0, queue.window(repeated).currentIndex());
            assertEquals(1, queue.size());
            playing = repeated;
        }
        assertEquals(RepeatMode.OFF, queue.cycleRepeat(playing));
        assertSame(second, queue.finish());
    }

    @Test
    public void skipLeavesLoopAndCancelsSingleTrackRepeat() {
        for (boolean loop : List.of(false, true)) {
            var queue = new PlaybackQueue();
            var first = track("1"); var next = track("next");
            queue.add(first); queue.add(next); queue.advance(); queue.cycleRepeat(first);
            if (loop) queue.cycleRepeat(first);
            assertSame(next, queue.skip());
            assertEquals(RepeatMode.OFF, queue.repeatMode());
        }
    }

    @Test
    public void skipWithinPlaylistKeepsOneShotRepeatIncludingAtLastTrack() {
        var queue = new PlaybackQueue();
        var first = track("1"); queue.addPlaylist(List.of(first, track("2")));
        var next = track("next"); queue.add(next); queue.advance(); queue.cycleRepeat(first);
        assertEquals("2", queue.skip().getInfo().title);
        assertEquals(RepeatMode.ONCE, queue.repeatMode());
        assertEquals("1", queue.skip().getInfo().title);
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        queue.finish();
        assertSame(next, queue.finish());
    }

    @Test
    public void navigationPreservesModeAndLoopFollowsSelection() {
        var queue = new PlaybackQueue();
        var first = track("1"); queue.addPlaylist(List.of(first, track("2"), track("3")));
        queue.advance(); queue.cycleRepeat(first); queue.cycleRepeat(first);
        var window = queue.window(first);
        var third = queue.jump(window, window.playlistId(), 2);
        assertEquals(RepeatMode.LOOP, queue.repeatMode());
        assertEquals("3", queue.finish().getInfo().title);
        window = queue.window(queue.finish());
        var back = queue.jump(window, window.playlistId(), 0);
        assertEquals("1", back.getInfo().title);
        assertEquals("1", queue.finish().getInfo().title);
        assertEquals(RepeatMode.LOOP, queue.repeatMode());
    }

    @Test
    public void skipPlaylistCancelsRepeatAndKeepsFollowingPlaylist() {
        var queue = new PlaybackQueue();
        var first = track("1"); queue.addPlaylist(List.of(first, track("2")));
        var next = track("next"); queue.addPlaylist(List.of(next));
        queue.advance(); queue.cycleRepeat(first); queue.cycleRepeat(first);
        var id = queue.window(first).playlistId();
        assertFalse(queue.discardPlaylist("stale", first));
        assertEquals(RepeatMode.LOOP, queue.repeatMode());
        assertTrue(queue.discardPlaylist(id, first));
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertSame(next, queue.advance());
        assertFalse(queue.discardPlaylist(id, next));
    }

    @Test
    public void errorsGetOneRetryThenExitLoopAndAdvance() {
        var queue = new PlaybackQueue();
        var first = track("broken"); var next = track("next");
        queue.addPlaylist(List.of(first, next)); queue.advance();
        queue.cycleRepeat(first); queue.cycleRepeat(first);
        var retry = queue.retryOrAdvance();
        assertNotSame(first, retry);
        assertEquals("broken", retry.getInfo().title);
        assertEquals(RepeatMode.LOOP, queue.repeatMode());
        assertSame(next, queue.retryOrAdvance());
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertEquals("next", queue.retryOrAdvance().getInfo().title);
        assertNull(queue.retryOrAdvance());
    }

    @Test
    public void failedSingleTrackDoesNotSpendAnotherRepeatOnBrokenAudio() {
        var queue = new PlaybackQueue();
        var first = track("broken"); var next = track("next");
        queue.add(first); queue.add(next); queue.advance(); queue.cycleRepeat(first);
        assertEquals("broken", queue.retryOrAdvance().getInfo().title);
        assertSame(next, queue.retryOrAdvance());
        assertEquals(RepeatMode.OFF, queue.repeatMode());
    }

    @Test
    public void playlistRepeatSurvivesBrokenTrackButCannotRetryForever() {
        var queue = new PlaybackQueue();
        var first = track("broken"); queue.addPlaylist(List.of(first, track("2")));
        var next = track("next"); queue.add(next); queue.advance(); queue.cycleRepeat(first);
        queue.retryOrAdvance();
        assertEquals("2", queue.retryOrAdvance().getInfo().title);
        assertEquals(RepeatMode.ONCE, queue.repeatMode());
        assertEquals("broken", queue.finish().getInfo().title);
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        queue.retryOrAdvance();
        assertEquals("2", queue.retryOrAdvance().getInfo().title);
        assertSame(next, queue.finish());
    }

    @Test
    public void singleEntryPlaylistRepeatsOnceAndPreservesItsIdentity() {
        var queue = new PlaybackQueue();
        var track = track("1"); queue.addPlaylist(List.of(track)); queue.advance();
        var id = queue.window(track).playlistId(); queue.cycleRepeat(track);
        assertEquals(id, queue.window(queue.finish()).playlistId());
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertNull(queue.finish());
    }

    @Test
    public void clearResetsModeAndRetryBudgetAndQueuesAreIndependent() {
        var queue = new PlaybackQueue(); var other = new PlaybackQueue();
        var first = track("1"); queue.add(first); queue.advance(); queue.cycleRepeat(first);
        assertEquals(RepeatMode.OFF, other.repeatMode());
        queue.retryOrAdvance(); queue.clear();
        assertEquals(RepeatMode.OFF, queue.repeatMode());
        assertTrue(queue.isEmpty()); assertNull(queue.cycleRepeat(null));
        queue.add(first); queue.advance();
        assertEquals("1", queue.retryOrAdvance().getInfo().title);
        assertNull(queue.retryOrAdvance());
    }
}