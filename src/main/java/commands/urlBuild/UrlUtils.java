package commands.urlBuild;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class UrlUtils {
    private UrlUtils() {}

    static URI tryParse(String input) {
        try {
            return URI.create(input);
        } catch (Exception e) {
            return null;
        }
    }

    static String getHostLower(URI uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase();
    }

    static String getPathOrEmpty(URI uri) {
        String path = uri.getPath();
        return path == null ? "" : path;
    }

    static String getFirstSegment(String path) {
        if  (path == null) return null;

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int slashIndex = normalized.indexOf('/');
        String firstSegment = (slashIndex >= 0) ? normalized.substring(0, slashIndex) : normalized;
        return firstSegment.isEmpty() ? null : firstSegment;
    }

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> parameters = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return parameters;

        for (String pair : rawQuery.split("&")) {
            int equalsIndex = pair.indexOf('=');
            String key = URLDecoder.decode(equalsIndex >= 0 ? pair.substring(0, equalsIndex) : pair, StandardCharsets.UTF_8);
            String value = URLDecoder.decode(equalsIndex >= 0 ? pair.substring(equalsIndex + 1) : "", StandardCharsets.UTF_8);
            parameters.put(key, value);
        }

        parameters.remove("pp");
        parameters.remove("feature");
        parameters.remove("si");
        parameters.remove("t");
        parameters.remove("start_radio");
        return parameters;
    }
}
