package com.github.tvbox.osc.util.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtSubscribe {
    private static final Pattern NAME_PATTERN = Pattern.compile(".*,(.+?)$");
    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"(.*?)\"");
    private static final Pattern KODI_PROP_PATTERN = Pattern.compile("#KODIPROP:([^=]+)=(.+)");
    private static final Pattern VLC_OPT_PATTERN = Pattern.compile("#EXTVLCOPT:([^=]+)=(.+)");

    public static void parse(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        if (str == null) {
            return;
        }
        str = str.replace("\ufeff", "").trim();
        if (str.regionMatches(true, 0, "#EXTM3U", 0, 7)) {
            parseM3u(linkedHashMap, str);
        } else {
            parseTxt(linkedHashMap, str);
        }
    }

    //解析m3u后缀
    private static void parseM3u(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        ArrayList<String> urls;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> channelTemp = linkedHashMap2;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("")) continue;
                if (line.startsWith("#EXTM3U")) continue;
                if (isSetting(line)) continue;
                if (line.startsWith("#EXTINF") || line.contains("#EXTINF")) {
                    String name = getStrByRegex(NAME_PATTERN, line);
                    String group = getStrByRegex(GROUP_PATTERN, line);
                    Map<String, String> inlineHeaders = new HashMap<>();
                    String nextLine = readNextPlayableLine(bufferedReader, inlineHeaders);
                    if (nextLine == null) {
                        break;
                    }
                    String url = nextLine.trim();
                    if (isUrl(url)) {
                        url = appendHeaders(url, inlineHeaders);
                        if (linkedHashMap.containsKey(group)) {
                            channelTemp = linkedHashMap.get(group);
                        } else {
                            channelTemp = new LinkedHashMap<>();
                            linkedHashMap.put(group, channelTemp);
                        }
                        if (null != channelTemp && channelTemp.containsKey(name)) {
                            urls = channelTemp.get(name);
                        } else {
                            urls = new ArrayList<>();
                            channelTemp.put(name, urls);
                        }
                        if (null != urls && !urls.contains(url)) urls.add(url);
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) return;
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String getStrByRegex(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) return matcher.group(1);
        return pattern.pattern().equals(GROUP_PATTERN.pattern()) ? "未分组" : "未命名";
    }

    private static boolean isUrl(String url) {
        return !url.isEmpty() && (url.startsWith("http")
                || url.startsWith("rtp")
                || url.startsWith("rtsp")
                || url.startsWith("rtmp")
                || url.startsWith("udp")
                || url.startsWith("rtspu")
                || url.startsWith("mitv")
                || url.startsWith("p2p"));
    }

    private static boolean isSetting(String line) {
        return line.startsWith("ua") || line.startsWith("parse") || line.startsWith("click") || line.startsWith("player") || line.startsWith("header") || line.startsWith("format") || line.startsWith("origin") || line.startsWith("referer") || line.startsWith("#EXTHTTP:") || line.startsWith("#EXTVLCOPT:") || line.startsWith("#KODIPROP:");
    }

    private static String readNextPlayableLine(BufferedReader bufferedReader, Map<String, String> inlineHeaders) throws Exception {
        String nextLine;
        while ((nextLine = bufferedReader.readLine()) != null) {
            String trimmed = nextLine.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("#EXTHTTP:")) {
                collectJsonHeader(trimmed.substring("#EXTHTTP:".length()), inlineHeaders);
                continue;
            }
            Matcher kodiPropMatcher = KODI_PROP_PATTERN.matcher(trimmed);
            if (kodiPropMatcher.matches()) {
                collectNamedHeader(kodiPropMatcher.group(1), kodiPropMatcher.group(2), inlineHeaders);
                continue;
            }
            Matcher vlcOptMatcher = VLC_OPT_PATTERN.matcher(trimmed);
            if (vlcOptMatcher.matches()) {
                collectNamedHeader(vlcOptMatcher.group(1), vlcOptMatcher.group(2), inlineHeaders);
                continue;
            }
            if (trimmed.startsWith("#")) {
                continue;
            }
            return trimmed;
        }
        return null;
    }

    private static void collectJsonHeader(String raw, Map<String, String> inlineHeaders) {
        if (raw == null) {
            return;
        }
        String content = raw.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
        }
        for (String part : content.split(",")) {
            String[] pair = part.split(":", 2);
            if (pair.length == 2) {
                inlineHeaders.put(cleanHeaderKey(pair[0]), cleanHeaderValue(pair[1]));
            }
        }
    }

    private static void collectNamedHeader(String rawKey, String rawValue, Map<String, String> inlineHeaders) {
        String key = cleanHeaderKey(rawKey);
        String value = cleanHeaderValue(rawValue);
        if (key.isEmpty() || value.isEmpty()) {
            return;
        }
        String lower = key.toLowerCase();
        if (lower.contains("user-agent") || lower.equals("http-user-agent")) {
            inlineHeaders.put("User-Agent", value);
        } else if (lower.contains("referer") || lower.equals("http-referrer")) {
            inlineHeaders.put("Referer", value);
        } else if (lower.contains("origin")) {
            inlineHeaders.put("Origin", value);
        } else {
            inlineHeaders.put(key, value);
        }
    }

    private static String cleanHeaderKey(String value) {
        return value == null ? "" : value.trim().replace("\"", "");
    }

    private static String cleanHeaderValue(String value) {
        return value == null ? "" : value.trim().replace("\"", "");
    }

    private static String appendHeaders(String url, Map<String, String> inlineHeaders) {
        if (inlineHeaders == null || inlineHeaders.isEmpty() || url.contains("@Headers=")) {
            return url;
        }
        StringBuilder builder = new StringBuilder(url);
        if (inlineHeaders.size() == 1 && inlineHeaders.containsKey("User-Agent")) {
            builder.append("@User-Agent=").append(inlineHeaders.get("User-Agent"));
            return builder.toString();
        }
        if (inlineHeaders.size() == 1 && inlineHeaders.containsKey("Referer")) {
            builder.append("@Referer=").append(inlineHeaders.get("Referer"));
            return builder.toString();
        }
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : inlineHeaders.entrySet()) {
            if (index > 0) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
            index++;
        }
        json.append("}");
        builder.append("@Headers=").append(json);
        return builder.toString();
    }

    //解析txt后缀
    public static void parseTxt(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        ArrayList<String> arrayList;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String readLine = bufferedReader.readLine();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap3 = linkedHashMap2;
            while (readLine != null) {
                if (readLine.trim().isEmpty() || readLine.startsWith("#")) {
                    readLine = bufferedReader.readLine();
                } else {
                        String[] split = readLine.split(",", 2);
                    if (split.length < 2) {
                        readLine = bufferedReader.readLine();
                    } else {
                        if (readLine.contains("#genre#")) {
                            String trim = split[0].trim();
                            if (!linkedHashMap.containsKey(trim)) {
                                linkedHashMap3 = new LinkedHashMap<>();
                                linkedHashMap.put(trim, linkedHashMap3);
                            } else {
                                linkedHashMap3 = linkedHashMap.get(trim);
                            }
                        } else {
                            String trim2 = split[0].trim();
                            for (String str2 : split[1].trim().split("#")) {
                                String trim3 = str2.trim();
                                if (isUrl(trim3)) {
                                    if (!linkedHashMap3.containsKey(trim2)) {
                                        arrayList = new ArrayList<>();
                                        linkedHashMap3.put(trim2, arrayList);
                                    } else {
                                        arrayList = linkedHashMap3.get(trim2);
                                    }
                                    if (!arrayList.contains(trim3)) {
                                        arrayList.add(trim3);
                                    }
                                }
                            }
                        }
                        readLine = bufferedReader.readLine();
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) {
                return;
            }
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Throwable unused) {
        }
    }

    public static JsonArray live2JsonArray(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap) {
        JsonArray jsonarr = new JsonArray();
        for (String str : linkedHashMap.keySet()) {
            JsonArray jsonarr2 = new JsonArray();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = linkedHashMap.get(str);
            if (!linkedHashMap2.isEmpty()) {
                for (String str2 : linkedHashMap2.keySet()) {
                    ArrayList<String> arrayList = linkedHashMap2.get(str2);
                    if (!arrayList.isEmpty()) {
                        JsonArray jsonarr3 = new JsonArray();
                        for (int i = 0; i < arrayList.size(); i++) {
                            jsonarr3.add(arrayList.get(i));
                        }
                        JsonObject jsonobj = new JsonObject();
                        try {
                            jsonobj.addProperty("name", str2);
                            jsonobj.add("urls", jsonarr3);
                        } catch (Throwable e) {
                        }
                        jsonarr2.add(jsonobj);
                    }
                }
                JsonObject jsonobj2 = new JsonObject();
                try {
                    jsonobj2.addProperty("group", str);
                    jsonobj2.add("channels", jsonarr2);
                } catch (Throwable e) {
                }
                jsonarr.add(jsonobj2);
            }
        }
        return jsonarr;
    }
}
