package com.github.tvbox.osc.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class LOG {
    private static String TAG = "TVBox-runtime";

    public static void e(String msg) {
        String line = "" + msg;
        Log.e(TAG, line);
        appendToFile("E", line);
    }

    public static void i(String msg) {
        String line = "" + msg;
        Log.i(TAG, line);
        appendToFile("I", line);
    }

    private static synchronized void appendToFile(String level, String msg) {
        try {
            File file = StorageBudgetManager.getLogFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String line = time + " " + level + "/" + TAG + " " + msg + "\n";
            try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                outputStream.write(line.getBytes(StandardCharsets.UTF_8));
            }
            StorageBudgetManager.trimLogs();
        } catch (Throwable ignored) {
        }
    }
}
