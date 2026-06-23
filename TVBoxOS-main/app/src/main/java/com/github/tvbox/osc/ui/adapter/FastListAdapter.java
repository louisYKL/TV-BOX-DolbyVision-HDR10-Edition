package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.HashMap;

public class FastListAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public FastListAdapter() {
        super(R.layout.item_search_word_hot, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tvSearchWord, item);
        resetView(helper.itemView);
        TextView textView = helper.getView(R.id.tvSearchWord);
        textView.setVisibility(View.VISIBLE);
        textView.setText(item);
    }


    // 记录失去焦点的控件
    public void onLostFocus(View child) {
        if(lostFocusTimestamp==0) {
            lostFocusTimestamp = System.currentTimeMillis();
        }
    }

    // 记录获得焦点的控件
    public int onSetFocus(View child){
        // 可以调整间隔还判断到底是不是整个view失去了焦点
        if(System.currentTimeMillis() - lostFocusTimestamp > 200) {
            setp = 0;
        }
        ViewGroup parentGroup = findRecyclerChildParent(child);
        if (parentGroup == null) {
            return 1;
        }
        int index = parentGroup.indexOfChild(child);
        if(focusView != null){
            ViewGroup focusParent = findRecyclerChildParent(focusView);
            if (focusParent == null || focusParent != parentGroup) {
                focusView = child;
                setp = 1;
                return 1;
            }
            int index2 = focusParent.indexOfChild(focusView);
            if(Math.abs(index-index2) > setp) { // 跳了控件，将焦点恢复到之前的控件上去
                setp = 0;
                int offset = (index > index2? -1 :1);
                View target = parentGroup.getChildAt(Math.max(0, Math.min(parentGroup.getChildCount() - 1, index + offset)));
                if (target != null) {
                    target.requestFocus();
                }
                return  -1;
            }
        }
        lostFocusTimestamp =0;
        focusView = child;
        setp =1;
        return 1;
    }

    public void  reset(){
        lostFocusTimestamp =0;
        setp =0;
        focusView = null;
    }

    public long lostFocusTimestamp=0;   // 控件失去焦点的时间
    public int setp =0; //步长
    View focusView;     // 当前获得焦点的控件

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

    private ViewGroup findRecyclerChildParent(View child) {
        if (child == null || !(child.getParent() instanceof ViewGroup)) {
            return null;
        }
        return (ViewGroup) child.getParent();
    }
}
