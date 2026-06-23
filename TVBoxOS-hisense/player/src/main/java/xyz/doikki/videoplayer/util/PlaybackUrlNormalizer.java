package xyz.doikki.videoplayer.util;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONObject;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class PlaybackUrlNormalizer {
    private PlaybackUrlNormalizer() {
    }

    public static UrlWithHeaders splitUrlAndHeaders(String path, Map<String, String> headers) {
        HashMap<String, String> mergedHeaders = headers == null ? new HashMap<>() : new HashMap<>(headers);
        if (TextUtils.isEmpty(path) || !path.contains("@")) {
            return new UrlWithHeaders(path, mergedHeaders);
        }
        String[] parts = path.split("@", 2);
        String cleanUrl = parts[0];
        String suffix = parts.length > 1 ? parts[1] : "";
        try {
            if (suffix.startsWith("Headers=")) {
                String rawJson = URLDecoder.decode(suffix.substring("Headers=".length()), "UTF-8");
                JSONObject jsonObject = new JSONObject(rawJson);
                for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
                    String key = iterator.next();
                    mergedHeaders.put(key, jsonObject.optString(key, ""));
                }
            } else {
                for (String token : suffix.split("@")) {
                    String[] kv = token.split("=", 2);
                    if (kv.length != 2) {
                        continue;
                    }
                    String key = kv[0];
                    String value = URLDecoder.decode(kv[1], "UTF-8");
                    if ("User-Agent".equalsIgnoreCase(key) || "Referer".equalsIgnoreCase(key) || "Origin".equalsIgnoreCase(key) || "Cookie".equalsIgnoreCase(key)) {
                        mergedHeaders.put(key, value);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new UrlWithHeaders(cleanUrl, mergedHeaders);
    }

    public static String normalizeHttpUrl(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        try {
            Uri parsed = Uri.parse(path);
            if (TextUtils.isEmpty(parsed.getScheme()) || parsed.isOpaque()) {
                return path;
            }
            URI normalized = new URI(
                    parsed.getScheme(),
                    parsed.getUserInfo(),
                    parsed.getHost(),
                    parsed.getPort(),
                    parsed.getPath(),
                    parsed.getQuery(),
                    parsed.getFragment()
            );
            return normalized.toASCIIString();
        } catch (Exception ignored) {
            return path;
        }
    }

    public static String unwrapAppStreamProxyToLocalPlay(String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        try {
            Uri uri = Uri.parse(path);
            String host = uri.getHost();
            String go = uri.getQueryParameter("go");
            String nestedUrl = uri.getQueryParameter("url");
            if (!("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host))
                    || (!"stream".equalsIgnoreCase(go) && !"play".equalsIgnoreCase(go))
                    || TextUtils.isEmpty(nestedUrl)) {
                return path;
            }
            Uri nestedUri = Uri.parse(nestedUrl);
            String nestedHost = nestedUri.getHost();
            String nestedPath = nestedUri.getPath();
            if (("127.0.0.1".equals(nestedHost) || "localhost".equalsIgnoreCase(nestedHost))
                    && nestedPath != null
                    && nestedPath.contains("/proxy/play/")) {
                return normalizeHttpUrl(nestedUrl);
            }
            String nestedNormalized = normalizeHttpUrl(nestedUrl);
            String doubleNested = unwrapAppStreamProxyToLocalPlay(nestedNormalized);
            if (!TextUtils.isEmpty(doubleNested) && !TextUtils.equals(doubleNested, nestedNormalized)) {
                return doubleNested;
            }
        } catch (Throwable ignored) {
        }
        return path;
    }

    public static boolean isHlsLike(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return false;
        }
        String lower = uri.toLowerCase(Locale.US);
        if (lower.contains(".m3u8") || lower.contains("type=hls") || lower.contains("format=hls")) {
            return true;
        }
        try {
            Uri parsedUri = Uri.parse(uri);
            String path = parsedUri.getPath();
            if (path == null) {
                return false;
            }
            path = path.toLowerCase(Locale.US);
            return path.endsWith("/live.php")
                    || path.contains("/live/")
                    || path.contains("/playlist/")
                    || path.contains("/m3u8")
                    || path.contains("index.m3u");
        } catch (Exception ignored) {
            return false;
        }
    }

    public static final class UrlWithHeaders {
        public final String url;
        public final Map<String, String> headers;

        public UrlWithHeaders(String url, Map<String, String> headers) {
            this.url = url;
            this.headers = headers;
        }
    }
}
