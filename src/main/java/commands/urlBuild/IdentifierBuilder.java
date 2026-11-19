package commands.urlBuild;

import java.net.URI;

public final class IdentifierBuilder {
    private IdentifierBuilder() {}

    public static boolean isYouTubePlaylist(String identifier) {
        URI uri = UrlUtils.tryParse(identifier);
        return uri != null && YouTubeNormalizer.isYoutube(uri) && UrlUtils.isPlaylistUrl(uri);
    }

    public static boolean isYouTubeIdentifier(String identifier) {
        if (identifier.startsWith("ytsearch:")) return true;

        URI uri = UrlUtils.tryParse(identifier);
        return uri != null && YouTubeNormalizer.isYoutube(uri);
    }

    public static String build(String rawInput) {
        if (rawInput == null) return null;

        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) return null;

        URI uri = UrlUtils.tryParse(trimmed);
        if (uri == null || uri.getScheme() == null) {
            return "ytsearch:" + trimmed;
        }

        if (YouTubeNormalizer.isYoutube(uri)) {
            return YouTubeNormalizer.normalize(uri);
        }
        return trimmed;
    }
}
