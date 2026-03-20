package commands.urlBuild;

import java.net.URI;
import java.util.Map;

final class YouTubeNormalizer {

    private YouTubeNormalizer() {}

    static boolean isYoutube(URI uri) {
        String host = UrlUtils.getHostLower(uri);
        return host.contains("youtube.com") || host.contains("youtu.be");
    }

    static String normalize(URI uri) {
        String host = UrlUtils.getHostLower(uri);
        String path = UrlUtils.getPathOrEmpty(uri);

        if (UrlUtils.isPlaylistUrl(uri)) {
            String playlistId = UrlUtils.getPlaylistId(uri);
            if (playlistId == null || playlistId.isBlank()) {
                return uri.toString();
            }
            return "https://youtube.com/playlist?list=" + playlistId;
        }

        if (host.contains("youtu.be")) {
            String videoId = UrlUtils.getFirstSegment(path);
            if (videoId == null || videoId.isBlank()) {
                return uri.toString();
            }
            return "https://youtube.com/watch?v=" + videoId;
        }

        if ("/watch".equals(path)) {
            Map<String, String> queryParams = UrlUtils.parseQuery(uri.getRawQuery());
            String videoId = queryParams.get("v");

            if (videoId == null || videoId.isBlank()) {
                return uri.toString();
            }
            return "https://youtube.com/watch?v=" + videoId;
        }
        return uri.toString();
    }
}
