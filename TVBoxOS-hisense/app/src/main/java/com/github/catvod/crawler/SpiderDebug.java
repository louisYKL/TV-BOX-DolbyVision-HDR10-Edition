package com.github.catvod.crawler;

import com.github.tvbox.osc.util.LOG;

public class SpiderDebug {
    public static void log(Throwable th) {
        try {
            if (th == null) {
                return;
            }
            String message = th.getMessage();
            if (message == null) {
                message = "";
            }
            android.util.Log.d("SpiderLog", message, th);
            if (!message.trim().isEmpty() && !"null".equalsIgnoreCase(message.trim())) {
                LOG.e("SpiderLog " + th.getClass().getSimpleName() + ": " + message);
            } else {
                LOG.e("SpiderLog " + th.getClass().getSimpleName());
            }
        } catch (Throwable th1) {

        }
    }

    public static void log(String msg) {
        try {
            android.util.Log.d("SpiderLog", msg);
            LOG.i("SpiderLog " + msg);
        } catch (Throwable th1) {

        }
    }

    public static String ec(int i) {
        return "";
    }
}
