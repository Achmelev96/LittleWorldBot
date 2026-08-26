package commands.play;

import audio.MusicCore;
import audio.TrackUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import commands.urlBuild.IdentifierBuilder;
import voice.CurrentStatus;
import voice.VoiceConnectionService;
import voice.VoiceStateValidator;
import voice.VoiceValidationResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class PlayUseCase {
    private final MusicCore musicCore;
    private final VoiceStateValidator voiceValidator;
    private final VoiceConnectionService voiceConnection;

    public PlayUseCase(
            MusicCore musicCore,
            VoiceStateValidator voiceValidator,
            VoiceConnectionService voiceConnection
    ) {
        this.musicCore = musicCore;
        this.voiceValidator = voiceValidator;
        this.voiceConnection = voiceConnection;
    }

    public CompletionStage<PlayResult> execute(CurrentStatus context, String rawQuery) {
        String identifier = IdentifierBuilder.build(rawQuery == null ? "" : rawQuery);
        if (identifier == null || identifier.isBlank()) {
            return CompletableFuture.completedFuture(
                    new PlayResult.Failure(PlayResult.FailureReason.EMPTY_QUERY)
            );
        }

        VoiceValidationResult userState = voiceValidator.requireUserInVoice(context);
        if (userState == VoiceValidationResult.GUILD_UNAVAILABLE) {
            return failure(PlayResult.FailureReason.GUILD_UNAVAILABLE);
        }
        if (userState != VoiceValidationResult.OK) {
            return failure(PlayResult.FailureReason.USER_NOT_IN_VOICE);
        }

        VoiceValidationResult permissionState = voiceValidator.requireJoinPermissions(context);
        if (permissionState == VoiceValidationResult.MISSING_PERMISSIONS) {
            return failure(PlayResult.FailureReason.MISSING_PERMISSIONS);
        }
        if (permissionState != VoiceValidationResult.OK) {
            return failure(PlayResult.FailureReason.GUILD_UNAVAILABLE);
        }

        var guild = context.guild();
        var guildHandler = musicCore.getGuildHandler(guild);
        voiceConnection.prepareAudioSender(context, guildHandler);

        if (voiceValidator.requireSameChannel(context) != VoiceValidationResult.OK
                && !voiceConnection.connectToUser(context)) {
            return failure(PlayResult.FailureReason.CONNECTION_FAILED);
        }

        CompletableFuture<PlayResult> result = new CompletableFuture<>();
        musicCore.getPlayerManager().loadItemOrdered(guild, identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                guildHandler.getScheduler().queue(track);
                musicCore.cancelAfkDisconnect(guild.getIdLong());
                result.complete(new PlayResult.TrackQueued(
                        track.getInfo().title,
                        TrackUtils.formatDuration(track.getInfo().length)
                ));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    result.complete(new PlayResult.Failure(PlayResult.FailureReason.NO_MATCHES));
                    return;
                }

                if (playlist.isSearchResult()) {
                    AudioTrack first = playlist.getTracks().getFirst();
                    guildHandler.getScheduler().queue(first);
                    musicCore.cancelAfkDisconnect(guild.getIdLong());
                    result.complete(new PlayResult.SearchResultQueued(first.getInfo().title));
                    return;
                }

                int trackCount = 0;
                for (AudioTrack track : playlist.getTracks()) {
                    guildHandler.getScheduler().queue(track);
                    trackCount++;
                }

                musicCore.cancelAfkDisconnect(guild.getIdLong());
                result.complete(new PlayResult.PlaylistQueued(playlist.getName(), trackCount));
            }

            @Override
            public void noMatches() {
                scheduleAfkIfIdle(guild.getIdLong());
                result.complete(new PlayResult.Failure(PlayResult.FailureReason.NO_MATCHES));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                System.err.println("[PlayUseCase][loadFailed] severity=" + exception.severity);
                exception.printStackTrace();
                scheduleAfkIfIdle(guild.getIdLong());
                result.complete(new PlayResult.Failure(
                        PlayResult.FailureReason.LOAD_FAILED,
                        exception.getMessage()
                ));
            }
        });
        return result;
    }

    private CompletionStage<PlayResult> failure(PlayResult.FailureReason reason) {
        return CompletableFuture.completedFuture(new PlayResult.Failure(reason));
    }

    private void scheduleAfkIfIdle(long guildId) {
        if (musicCore.isIdle(guildId)) {
            musicCore.scheduleAfkDisconnect(guildId);
        }
    }
}
