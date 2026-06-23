package com.github.tvbox.osc.util;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;

import com.github.tvbox.osc.base.App;

public class PlayerCapability {
    private PlayerCapability() {
    }

    public static boolean supportsAudioPassthrough() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        try {
            AudioManager audioManager = (AudioManager) App.getInstance().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return false;
            }
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            if (devices == null || devices.length == 0) {
                return false;
            }
            for (AudioDeviceInfo device : devices) {
                if (device == null) {
                    continue;
                }
                int type = device.getType();
                if (type != AudioDeviceInfo.TYPE_HDMI
                        && type != AudioDeviceInfo.TYPE_HDMI_ARC
                        && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || type != AudioDeviceInfo.TYPE_HDMI_EARC)) {
                    continue;
                }
                for (int encoding : device.getEncodings()) {
                    if (encoding == AudioFormat.ENCODING_AC3
                            || encoding == AudioFormat.ENCODING_E_AC3
                            || encoding == AudioFormat.ENCODING_DTS
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && encoding == AudioFormat.ENCODING_DTS_HD)
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && encoding == AudioFormat.ENCODING_IEC61937)
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && encoding == AudioFormat.ENCODING_DOLBY_TRUEHD)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }
}
