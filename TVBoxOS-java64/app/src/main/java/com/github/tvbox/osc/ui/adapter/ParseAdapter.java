package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.ParseBean;

import java.util.ArrayList;

public class ParseAdapter extends BaseQuickAdapter<ParseBean, BaseViewHolder> {
    public ParseAdapter() {
        super(R.layout.item_play_parse, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, ParseBean item) {
        TextView tvParse = helper.getView(R.id.tvParse);
        tvParse.setVisibility(View.VISIBLE);
        if (item.isDefault()) {
            tvParse.setTextColor(mContext.getResources().getColor(R.color.apple_tv_selected_text_dark));
        } else {
            tvParse.setTextColor(mContext.getResources().getColor(R.color.apple_tv_text_primary));
        }
        tvParse.setText(item.getName());
        if (helper.getLayoutPosition() == 0) {
            helper.itemView.setNextFocusLeftId(R.id.screen_display);
        }
    }
}
