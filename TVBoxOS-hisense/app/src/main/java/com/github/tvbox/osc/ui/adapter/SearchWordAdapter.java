package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchWordAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public SearchWordAdapter() {
        super(R.layout.item_search_word_split, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        String safeItem = item == null ? "" : item.trim();
        helper.setText(R.id.tvSearchWord, safeItem);
        resetView(helper.itemView);
        TextView textView = helper.getView(R.id.tvSearchWord);
        textView.setVisibility(View.VISIBLE);
        textView.setAlpha(1f);
        textView.setText(safeItem);
        textView.setSelected(true);
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
