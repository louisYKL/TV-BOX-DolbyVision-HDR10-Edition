package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.github.tvbox.osc.ui.adapter.FastListAdapter;
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter;
import com.github.tvbox.osc.ui.adapter.SearchWordAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.lzy.okgo.OkGo;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class FastSearchActivity extends BaseActivity {
    private static final String WORD_PANEL_HISTORY = "最近搜索";
    private static final String WORD_PANEL_GUESS = "猜你想搜";
    private static final String[] DEFAULT_FAST_SEARCH_WORDS = {
            "低智商犯罪",
            "主角",
            "藏海传",
            "长安的荔枝",
            "庆余年",
            "凡人修仙传",
            "家业",
            "书卷一梦",
            "美人余",
            "苏超"
    };
    private View llLayout;
    private TextView mSearchTitle;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewFilter;
    private TvRecyclerView mGridViewWord;
    private TvRecyclerView mGridViewWordFenci;
    SourceViewModel sourceViewModel;

    private SearchWordAdapter searchWordAdapter;
    private FastSearchAdapter searchAdapter;
    private FastSearchAdapter searchAdapterFilter;
    private FastListAdapter spListAdapter;
    private String searchTitle = "";
    private HashMap<String, String> spNames;
    private boolean isFilterMode = false;
    private String searchFilterKey = "";    // 过滤的key
    private HashMap<String, ArrayList<Movie.Video>> resultVods; // 搜索结果
    private final List<String> quickSearchWord = new ArrayList<>();
    private HashMap<String, String> mCheckSources = null;

    private final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View itemView, boolean hasFocus) {
            try {
                if (!hasFocus) {
                    spListAdapter.onLostFocus(itemView);
                } else {
                    int ret = spListAdapter.onSetFocus(itemView);
                    if (ret < 0) return;
                    TextView v = findSearchWordText(itemView);
                    if (v == null) return;
                    String sb = v.getText().toString();
                    filterResult(sb);
                }
            } catch (Exception e) {
                Toast.makeText(FastSearchActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }

        }
    };

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_fast_search;
    }

    @Override
    protected void init() {
        spNames = new HashMap<String, String>();
        resultVods = new HashMap<String, ArrayList<Movie.Video>>();
        initView();
        initViewModel();
        initData();
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
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        mSearchTitle = findViewById(R.id.mSearchTitle);
        mGridView = findViewById(R.id.mGridView);
        mGridViewWord = findViewById(R.id.mGridViewWord);
        mGridViewFilter = findViewById(R.id.mGridViewFilter);

        mGridViewWord.setHasFixedSize(false);
        mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
        spListAdapter = new FastListAdapter();
        mGridViewWord.setAdapter(spListAdapter);

        mGridViewWord.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View child) {
                child.setFocusable(true);
                child.setOnFocusChangeListener(focusChangeListener);
                TextView t = findSearchWordText(child);
                if (t != null && "全部".contentEquals(t.getText())) {
                    t.requestFocus();
                    child.requestFocus();
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                view.setOnFocusChangeListener(null);
            }
        });

        spListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                String spName = spListAdapter.getItem(position);
                filterResult(spName);
            }
        });

        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 5));
        int contentSpacing = getResources().getDimensionPixelSize(R.dimen.vs_18);
        mGridView.setSpacingWithMargins(contentSpacing, contentSpacing);

        searchAdapter = new FastSearchAdapter();
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
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });


        mGridViewFilter.setLayoutManager(new V7GridLayoutManager(this.mContext, 5));
        mGridViewFilter.setSpacingWithMargins(contentSpacing, contentSpacing);
        searchAdapterFilter = new FastSearchAdapter();
        mGridViewFilter.setAdapter(searchAdapterFilter);
        searchAdapterFilter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapterFilter.getData().get(position);
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
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });

        setLoadSir(llLayout);

        // 分词
        searchWordAdapter = new SearchWordAdapter();
        mGridViewWordFenci = findViewById(R.id.mGridViewWordFenci);
        mGridViewWordFenci.setAdapter(searchWordAdapter);
        mGridViewWordFenci.setHasFixedSize(false);
        mGridViewWordFenci.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        mGridViewWordFenci.setSpacingWithMargins(
                getResources().getDimensionPixelSize(R.dimen.vs_14),
                getResources().getDimensionPixelSize(R.dimen.vs_14)
        );
        searchWordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                String str = searchWordAdapter.getData().get(position);
                search(str);
            }
        });
        searchWordAdapter.setNewData(new ArrayList<>());
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
    }

    private void filterResult(String spName) {
        if (TextUtils.equals("全部", spName)) {
            mGridView.setVisibility(View.VISIBLE);
            mGridViewFilter.setVisibility(View.GONE);
            return;
        }
        mGridView.setVisibility(View.GONE);
        mGridViewFilter.setVisibility(View.VISIBLE);
        String key = spNames.get(spName);
        if (TextUtils.isEmpty(key)) return;

        if (TextUtils.equals(searchFilterKey, key)) return;
        searchFilterKey = key;

        List<Movie.Video> list = resultVods.get(key);
        searchAdapterFilter.setNewData(list);
    }

    private void fenci() {
        quickSearchWord.clear();
        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle));
        addFastSearchWords(quickSearchWord, getSearchHistoryWords(), "");
        addFastSearchWords(quickSearchWord, getDefaultGuessWords(), "");
        List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
        // 分词
