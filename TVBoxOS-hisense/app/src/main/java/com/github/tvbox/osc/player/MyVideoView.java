package com.github.tvbox.osc.player;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.doikki.videoplayer.player.AbstractPlayer;
import xyz.doikki.videoplayer.player.AndroidMediaPlayer;
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
        long resolvedPosition = resolvePersistablePosition();
        if (resolvedPosition > 0L) {
            mCurrentPosition = resolvedPosition;
        }
        saveProgress();
    }

    public long getCachedProgressPosition() {
        return Math.max(0L, mCurrentPosition);
    }

    public long resolvePersistablePosition() {
        long cachedPosition = Math.max(0L, mCurrentPosition);
        if (mMediaPlayer == null) {
            return cachedPosition;
        }
        if (mMediaPlayer instanceof AndroidMediaPlayer
                && ((AndroidMediaPlayer) mMediaPlayer).isPositionQueryUnstable()) {
            return cachedPosition;
        }
        try {
            long livePosition = getCurrentPosition();
            if (livePosition > 0L) {
                return livePosition;
            }
        } catch (Throwable ignored) {
        }
        try {
            long livePosition = mMediaPlayer.getCurrentPosition();
            if (livePosition > 0L) {
                return livePosition;
            }
        } catch (Throwable ignored) {
        }
        return cachedPosition;
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
