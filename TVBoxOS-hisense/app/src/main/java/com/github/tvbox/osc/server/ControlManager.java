package com.github.tvbox.osc.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.SearchReceiver;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
/**
 * @author pj567
 * @date :2021/1/4
 * @description:
 */
public class ControlManager {
    private static ControlManager instance;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private RemoteServer mServer = null;
    public static Context mContext;

    private ControlManager() {

    }

    public static ControlManager get() {
        if (instance == null) {
            synchronized (ControlManager.class) {
                if (instance == null) {
                    instance = new ControlManager();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public String getAddress(boolean local) {
        if (mServer == null) {
            return "";
        }
        return local ? mServer.getLoadAddress() : mServer.getServerAddress();
    }

    public void startServer() {
        if (mServer != null) {
            return;
        }
        do {
            mServer = new RemoteServer(RemoteServer.serverPort, mContext);
            mServer.setDataReceiver(new DataReceiver() {
                @Override
                public void onTextReceived(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("title", text);
                        intent.setAction(SearchReceiver.action);
                        intent.setPackage(mContext.getPackageName());
                        intent.setComponent(new ComponentName(mContext, SearchReceiver.class));
                        intent.putExtras(bundle);
                        mContext.sendBroadcast(intent);
                    }
                }

                @Override
                public void onApiReceived(String url) {
                    onApiReceived(url, "", true, false);
                }

                @Override
                public void onApiReceived(String url, String liveUrl) {
                    onApiReceived(url, liveUrl, true, true);
                }

                @Override
                public void onApiReceived(String url, String liveUrl, boolean hasVodChange, boolean hasLiveChange) {
                    boolean changed = false;
                    if (hasVodChange) {
                        String newApi = url == null ? "" : url.trim();
                        String oldApi = Hawk.get(HawkConfig.API_URL, "");
                        if (!TextUtils.equals(oldApi, newApi)) {
                            Hawk.put(HawkConfig.API_URL, newApi);
                            if (!TextUtils.isEmpty(newApi)) {
                                HistoryHelper.setApiHistory(newApi);
                            }
                            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, newApi));
                            changed = true;
                        }
                    }
                    if (hasLiveChange) {
                        String newLiveApi = liveUrl == null ? "" : liveUrl.trim();
                        String oldLiveApi = Hawk.get(HawkConfig.LIVE_API_URL, "");
                        if (!TextUtils.equals(oldLiveApi, newLiveApi)) {
                            Hawk.put(HawkConfig.LIVE_API_URL, newLiveApi);
                            if (!TextUtils.isEmpty(newLiveApi)) {
                                HistoryHelper.setLiveApiHistory(newLiveApi);
                            }
                            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_LIVE_API_URL_CHANGE, newLiveApi));
                            changed = true;
                        }
                    }
                    if (changed) {
                        relaunchHomeForConfigRefresh(hasVodChange, hasLiveChange);
                    }
                }

                @Override
                public void onPushReceived(String url) {
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url));
                }
            });
            try {
                mServer.start();
                break;
            } catch (IOException ex) {
                RemoteServer.serverPort++;
                mServer.stop();
            }
        } while (RemoteServer.serverPort < 9999);
    }

    private void relaunchHomeForConfigRefresh(boolean hasVodChange, boolean hasLiveChange) {
        MAIN_HANDLER.post(() -> {
            try {
                if (hasVodChange) {
                    ApiConfig.get().clearLoader();
                }
                AppManager.getInstance().finishAllActivity();
            } catch (Throwable ignored) {
            }
            Intent intent = new Intent(mContext, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("useCache", !hasVodChange && hasLiveChange);
            mContext.startActivity(intent);
        });
    }

    public void stopServer() {
        if (mServer != null) {
            if (mServer.isStarting()) {
                mServer.stop();
            }
            mServer = null;
        }
    }
}
