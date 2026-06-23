package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.PinyinAdapter;
import com.github.tvbox.osc.ui.adapter.SearchAdapter;
import com.github.tvbox.osc.ui.dialog.RemoteDialog;
import com.github.tvbox.osc.ui.dialog.SearchCheckboxDialog;
import com.github.tvbox.osc.ui.tv.widget.SearchKeyboard;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchActivity extends BaseActivity {
    private static final String HOT_SEARCH_URL = "https://movie.douban.com/j/search_subjects?type=tv&tag=%E7%83%AD%E9%97%A8&sort=recommend&page_limit=20&page_start=0";
    private static final String SEARCH_REC_TAG = "search_rec";
    private static final String WORD_PANEL_HOT = "热词 | 历史";
    private static final String WORD_PANEL_HISTORY = "历史 搜索";
    private static final String WORD_PANEL_HOT_ACTIVE = "热词 搜索";
    private static final String WORD_PANEL_GUESS = "猜你想搜";
    private static final String[] DEFAULT_HOT_WORDS = {
            "\u5bb6\u4e1a",
            "\u4e3b\u89d2",
            "\u4f4e\u667a\u5546\u72af\u7f6a",
            "\u82cf\u8d85",
            "\u4e66\u5377\u4e00\u68a6",
            "\u7f8e\u4eba\u4f59",
            "\u85cf\u6d77\u4f20",
            "\u957f\u5b89\u7684\u8354\u679d",
            "\u5e86\u4f59\u5e74",
            "\u51e1\u4eba\u4fee\u4ed9\u4f20"
    };
    private LinearLayout llLayout;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewWord;
    SourceViewModel sourceViewModel;
    private RemoteDialog remoteDialog;
    private EditText etSearch;
    private TextView tvSearch;
    private TextView tvClear;
    private SearchKeyboard keyboard;
    private SearchAdapter searchAdapter;
    private PinyinAdapter wordAdapter;
    private String searchTitle = "";
    private TextView tvSearchCheckboxBtn;

    private static HashMap<String, String> mCheckSources = null;
    private SearchCheckboxDialog mSearchCheckboxDialog = null;

    private TextView wordsSwitch;
    private final ArrayList<String> guessWords = new ArrayList<>();
    private String lastSuggestionQuery = "";

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search;
    }


    private static Boolean hasKeyBoard;
    private static Boolean isSearchBack;
    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
        hasKeyBoard = true;
        isSearchBack = false;
    }

    private List<Runnable> pauseRunnable = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (pauseRunnable != null && pauseRunnable.size() > 0) {
            searchExecutorService = Executors.newFixedThreadPool(5);
            allRunCount.set(pauseRunnable.size());
            for (Runnable runnable : pauseRunnable) {
                searchExecutorService.execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }
        if (hasKeyBoard) {
            tvSearch.requestFocus();
            tvSearch.requestFocusFromTouch();
        }else {
            if(!isSearchBack){
                etSearch.requestFocus();
                etSearch.requestFocusFromTouch();
            }
        }
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        etSearch = findViewById(R.id.etSearch);
        tvSearch = findViewById(R.id.tvSearch);
        tvSearchCheckboxBtn = findViewById(R.id.tvSearchCheckboxBtn);
        tvClear = findViewById(R.id.tvClear);
        mGridView = findViewById(R.id.mGridView);
        keyboard = findViewById(R.id.keyBoardRoot);
        mGridViewWord = findViewById(R.id.mGridViewWord);
        mGridViewWord.setHasFixedSize(false);
        mGridViewWord.setItemAnimator(null);
        mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
        int wordSpacing = getResources().getDimensionPixelSize(R.dimen.vs_12);
        mGridViewWord.setSpacingWithMargins(wordSpacing, wordSpacing);
        wordAdapter = new PinyinAdapter();
        mGridViewWord.setAdapter(wordAdapter);
        mGridViewWord.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@androidx.annotation.NonNull View child) {
                resetWordItemView(child);
            }

            @Override
            public void onChildViewDetachedFromWindow(@androidx.annotation.NonNull View view) {
            }
        });
        wordsSwitch = findViewById(R.id.wordSwitch);
        wordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                    Bundle bundle = new Bundle();
                    bundle.putString("title", wordAdapter.getItem(position));
                    jumpActivity(FastSearchActivity.class, bundle);
                }else {
                    search(wordAdapter.getItem(position));
                }
            }
        });
        mGridView.setHasFixedSize(true);
        // lite
        if (Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0)
            mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
            // with preview
        else
            mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    try {
                        if (searchExecutorService != null) {
                            pauseRunnable = searchExecutorService.shutdownNow();
                            searchExecutorService = null;
                            JsLoader.stopAll();
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    hasKeyBoard = false;
                    isSearchBack = true;
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });
        wordsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                String wd = wordsSwitch.getText().toString().trim();
                if (WORD_PANEL_GUESS.equals(wd)) {
                    showDefaultWordPanel();
                    return;
                }
                if (wd.contains("历史")) {
                    wordsSwitch.setText(WORD_PANEL_HOT_ACTIVE);
                    if (hots != null && !hots.isEmpty()) {
                        wordAdapter.setNewData(new ArrayList<>(hots));
                        scrollWordsToTop();
                    } else {
                        showHotWordsPanel();
                    }
                    return;
                }
                if (wd.contains("热词")) {
                    if (!showHistoryWordsPanel()) {
                        Toast.makeText(mContext, "暂无历史搜索", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                hasKeyBoard = true;
                String wd = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(wd)) {
                    if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                        Bundle bundle = new Bundle();
                        bundle.putString("title", wd);
                        jumpActivity(FastSearchActivity.class, bundle);
                    }else {
                        search(wd);
                    }
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                initData();
                etSearch.setText("");
            }
        });

        //软键盘

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String wd = etSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(wd)) {
                        if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                            Bundle bundle = new Bundle();
                            bundle.putString("title", wd);
                            jumpActivity(FastSearchActivity.class, bundle);
                        } else {
                            hiddenImm();
                            search(wd);
                        }
                    } else {
                        Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });

        // 监听遥控器
        etSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String wd = etSearch.getText().toString().trim();
                    if (!TextUtils.isEmpty(wd)) {
                        if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)) {
                            Bundle bundle = new Bundle();
                            bundle.putString("title", wd);
                            jumpActivity(FastSearchActivity.class, bundle);
                        } else {
                            hiddenImm();
                            search(wd);
                        }
                    } else {
                        Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s == null ? "" : s.toString().trim();
                if (TextUtils.isEmpty(text)) {
                    restoreSuggestionPanel();
                    return;
                }
                loadRec(text);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        keyboard.setOnSearchKeyListener(new SearchKeyboard.OnSearchKeyListener() {
            @Override
            public void onSearchKey(int pos, String key) {
                if (pos > 1) {
                    String text = etSearch.getText().toString().trim();
                    text += key;
                    etSearch.setText(text);
                } else if (pos == 1) {
                    String text = etSearch.getText().toString().trim();
                    if (text.length() > 0) {
                        text = text.substring(0, text.length() - 1);
                        etSearch.setText(text);
                    }
                } else if (pos == 0) {
                    remoteDialog = new RemoteDialog(mContext);
                    remoteDialog.show();
                }
            }
        });
        setLoadSir(llLayout);
        tvSearchCheckboxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<SourceBean> searchAbleSource = ApiConfig.get().getSearchSourceBeanList();
                if (mSearchCheckboxDialog == null) {
                    mSearchCheckboxDialog = new SearchCheckboxDialog(SearchActivity.this, searchAbleSource, mCheckSources);
                }else {
                    if(searchAbleSource.size()!=mSearchCheckboxDialog.mSourceList.size()){
                        mSearchCheckboxDialog.setMSourceList(searchAbleSource);
                    }
                }
                mSearchCheckboxDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
                mSearchCheckboxDialog.show();
            }
        });
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
    }

    /**
     * 拼音联想
     */
    private void loadRec(String key) {
        final String query = key == null ? "" : key.trim();
        if (TextUtils.isEmpty(query)) {
            restoreSuggestionPanel();
            return;
        }
        if (TextUtils.equals(query, lastSuggestionQuery)
                && WORD_PANEL_GUESS.equals(wordsSwitch.getText().toString().trim())
                && !guessWords.isEmpty()) {
            return;
        }
        lastSuggestionQuery = query;
        showLocalSuggestionFallback(query);
        if (query.length() < 2) {
            return;
        }
        OkGo.getInstance().cancelTag(SEARCH_REC_TAG);
        OkGo.<String>get("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
                .tag(SEARCH_REC_TAG)
                .params("format", "json")
                .params("page_num", 0)
                .params("page_size", 20)
                .params("key", query)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            if (!TextUtils.equals(query, lastSuggestionQuery)) {
                                return;
                            }
                            Gson gson = new Gson();
                            JsonElement json = gson.fromJson(response.body(), JsonElement.class);
                            ArrayList<String> recWords = new ArrayList<>();
                            String title = WORD_PANEL_GUESS;
                            JsonObject root = json == null ? null : json.getAsJsonObject();
                            JsonObject data = root != null && root.has("data") ? root.getAsJsonObject("data") : null;
                            JsonObject searchData = data != null && data.has("search_data") ? data.getAsJsonObject("search_data") : null;
                            JsonArray groups = searchData != null && searchData.has("vecGroupData") ? searchData.getAsJsonArray("vecGroupData") : null;
                            if (groups != null) {
                                for (JsonElement groupElement : groups) {
                                    if (groupElement == null || !groupElement.isJsonObject()) {
                                        continue;
                                    }
                                    JsonObject groupObject = groupElement.getAsJsonObject();
                                    if (groupObject.has("group_title")) {
                                        String groupTitle = groupObject.get("group_title").getAsString().trim();
                                        if (!TextUtils.isEmpty(groupTitle)) {
                                            title = groupTitle;
                                        }
                                    }
                                    JsonArray groupDataArr = groupObject.has("group_data") ? groupObject.getAsJsonArray("group_data") : null;
                                    if (groupDataArr == null) {
                                        continue;
                                    }
                                    for (JsonElement groupDataElement : groupDataArr) {
                                        if (groupDataElement == null || !groupDataElement.isJsonObject()) {
                                            continue;
                                        }
                                        JsonObject groupData = groupDataElement.getAsJsonObject();
                                        appendSuggestionWord(recWords, readNestedString(groupData,
                                                "dtReportInfo", "reportData", "keyword_txt"));
                                        appendSuggestionWord(recWords, readNestedString(groupData,
                                                "action", "actionArgs", "search_keyword", "strVal"));
                                        appendSuggestionWord(recWords, readNestedString(groupData,
                                                "cell_info", "title"));
                                    }
                                    if (!recWords.isEmpty()) {
                                        break;
                                    }
                                }
                            }
                            if (recWords.isEmpty()) {
                                showLocalSuggestionFallback(query);
                                return;
                            }
                            showGuessWordsPanel(title, recWords);
                        } catch (Throwable th) {
                            th.printStackTrace();
                            if (TextUtils.equals(query, lastSuggestionQuery)) {
                                showLocalSuggestionFallback(query);
                            }
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        if (TextUtils.equals(query, lastSuggestionQuery)) {
                            showLocalSuggestionFallback(query);
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private static ArrayList<String> hots;

    private void useDefaultHotWords() {
        hots = new ArrayList<>();
        for (String word : DEFAULT_HOT_WORDS) {
            hots.add(word);
        }
        applyWordPanelData(new ArrayList<>(hots), WORD_PANEL_HOT);
        if (wordAdapter.getData() == null || wordAdapter.getData().isEmpty()) {
            wordAdapter.addData(new ArrayList<>(hots));
        }
        scrollWordsToTop();
    }

    private String cleanHotWord(String title) {
        if (TextUtils.isEmpty(title)) return "";
        return title.trim().replaceAll("<|>|《|》|-", "").split(" ")[0];
    }

    private String cleanSuggestionWord(String text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        String cleaned = text
                .replace("<hl>", "")
                .replace("</hl>", "")
                .replace("《", "")
                .replace("》", "")
                .trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private void appendSuggestionWord(ArrayList<String> words, String rawText) {
        String word = cleanSuggestionWord(rawText);
        if (!TextUtils.isEmpty(word) && !words.contains(word)) {
            words.add(word);
        }
    }

    private String readNestedString(JsonObject root, String... keys) {
        if (root == null || keys == null || keys.length == 0) {
            return "";
        }
        JsonElement current = root;
        for (String key : keys) {
            if (current == null || !current.isJsonObject()) {
                return "";
            }
            JsonObject object = current.getAsJsonObject();
            if (!object.has(key)) {
                return "";
            }
            current = object.get(key);
        }
        if (current == null || current.isJsonNull()) {
            return "";
        }
        try {
            return current.getAsString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void addHotWord(ArrayList<String> data, String title) {
        String word = cleanHotWord(title);
        if (!TextUtils.isEmpty(word) && !data.contains(word)) {
            data.add(word);
        }
    }

    private void initData() {
        initCheckedSourcesForSearch();
        if (hots == null || hots.isEmpty()) {
            hots = new ArrayList<>();
            for (String word : DEFAULT_HOT_WORDS) {
                if (!hots.contains(word)) {
                    hots.add(word);
                }
            }
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            }else {
                search(title);
            }
        }
        showDefaultWordPanel();
        // 加载热词
        OkGo.<String>get(HOT_SEARCH_URL)
//        OkGo.<String>get("https://api.web.360kan.com/v1/rank")
//                .params("cat", "1")
                .headers("User-Agent", "Mozilla/5.0")
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            hots = new ArrayList<String>();
                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("subjects").getAsJsonArray();
//                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                if (obj.has("title")) {
                                    addHotWord(hots, obj.get("title").getAsString());
                                }
                            }
                            if (hots.isEmpty()) {
                                useDefaultHotWords();
                                return;
                            }
                            if (getSearchHistoryWords().isEmpty()
                                    && !WORD_PANEL_GUESS.equals(wordsSwitch.getText().toString().trim())) {
                                showHotWordsPanel();
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                            useDefaultHotWords();
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        useDefaultHotWords();
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true)){
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            }else{
                search(title);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    public static void setCheckedSourcesForSearch(HashMap<String,String> checkedSources) {
        mCheckSources = checkedSources;
    }

    private void search(String title) {
        cancel();
        if (remoteDialog != null) {
            remoteDialog.dismiss();
            remoteDialog = null;
        }
        showLoading();
        etSearch.setText(title);

        //写入历史记录
        HistoryHelper.setSearchHistory(title);


        this.searchTitle = title;
        mGridView.setVisibility(View.INVISIBLE);
        searchAdapter.setNewData(new ArrayList<>());
        showLocalSuggestionFallback(title);
        ensureWordPanelVisible();
        searchResult();
    }

    private ExecutorService searchExecutorService = null;
    private AtomicInteger allRunCount = new AtomicInteger(0);

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            searchAdapter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            allRunCount.incrementAndGet();
        }
        if (siteKey.size() <= 0) {
            Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show();
            showEmpty();
            ensureWordPanelVisible();
            return;
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getSearch(key, searchTitle);
                }
            });
        }
    }

    private boolean matchSearchResult(String name, String searchTitle) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false;
        searchTitle = searchTitle.trim();
        String[] arr = searchTitle.split("\\s+");
        int matchNum = 0;
        for(String one : arr) {
            if (name.contains(one)) matchNum++;
        }
        return matchNum == arr.length ? true : false;
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (matchSearchResult(video.name, searchTitle)) data.add(video);
            }
            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
                ensureWordPanelVisible();
            }
            cancel();
        }
    }


    private void cancel() {
        OkGo.getInstance().cancelTag("search");
        OkGo.getInstance().cancelTag(SEARCH_REC_TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        EventBus.getDefault().unregister(this);
    }

    private void hiddenImm()
    {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    private void showHotWordsPanel() {
        guessWords.clear();
        if (hots == null || hots.isEmpty()) {
            useDefaultHotWords();
            return;
        }
        applyWordPanelData(new ArrayList<>(hots), WORD_PANEL_HOT);
    }

    private void showGuessWordsPanel(String title, List<String> words) {
        guessWords.clear();
        guessWords.addAll(words);
        if (guessWords.isEmpty()) {
            ArrayList<String> fallback = buildLocalSuggestionWords(lastSuggestionQuery);
            if (fallback.isEmpty()) {
                for (String word : DEFAULT_HOT_WORDS) {
                    fallback.add(word);
                }
            }
            applyWordPanelData(new ArrayList<>(fallback), WORD_PANEL_GUESS);
            guessWords.addAll(fallback);
        } else {
            applyWordPanelData(new ArrayList<>(guessWords), WORD_PANEL_GUESS);
        }
    }

    private boolean showDefaultWordPanel() {
        if (showHistoryWordsPanel()) {
            return true;
        }
        showHotWordsPanel();
        return true;
    }

    private boolean showHistoryWordsPanel() {
        ArrayList<String> hisWord = getVisibleHistoryWords();
        if (hisWord.isEmpty()) {
            return false;
        }
        guessWords.clear();
        applyWordPanelData(new ArrayList<>(hisWord), WORD_PANEL_HISTORY);
        return wordAdapter.getData() != null && !wordAdapter.getData().isEmpty();
    }

    private ArrayList<String> getSearchHistoryWords() {
        return HistoryHelper.getSearchHistory();
    }

    private ArrayList<String> getVisibleHistoryWords() {
        ArrayList<String> visibleWords = new ArrayList<>();
        addMatchingWords(visibleWords, getSearchHistoryWords(), "");
        if (visibleWords.size() < 8) {
            if (hots != null && !hots.isEmpty()) {
                addMatchingWords(visibleWords, hots, "");
            } else {
                ArrayList<String> defaults = new ArrayList<>();
                for (String word : DEFAULT_HOT_WORDS) {
                    defaults.add(word);
                }
                addMatchingWords(visibleWords, defaults, "");
            }
        }
        return visibleWords;
    }

    private void showLocalSuggestionFallback(String query) {
        ArrayList<String> localWords = buildLocalSuggestionWords(query);
        if (localWords.isEmpty()) {
            localWords = buildLocalSuggestionWords("");
        }
        if (localWords.isEmpty()) {
            useDefaultHotWords();
            return;
        }
        showGuessWordsPanel(WORD_PANEL_GUESS, localWords);
    }

    private ArrayList<String> buildLocalSuggestionWords(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        ArrayList<String> localWords = new ArrayList<>();
        if (hots != null && !hots.isEmpty()) {
            addMatchingWords(localWords, hots, normalizedQuery);
        } else {
            ArrayList<String> defaults = new ArrayList<>();
            for (String word : DEFAULT_HOT_WORDS) {
                defaults.add(word);
            }
            addMatchingWords(localWords, defaults, normalizedQuery);
        }
        if (!TextUtils.isEmpty(normalizedQuery) && localWords.size() < 8) {
            if (hots != null && !hots.isEmpty()) {
                addMatchingWords(localWords, hots, "");
            } else {
                ArrayList<String> defaults = new ArrayList<>();
                for (String word : DEFAULT_HOT_WORDS) {
                    defaults.add(word);
                }
                addMatchingWords(localWords, defaults, "");
            }
        }
        return localWords;
    }

    private void addMatchingWords(ArrayList<String> out, List<String> source, String normalizedQuery) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (String raw : source) {
            String cleaned = cleanSuggestionWord(raw);
            if (TextUtils.isEmpty(cleaned) || out.contains(cleaned)) {
                continue;
            }
            if (TextUtils.isEmpty(normalizedQuery)
                    || cleaned.toLowerCase().contains(normalizedQuery)) {
                out.add(cleaned);
            }
            if (out.size() >= 20) {
                return;
            }
        }
    }

    private void restoreSuggestionPanel() {
        lastSuggestionQuery = "";
        showDefaultWordPanel();
        if (wordAdapter.getData() == null || wordAdapter.getData().isEmpty()) {
            showHotWordsPanel();
        }
    }

    private void scrollWordsToTop() {
        if (mGridViewWord != null) {
            mGridViewWord.smoothScrollToPosition(0);
        }
    }

    private void applyWordPanelData(List<String> words, String title) {
        ArrayList<String> visibleWords = new ArrayList<>();
        addMatchingWords(visibleWords, words, "");
        if (visibleWords.isEmpty()) {
            for (String word : DEFAULT_HOT_WORDS) {
                if (!visibleWords.contains(word)) {
                    visibleWords.add(word);
                }
            }
        }
        showSuccess();
        if (wordsSwitch != null) {
            wordsSwitch.setText(TextUtils.isEmpty(title) ? WORD_PANEL_GUESS : title);
            wordsSwitch.setVisibility(View.VISIBLE);
            wordsSwitch.setAlpha(1f);
        }
        wordAdapter.setNewData(visibleWords);
        wordAdapter.notifyDataSetChanged();
        LOG.i("echo-search-word-panel title=" + (wordsSwitch == null ? "" : wordsSwitch.getText())
                + " count=" + visibleWords.size()
                + " first=" + (visibleWords.isEmpty() ? "" : visibleWords.get(0)));
        if (mGridViewWord != null) {
            mGridViewWord.setVisibility(View.VISIBLE);
            mGridViewWord.setAlpha(1f);
            mGridViewWord.setFocusable(true);
            mGridViewWord.setFocusableInTouchMode(false);
            mGridViewWord.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < mGridViewWord.getChildCount(); i++) {
                        resetWordItemView(mGridViewWord.getChildAt(i));
                    }
                    mGridViewWord.requestLayout();
                    mGridViewWord.invalidate();
                    if (mGridViewWord.getAdapter() != null) {
                        mGridViewWord.getAdapter().notifyDataSetChanged();
                    }
                    mGridViewWord.scrollToPosition(0);
                }
            });
        }
    }

    private void ensureWordPanelVisible() {
        if (wordAdapter == null) {
            return;
        }
        if (wordAdapter.getData() == null || wordAdapter.getData().isEmpty()) {
            ArrayList<String> fallback = buildLocalSuggestionWords(lastSuggestionQuery);
            if (fallback.isEmpty()) {
                for (String word : DEFAULT_HOT_WORDS) {
                    fallback.add(word);
                }
            }
            wordAdapter.setNewData(fallback);
            wordAdapter.notifyDataSetChanged();
        }
        if (wordsSwitch != null) {
            wordsSwitch.setVisibility(View.VISIBLE);
            wordsSwitch.setAlpha(1f);
            if (TextUtils.isEmpty(wordsSwitch.getText())) {
                wordsSwitch.setText(WORD_PANEL_GUESS);
            }
        }
        if (mGridViewWord != null) {
            mGridViewWord.setVisibility(View.VISIBLE);
            mGridViewWord.setAlpha(1f);
            mGridViewWord.bringToFront();
            mGridViewWord.requestLayout();
            mGridViewWord.invalidate();
        }
    }

    private void resetWordItemView(View view) {
        if (view == null) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setFocusable(true);
        TextView textView = view.findViewById(R.id.tvSearchWord);
        if (textView != null) {
            textView.setVisibility(View.VISIBLE);
            textView.setAlpha(1f);
            textView.setScaleX(1f);
            textView.setScaleY(1f);
            textView.setTranslationX(0f);
            textView.setTranslationY(0f);
            textView.setTextColor(getResources().getColorStateList(R.color.search_word_text));
        }
    }
}
