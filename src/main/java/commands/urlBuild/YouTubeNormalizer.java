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

        if (host.contains("youtu.be")) {
            String videoId = UrlUtils.getFirstSegment(path);
            return videoId != null ? buildMusicWatch(videoId, null, null) : uri.toString();
        }

        if (path.startsWith("/shorts/")) {
            String videoId = UrlUtils.getFirstSegment(path.substring("/shorts/".length()));
            return videoId != null ? buildMusicWatch(videoId, null, null) : uri.toString();
        }

        if ("/watch".equals(path)) {
            Map<String, String> queryParams = UrlUtils.parseQuery(uri.getRawQuery());
            String videoId = queryParams.get("v");
            if (videoId == null || videoId.isBlank()) return uri.toString();

            String playlistId = queryParams.get("list");
            String playlistIndex = queryParams.get("index");

            boolean isMix = playlistId != null && playlistId.startsWith("RD");
            return buildMusicWatch(videoId, isMix ? null : playlistId, isMix ? null : playlistIndex);
        }

        return uri.toString();
    }

    private static String buildMusicWatch(String videoId, String list, String index) {
        StringBuilder sb = new StringBuilder("https://music.youtube.com/watch?v=").append(videoId);
        if (list != null && !list.isBlank()) {
            sb.append("&list=").append(list);
            if (index != null && !index.isBlank()) {
                sb.append("&index=").append(index);
            }
        }
        return sb.toString();
    }

}
