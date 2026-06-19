package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;

public class PinyinAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public PinyinAdapter() {
        super(R.layout.item_search_word_hot, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tvSearchWord, item);
        resetView(helper.itemView);
        TextView textView = helper.getView(R.id.tvSearchWord);
        textView.setVisibility(View.VISIBLE);
        textView.setAlpha(1f);
        textView.setText(item);
        textView.setTextColor(mContext.getResources().getColorStateList(R.color.search_word_text));
    }

    private void resetView(View view) {
        if (view == null) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }
}
