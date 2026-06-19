package com.github.tvbox.osc.player;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.doikki.videoplayer.player.AbstractPlayer;
import xyz.doikki.videoplayer.player.VideoView;

public class MyVideoView extends VideoView {
    private boolean keepSurfaceOnFullScreen;

    public MyVideoView(@NonNull Context context) {
        super(context, null);
    }

    public MyVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public MyVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public AbstractPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void persistProgressNow() {
        if (mMediaPlayer != null) {
            try {
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            } catch (Throwable ignored) {
            }
        }
        saveProgress();
    }

    public boolean isPlaybackActive() {
        return isInPlaybackState();
    }

    public void setKeepSurfaceOnFullScreen(boolean keepSurfaceOnFullScreen) {
        this.keepSurfaceOnFullScreen = keepSurfaceOnFullScreen;
    }

    @Override
    protected boolean shouldUseInPlaceFullScreen() {
        return keepSurfaceOnFullScreen;
    }
}
