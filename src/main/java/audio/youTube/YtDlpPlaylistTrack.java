package audio.youTube;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

final class YtDlpPlaylistTrack extends DelegatedAudioTrack {
    private final YtDlpResolver resolver;
    private final AudioPlayerManager playerManager;

    YtDlpPlaylistTrack(AudioTrackInfo info, YtDlpResolver resolver, AudioPlayerManager playerManager) {
        super(info);
        this.resolver = resolver;
        this.playerManager = playerManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        YtDlpResolvedTrack resolved = resolver.resolveBlocking(getIdentifier());
        var item = playerManager.loadItemSync(new AudioReference(resolved.streamUrl(), resolved.title()));
        if (!(item instanceof InternalAudioTrack track)) {
            throw new FriendlyException("Could not load the audio stream", FriendlyException.Severity.COMMON, null);
        }

        setUserData(resolved);
        processDelegate(track, executor);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new YtDlpPlaylistTrack(trackInfo, resolver, playerManager);
    }
}
