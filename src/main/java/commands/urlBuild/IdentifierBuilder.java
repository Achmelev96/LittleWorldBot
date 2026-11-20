package commands.urlBuild;

import java.net.URI;

public final class IdentifierBuilder {
    private IdentifierBuilder() {}

    public static String build(String rawInput) {
        if (rawInput == null) return null;

        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) return null;

        URI uri = UrlUtils.tryParse(trimmed);
        if (uri == null || uri.getScheme() == null) {
            return "ytsearch:" + trimmed;
        }

        if (YouTubeNormalizer.isYoutube(uri)) {
            String playlistId = UrlUtils.getPlaylistId(uri);

            if (UrlUtils.isPlaylistUrl(uri)) {
                if (playlistId != null && !UrlUtils.isRadioPlaylistId(playlistId)) {
                    return YouTubeNormalizer.normalize(uri);
                }
                return YouTubeNormalizer.normalize(uri);
            }
            return YouTubeNormalizer.normalize(uri);
        }
        return trimmed;
    }
}
