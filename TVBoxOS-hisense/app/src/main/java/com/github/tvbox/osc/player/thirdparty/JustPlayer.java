package com.github.tvbox.osc.player.thirdparty;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.github.tvbox.osc.base.App;

import java.util.HashMap;

public class JustPlayer {
    public static final String TAG = "ThirdParty.JustPlayer";
    private static final String PACKAGE_NAME = "com.brouken.player";

    private JustPlayer() {
    }

    public static String getPackageInfo() {
        try {
            ApplicationInfo info = App.getInstance().getPackageManager().getApplicationInfo(PACKAGE_NAME, 0);
            return info.enabled ? PACKAGE_NAME : null;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.v(TAG, "Just Player package `" + PACKAGE_NAME + "` does not exist.");
            return null;
        }
    }

    public static boolean run(Activity activity, String url, String title, String subtitle, HashMap<String, String> headers) {
        String packageName = getPackageInfo();
        if (activity == null || packageName == null || url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(packageName);
            intent.setDataAndType(Uri.parse(url), "video/*");
            intent.putExtra(Intent.EXTRA_TITLE, title);
            intent.putExtra("title", title);
            intent.putExtra("name", title);
            if (subtitle != null && !subtitle.trim().isEmpty()) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subtitle);
                intent.putExtra("subtitle", subtitle);
                intent.putExtra("subs", subtitle);
            }
            activity.startActivity(intent);
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Can't run Just Player", ex);
            return false;
        }
    }
}
