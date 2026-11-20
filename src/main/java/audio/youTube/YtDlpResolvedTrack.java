package audio.youTube;

public record YtDlpResolvedTrack(
        String title,
        String author,
        long durationMs,
        String webpageUrl,
        String thumbnailUrl,
        String streamUrl
) {
}
