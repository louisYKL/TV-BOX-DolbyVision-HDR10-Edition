package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.github.tvbox.osc.R;

public class RoundedPosterLayout extends FrameLayout {
    private final RectF clipRect = new RectF();
    private final Path clipPath = new Path();
    private final float posterRadius;

    public RoundedPosterLayout(Context context) {
        this(context, null);
    }

    public RoundedPosterLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedPosterLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources resources = context.getResources();
        posterRadius = resources.getDimension(R.dimen.vs_12);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setBackgroundColor(Color.TRANSPARENT);
        setClipChildren(true);
        setClipToPadding(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), posterRadius);
                }
            });
            setClipToOutline(true);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        clipRect.set(0f, 0f, getWidth(), getHeight());
        int saveCount = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(clipRect, posterRadius, posterRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
