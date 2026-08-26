warning: in the working copy of 'src/main/java/audio/TrackHandler.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'src/main/java/commands/play/PlayUseCase.java', LF will be replaced by CRLF the next time Git touches it
[1mdiff --git a/src/main/java/audio/TrackHandler.java b/src/main/java/audio/TrackHandler.java[m
[1mindex b86c40a..e6f4374 100644[m
[1m--- a/src/main/java/audio/TrackHandler.java[m
[1m+++ b/src/main/java/audio/TrackHandler.java[m
[36m@@ -2,6 +2,7 @@[m [mpackage audio;[m
 [m
 import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;[m
 import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;[m
[32m+[m[32mimport com.sedmelluq.discord.lavaplayer.tools.FriendlyException;[m
 import com.sedmelluq.discord.lavaplayer.track.AudioTrack;[m
 import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;[m
 [m
[36m@@ -18,6 +19,9 @@[m [mpublic class TrackHandler extends AudioEventAdapter {[m
     }[m
 [m
     public synchronized void queue(AudioTrack track) {[m
[32m+[m[32m        if (player.getPlayingTrack() == null && player.isPaused()) {[m
[32m+[m[32m            player.setPaused(false);[m
[32m+[m[32m        }[m
         if (!player.startTrack(track, true)) {[m
             queue.offer(track);[m
         } else {[m
[36m@@ -32,6 +36,7 @@[m [mpublic class TrackHandler extends AudioEventAdapter {[m
     public synchronized void stopAll() {[m
         queue.clear();[m
         player.stopTrack();[m
[32m+[m[32m        player.setPaused(false);[m
     }[m
 [m
     // for skip[m
[36m@@ -47,6 +52,9 @@[m [mpublic class TrackHandler extends AudioEventAdapter {[m
 [m
     @Override[m
     public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {[m
[32m+[m[32m        System.out.println("[TrackHandler][end] reason=" + endReason[m
[32m+[m[32m                + ", title=" + track.getInfo().title[m
[32m+[m[32m                + ", uri=" + track.getInfo().uri);[m
         if (endReason.mayStartNext) {[m
             var started = nextTrack();[m
             if (started == null) {[m
[36m@@ -60,6 +68,23 @@[m [mpublic class TrackHandler extends AudioEventAdapter {[m
         audio.MusicCore.getInstance().notifyPlaybackStateChangedByPlayer(player);[m
     }[m
 [m
[32m+[m[32m    @Override[m
[32m+[m[32m    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {[m
[32m+[m[32m        System.err.println("[TrackHandler][exception] severity=" + exception.severity[m
[32m+[m[32m                + ", title=" + track.getInfo().title[m
[32m+[m[32m                + ", uri=" + track.getInfo().uri[m
[32m+[m[32m                + ", position=" + track.getPosition());[m
[32m+[m[32m        exception.printStackTrace();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    @Override[m
[32m+[m[32m    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {[m
[32m+[m[32m        System.err.println("[TrackHandler][stuck] thresholdMs=" + thresholdMs[m
[32m+[m[32m                + ", title=" + track.getInfo().title[m
[32m+[m[32m                + ", uri=" + track.getInfo().uri[m
[32m+[m[32m                + ", position=" + track.getPosition());[m
[32m+[m[32m    }[m
[32m+[m
     public boolean isQueueEmpty() {[m
         return queue.isEmpty();[m
     }[m
[1mdiff --git a/src/main/java/commands/play/PlayUseCase.java b/src/main/java/commands/play/PlayUseCase.java[m
[1mindex fa143a0..294e7a3 100644[m
[1m--- a/src/main/java/commands/play/PlayUseCase.java[m
[1m+++ b/src/main/java/commands/play/PlayUseCase.java[m
[36m@@ -102,6 +102,7 @@[m [mpublic final class PlayUseCase {[m
 [m
             @Override[m
             public void noMatches() {[m
[32m+[m[32m                scheduleAfkIfIdle(guild.getIdLong());[m
                 result.complete(new PlayResult.Failure(PlayResult.FailureReason.NO_MATCHES));[m
             }[m
 [m
[36m@@ -109,6 +110,7 @@[m [mpublic final class PlayUseCase {[m
             public void loadFailed(FriendlyException exception) {[m
                 System.err.println("[PlayUseCase][loadFailed] severity=" + exception.severity);[m
                 exception.printStackTrace();[m
[32m+[m[32m                scheduleAfkIfIdle(guild.getIdLong());[m
                 result.complete(new PlayResult.Failure([m
                         PlayResult.FailureReason.LOAD_FAILED,[m
                         exception.getMessage()[m
[36m@@ -121,4 +123,10 @@[m [mpublic final class PlayUseCase {[m
     private CompletionStage<PlayResult> failure(PlayResult.FailureReason reason) {[m
         return CompletableFuture.completedFuture(new PlayResult.Failure(reason));[m
     }[m
[32m+[m
[32m+[m[32m    private void scheduleAfkIfIdle(long guildId) {[m
[32m+[m[32m        if (musicCore.isIdle(guildId)) {[m
[32m+[m[32m            musicCore.scheduleAfkDisconnect(guildId);[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
 }[m