//        OkGo.<String>get("http://api.pullword.com/get.php?source=" + URLEncoder.encode(searchTitle) + "&param1=0&param2=0&json=1")
//                .tag("fenci")
//                .execute(new AbsCallback<String>() {
//                    @Override
//                    public String convertResponse(okhttp3.Response response) throws Throwable {
//                        if (response.body() != null) {
//                            return response.body().string();
//                        } else {
//                            throw new IllegalStateException("网络请求错误");
//                        }
//                    }
//
//                    @Override
//                    public void onSuccess(Response<String> response) {
//                        String json = response.body();
//                        quickSearchWord.clear();
//                        try {
//                            for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
//                                quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
//                            }
//                        } catch (Throwable th) {
//                            th.printStackTrace();
//                        }
//                        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle));
//                        List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
//                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
//                    }
//
//                    @Override
//                    public void onError(Response<String> response) {
//                        super.onError(response);
//                    }
//                });
    }

    private void initData() {
        initCheckedSourcesForSearch();
        showDefaultWordPanel();
        ensureTopWordPanelVisible();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            search(title);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            search(title);
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
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD) {
            if (event.obj != null) {
                List<String> data = (List<String>) event.obj;
                applyTopWordPanelData(data, WORD_PANEL_GUESS);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private void search(String title) {
        cancel();
        showLoading();
        this.searchTitle = title;
        fenci();
        mGridView.setVisibility(View.INVISIBLE);
        mGridViewFilter.setVisibility(View.GONE);
        searchAdapter.setNewData(new ArrayList<>());
        searchAdapterFilter.setNewData(new ArrayList<>());

        spListAdapter.reset();
        resultVods.clear();
        searchFilterKey = "";
        isFilterMode = false;
        spNames.clear();

        //写入历史记录
        HistoryHelper.setSearchHistory(title);
        showGuessWordsFor(title);
        ensureTopWordPanelVisible();
        ensureSourcePanelVisible();

        searchResult();
    }

    private ExecutorService searchExecutorService = null;
    private final AtomicInteger allRunCount = new AtomicInteger(0);

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
            searchAdapterFilter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);


        ArrayList<String> siteKey = new ArrayList<>();
        ArrayList<String> sourceTabs = new ArrayList<>();
        sourceTabs.add("全部");
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            this.spNames.put(bean.getName(), bean.getKey());
            if (!sourceTabs.contains(bean.getName())) {
                sourceTabs.add(bean.getName());
            }
            allRunCount.incrementAndGet();
        }
        spListAdapter.setNewData(sourceTabs);
        spListAdapter.notifyDataSetChanged();
        ensureSourcePanelVisible();

        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        sourceViewModel.getSearch(key, searchTitle);
                    } catch (Exception e) {

                    }
                }
            });
        }
    }

    // 向过滤栏添加有结果的spname
    private String addWordAdapterIfNeed(String key) {
        try {
            String name = "";
            for (String n : spNames.keySet()) {
                if (TextUtils.equals(spNames.get(n), key)) {
                    name = n;
                    break;
                }
            }
            if (TextUtils.isEmpty(name)) return key;

            List<String> names = spListAdapter.getData();
            for (int i = 0; i < names.size(); ++i) {
                if (TextUtils.equals(name, names.get(i))) {
                    return key;
                }
            }

            spListAdapter.addData(name);
            return key;
        } catch (Exception e) {
            return key;
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
        return matchNum == arr.length;
    }

    private void searchData(AbsXml absXml) {
        String lastSourceKey = "";

        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            List<Movie.Video> fallbackData = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                fallbackData.add(video);
                if (!matchSearchResult(video.name, searchTitle)) {
                    continue;
                }
                data.add(video);
            }
            if (data.isEmpty()) {
                data.addAll(fallbackData);
            }
            for (Movie.Video video : data) {
                if (!resultVods.containsKey(video.sourceKey)) {
                    resultVods.put(video.sourceKey, new ArrayList<Movie.Video>());
                }
                resultVods.get(video.sourceKey).add(video);
                if (!TextUtils.equals(video.sourceKey, lastSourceKey)) {
                    lastSourceKey = this.addWordAdapterIfNeed(video.sourceKey);
                }
            }

            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                if (!isFilterMode)
                    mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() == 0) {
                showEmpty();
                ensureTopWordPanelVisible();
                ensureSourcePanelVisible();
            }
            cancel();
        }
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("search");
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

    private void showDefaultWordPanel() {
        ArrayList<String> history = getSearchHistoryWords();
        if (!history.isEmpty()) {
            ArrayList<String> displayWords = new ArrayList<>(history);
            addFastSearchWords(displayWords, getDefaultGuessWords(), "");
            applyTopWordPanelData(displayWords, WORD_PANEL_HISTORY);
            return;
        }
        applyTopWordPanelData(getDefaultGuessWords(), WORD_PANEL_GUESS);
    }

    private void showGuessWordsFor(String query) {
        ArrayList<String> matched = new ArrayList<>();
        addFastSearchWords(matched, getSearchHistoryWords(), "");
        addFastSearchWords(matched, getDefaultGuessWords(), "");
        if (!TextUtils.isEmpty(query)) {
            addFastSearchWords(matched, SearchHelper.splitWords(query), "");
        }
        if (matched.size() < 8) {
            addFastSearchWords(matched, getDefaultGuessWords(), "");
        }
        if (matched.isEmpty()) {
            matched = getDefaultGuessWords();
        }
        applyTopWordPanelData(matched, WORD_PANEL_GUESS);
    }

    private ArrayList<String> getSearchHistoryWords() {
        return HistoryHelper.getSearchHistory();
    }

    private ArrayList<String> getDefaultGuessWords() {
        ArrayList<String> result = new ArrayList<>();
        for (String word : DEFAULT_FAST_SEARCH_WORDS) {
            if (!result.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    private void addFastSearchWords(List<String> out, List<String> source, String query) {
        if (source == null || source.isEmpty()) {
            return;
        }
        String normalizedQuery = TextUtils.isEmpty(query) ? "" : query.trim().toLowerCase();
        for (String item : source) {
            String cleaned = cleanFastSearchWord(item);
            if (TextUtils.isEmpty(cleaned) || out.contains(cleaned)) {
                continue;
            }
            if (TextUtils.isEmpty(normalizedQuery) || cleaned.toLowerCase().contains(normalizedQuery)) {
                out.add(cleaned);
            }
            if (out.size() >= 20) {
                return;
            }
        }
    }

    private String cleanFastSearchWord(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value.replace("<hl>", "")
                .replace("</hl>", "")
                .replace("《", "")
                .replace("》", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private void applyTopWordPanelData(List<String> words, String title) {
        ArrayList<String> normalized = new ArrayList<>();
        addFastSearchWords(normalized, words, "");
        if (normalized.isEmpty()) {
            normalized = getDefaultGuessWords();
        }
        showSuccess();
        mSearchTitle.setText(TextUtils.isEmpty(title) ? WORD_PANEL_GUESS : title);
        searchWordAdapter.setNewData(normalized);
        searchWordAdapter.notifyDataSetChanged();
        com.github.tvbox.osc.util.LOG.i("echo-fast-search-word-panel title=" + mSearchTitle.getText()
                + " count=" + normalized.size()
                + " first=" + (normalized.isEmpty() ? "" : normalized.get(0)));
        if (mGridViewWordFenci != null) {
            mGridViewWordFenci.setVisibility(View.VISIBLE);
            mGridViewWordFenci.setAlpha(1f);
            mGridViewWordFenci.requestLayout();
            mGridViewWordFenci.invalidate();
        }
        ensureTopWordPanelVisible();
    }

    private void ensureTopWordPanelVisible() {
        if (searchWordAdapter.getData() == null || searchWordAdapter.getData().isEmpty()) {
            searchWordAdapter.setNewData(getDefaultGuessWords());
            searchWordAdapter.notifyDataSetChanged();
        }
        if (mGridViewWordFenci != null) {
            mGridViewWordFenci.setVisibility(View.VISIBLE);
            mGridViewWordFenci.setAlpha(1f);
            mGridViewWordFenci.bringToFront();
            mGridViewWordFenci.post(new Runnable() {
                @Override
                public void run() {
                    mGridViewWordFenci.requestLayout();
                    mGridViewWordFenci.invalidate();
                    if (mGridViewWordFenci.getAdapter() != null) {
                        mGridViewWordFenci.getAdapter().notifyDataSetChanged();
                    }
                    mGridViewWordFenci.scrollToPosition(0);
                }
            });
        }
    }

    private void ensureSourcePanelVisible() {
        if (spListAdapter == null) {
            return;
        }
        if (spListAdapter.getData() == null || spListAdapter.getData().isEmpty()) {
            ArrayList<String> fallback = new ArrayList<>();
            fallback.add("全部");
            spListAdapter.setNewData(fallback);
            spListAdapter.notifyDataSetChanged();
        }
        if (mGridViewWord != null) {
            mGridViewWord.setVisibility(View.VISIBLE);
            mGridViewWord.setAlpha(1f);
            mGridViewWord.bringToFront();
            mGridViewWord.requestLayout();
            mGridViewWord.invalidate();
        }
    }

    private TextView findSearchWordText(View itemView) {
        if (itemView == null) {
            return null;
        }
        if (itemView instanceof TextView) {
            return (TextView) itemView;
        }
        return itemView.findViewById(R.id.tvSearchWord);
    }
}
