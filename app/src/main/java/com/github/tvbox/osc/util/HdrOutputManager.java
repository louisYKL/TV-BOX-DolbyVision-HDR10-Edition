package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

public final class HdrOutputManager {
    private HdrOutputManager() {
    }

    public static boolean requestHdr(Context context, String reason) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LOG.i("echo-hdr-window skip sdk=" + Build.VERSION.SDK_INT + " reason=" + reason);
            return false;
        }
        Activity activity = findActivity(context);
        if (activity != null && Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(() -> requestHdr(activity, reason + "-main"));
            LOG.i("echo-hdr-window posted-main reason=" + reason);
            return true;
        }
        Window window = activity == null ? null : activity.getWindow();
        if (window == null) {
            LOG.i("echo-hdr-window skip no-window reason=" + reason);
            return false;
        }
        try {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.setColorMode(ActivityInfo.COLOR_MODE_HDR);
            window.setAttributes(attrs);
            WindowManager.LayoutParams applied = window.getAttributes();
            LOG.i("echo-hdr-window requested reason=" + reason
                    + " colorMode=" + applied.getColorMode()
                    + " hdrColorMode=" + ActivityInfo.COLOR_MODE_HDR
                    + " caps=" + HdrDeviceSupport.query(activity).summary);
            return true;
        } catch (Throwable th) {
            LOG.e("echo-hdr-window failed reason=" + reason + " err=" + th.getMessage());
            return false;
        }
    }

    public static void releaseHdr(Context context, String reason) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Activity activity = findActivity(context);
        if (activity != null && Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(() -> releaseHdr(activity, reason + "-main"));
            LOG.i("echo-hdr-window release-posted-main reason=" + reason);
            return;
        }
        Window window = activity == null ? null : activity.getWindow();
        if (window == null) {
            return;
        }
        try {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.setColorMode(ActivityInfo.COLOR_MODE_DEFAULT);
            window.setAttributes(attrs);
            LOG.i("echo-hdr-window released reason=" + reason
                    + " colorMode=" + window.getAttributes().getColorMode());
        } catch (Throwable th) {
            LOG.e("echo-hdr-window release-failed reason=" + reason + " err=" + th.getMessage());
        }
    }

    public static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
