package com.github.tvbox.osc.util;

import android.text.TextUtils;

import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Locale;

public class HistoryHelper {
    private static final Integer[] hisNumArray = {30,50,100};

    public static String getHistoryNumName(int index){
        Integer value = getHisNum(index);
        return value + "条";
    }

    public static int getHisNum(int index){
        Integer value = null;
        if(index>=0 && index < hisNumArray.length){
            value = hisNumArray[index];
        }else{
            value = hisNumArray[0];
        }
        return value;
    }

    public static void setSearchHistory(String title){
        title = normalizeSearchHistory(title);
        if (!isUsefulSearchHistory(title)) {
            return;
        }
        // 读取历史记录
        ArrayList<String> history = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<String>());
        history.remove(title);
        history.add(0, title);
        // 保证最多只保留 15 条，超过的就删除最后一条
        if (history.size() > 15) {
            history.remove(history.size() - 1);
        }
        Hawk.put(HawkConfig.SEARCH_HISTORY, history);
    }

    public static ArrayList<String> getSearchHistory() {
        ArrayList<String> history = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<String>());
        ArrayList<String> cleanedHistory = new ArrayList<>();
        if (history == null) {
            return cleanedHistory;
        }
        for (String item : history) {
            String cleaned = normalizeSearchHistory(item);
            if (isUsefulSearchHistory(cleaned) && !cleanedHistory.contains(cleaned)) {
                cleanedHistory.add(cleaned);
            }
        }
        return cleanedHistory;
    }

    public static String normalizeSearchHistory(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<hl>", "")
                .replace("</hl>", "")
                .replace("《", "")
                .replace("》", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    public static boolean isUsefulSearchHistory(String value) {
        String normalized = normalizeSearchHistory(value);
        if (TextUtils.isEmpty(normalized)) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.US);
        if (lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.contains("127.0.0.1")
                || lower.contains("localhost")
                || lower.contains("/proxy/play/")
                || lower.contains("url=")) {
            return false;
        }
        return !normalized.matches("^[\\p{Punct}\\s]+$");
    }

    public static void setLiveApiHistory(String value){
        ArrayList<String> history = Hawk.get(HawkConfig.LIVE_API_HISTORY, new ArrayList<String>());
        if (!history.contains(value)) {
            history.add(0, value);
        }
        if (history.size() > 30) {
            history.remove(30);
        }
        Hawk.put(HawkConfig.LIVE_API_HISTORY, history);
    }

    public static void setApiHistory(String value){
        ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<String>());
        if (!history.contains(value)) {
            history.add(0, value);
        }
        if (history.size() > 30) {
            history.remove(30);
        }
        Hawk.put(HawkConfig.API_HISTORY, history);
    }
}
