package com.github.tvbox.osc.ui.tv.widget;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

/**
 * @author acer
 * @date 2018/8/22 11:46
 */
public class DefaultTransformer implements ViewPager.PageTransformer{
    @Override
    public void transformPage(View page, float position) {
        if (page == null) {
            return;
        }
        if (Float.isNaN(position) || Float.isInfinite(position)) {
            page.setVisibility(View.VISIBLE);
            page.setAlpha(1f);
            page.setScaleX(1f);
            page.setScaleY(1f);
            page.setTranslationX(0f);
            page.setTranslationY(0f);
            return;
        }
        if (position <= -1f || position >= 1f) {
            page.setAlpha(0f);
            page.setScaleX(0.95f);
            page.setScaleY(0.95f);
            page.setTranslationX(position < 0 ? -page.getWidth() * 0.06f : page.getWidth() * 0.06f);
            page.setTranslationY(18f);
            page.setVisibility(View.INVISIBLE);
            return;
        }
        float abs = Math.abs(position);
        float alpha = 1f - (abs * 0.18f);
        float scale = 1f - Math.min(0.035f, abs * 0.035f);
        float translationX = -position * page.getWidth() * 0.035f;
        float translationY = abs * 10f;
        page.setVisibility(View.VISIBLE);
        page.setAlpha(alpha);
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setTranslationX(translationX);
        page.setTranslationY(translationY);
    }
}
