package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.github.tvbox.osc.R;

public class LiquidGlassPosterFrameLayout extends LiquidGlassFrameLayout {
    public LiquidGlassPosterFrameLayout(Context context) {
        this(context, null);
    }

    public LiquidGlassPosterFrameLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidGlassPosterFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setGlassRadius(getResources().getDimension(R.dimen.vs_16));
    }
}
