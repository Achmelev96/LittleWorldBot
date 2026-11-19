package commands.urlBuild;

import java.net.URI;
import java.util.Map;

final class YouTubeNormalizer {

    private YouTubeNormalizer() {}

    static boolean isYoutube(URI uri) {
        String scheme = uri.getScheme();
        String host = UrlUtils.getHostLower(uri);
        return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                && (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be"));
    }

    static String normalize(URI uri) {
        String host = UrlUtils.getHostLower(uri);
        String path = UrlUtils.getPathOrEmpty(uri);
        Map<String, String> params = UrlUtils.parseQuery(uri.getRawQuery());
        String playlistId = params.get("list");

        boolean isRegularPlaylist = playlistId != null && playlistId.matches("[A-Za-z0-9_-]+")
                && !UrlUtils.isRadioPlaylistId(playlistId) && !params.containsKey("start_radio");
        String playlistUrl = isRegularPlaylist ? "https://youtube.com/playlist?list=" + playlistId : null;

        if (UrlUtils.isPlaylistUrl(uri)) {
            return playlistUrl;
        }

        String videoId;
        if (host.equals("youtu.be")) {
            videoId = UrlUtils.getFirstSegment(path);
        } else if ("/watch".equals(path)) {
            videoId = params.get("v");
        } else if (path.startsWith("/shorts/") || path.startsWith("/live/") || path.startsWith("/embed/")) {
            videoId = UrlUtils.getFirstSegment(path.substring(path.indexOf('/', 1)));
        } else {
            return playlistId == null && !params.containsKey("start_radio") ? uri.toString() : null;
        }

        if (playlistUrl != null) return playlistUrl;

        return videoId != null && videoId.matches("[A-Za-z0-9_-]{11}")
                ? "https://youtube.com/watch?v=" + videoId
                : null;
    }
}
