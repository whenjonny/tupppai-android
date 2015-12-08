package com.psgod.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.psgod.Constants;
import com.psgod.LoadUtils;
import com.psgod.R;
import com.psgod.eventbus.RefreshEvent;
import com.psgod.model.Activities;
import com.psgod.model.ActivitiesAct;
import com.psgod.model.PhotoItem;
import com.psgod.network.request.PSGodRequestQueue;
import com.psgod.network.request.PhotoActRequest;
import com.psgod.ui.adapter.RecentPageActAdapter;
import com.psgod.ui.widget.FloatScrollView;
import com.psgod.ui.widget.dialog.CustomProgressingDialog;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class RecentActActivity extends PSGodBaseActivity {
    private static final String TAG = RecentActActivity.class
            .getSimpleName();

    private PullToRefreshListView mListView;
    private View mHeadView;
    private ImageView mHeadImg;
    private RelativeLayout mHeadTxtArea;
    private RecentPageActAdapter mAdapter;
    private List<PhotoItem> mPhotoItems;
    private List<ActivitiesAct> mActs;
    private View mFollowListFooter;
    private TextView mTitle;
    private ImageView mFinish;

    private int mPage = 1;
    private boolean canLoadMore = false;

    // 上次刷新时间
    private long mLastUpdatedTime;
    // 列表的类型
    private String mSpKey;
    private LoadUtils loadUtils;
    private View mEmptyView;

    private String id;

    private CustomProgressingDialog progressingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_recent_act);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");

        initView();
        initEvent();
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        mListView = (PullToRefreshListView) findViewById(R.id.fragment_recentpage_act_list);
        mTitle = (TextView) findViewById(R.id.activity_act_title_name);
        mHeadView = LayoutInflater.from(RecentActActivity.this).inflate(R.layout.header_recent_page_act, null);
        mHeadImg = (ImageView) mHeadView.findViewById(R.id.header_recentpage_act_img);
        mHeadTxtArea = (RelativeLayout) mHeadView.findViewById(R.id.header_recentpage_act_area);
        mHeadTxtArea.setVisibility(View.GONE);
        mEmptyView = findViewById(R.id.recent_fragment_act_empty_view);
        mPhotoItems = new ArrayList<PhotoItem>();
        mActs = new ArrayList<ActivitiesAct>();
        mAdapter = new RecentPageActAdapter(RecentActActivity.this, mPhotoItems);
        mListView.setAdapter(mAdapter);
        mFollowListFooter = LayoutInflater.from(RecentActActivity.this).inflate(
                R.layout.footer_load_more, null);
        mFollowListFooter.setVisibility(View.INVISIBLE);

        mListView.getRefreshableView().addHeaderView(mHeadView);
        mListView.getRefreshableView().addFooterView(mFollowListFooter);

        listListener = new PhotoListListener(RecentActActivity.this);

        loadUtils = new LoadUtils(RecentActActivity.this);
        progressingDialog = new CustomProgressingDialog(this);
        progressingDialog.show();
    }

    private class PhotoListListener implements PullToRefreshBase.OnLastItemVisibleListener,
            PullToRefreshBase.OnRefreshListener<ListView> {
        private Context mContext;
        private static final long DEFAULT_LAST_REFRESH_TIME = -1;

        public PhotoListListener(Context context) {
            mContext = context;
            SharedPreferences sp = mContext.getSharedPreferences(
                    Constants.SharedPreferencesKey.NAME, Context.MODE_PRIVATE);
            mSpKey = Constants.SharedPreferencesKey.RECENT_PHOTO_LIST_LAST_REFRESH_TIME;


            mLastUpdatedTime = sp.getLong(mSpKey, DEFAULT_LAST_REFRESH_TIME);
        }

        @Override
        public void onLastItemVisible() {
            if (canLoadMore) {
                mPage += 1;
                mFollowListFooter.setVisibility(View.VISIBLE);

                PhotoActRequest.Builder builder = new PhotoActRequest.Builder()
                        .setPage(mPage).setId(id)
                        .setListener(loadMoreListener)
                        .setErrorListener(errorListener);

                PhotoActRequest request = builder.build();
                request.setTag(TAG);
                RequestQueue requestQueue = PSGodRequestQueue.getInstance(
                        mContext).getRequestQueue();
                requestQueue.add(request);
            }
        }

        @Override
        public void onRefresh(PullToRefreshBase<ListView> refreshView) {
            mPage = 1;
            // 上次刷新时间
            if (mLastUpdatedTime == DEFAULT_LAST_REFRESH_TIME) {
                mLastUpdatedTime = System.currentTimeMillis();
            }

            PhotoActRequest.Builder builder = new PhotoActRequest.Builder()
                    .setPage(mPage).setId(id)
                    .setListener(refreshListener)
                    .setErrorListener(errorListener);

            PhotoActRequest request = builder.build();
            request.setTag(TAG);
            RequestQueue requestQueue = PSGodRequestQueue.getInstance(mContext)
                    .getRequestQueue();
            requestQueue.add(request);
        }
    }

    private PhotoListListener listListener;

    private void initEvent() {
        mListView.setOnRefreshListener(listListener);
        mListView.setOnLastItemVisibleListener(listListener);
        mHeadImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mActs.get(0).getUrl().equals("")) {
                    Intent intent = new Intent(RecentActActivity.this, WebBrowserActivity.class);
                    intent.putExtra(WebBrowserActivity.KEY_DESC, mActs.get(0).getName());
                    intent.putExtra(WebBrowserActivity.KEY_URL, mActs.get(0).getUrl());
                    RecentActActivity.this.startActivity(intent);
                }
            }
        });
//        mHeadTxt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mActs.size() > 0) {
////                    loadUtils.upLoad(mActs.get(0).getType(), Long.parseLong(mActs.get(0).getAsk_id()));
//                    Intent intent = new Intent(RecentActActivity.this,MultiImageSelectActivity.class);
//                    intent.putExtra("AskId",Long.parseLong(mActs.get(0).getAsk_id()));
//                    intent.putExtra("ActivityId",mActs.get(0).getId());
//                    intent.putExtra("SelectType","TypeReplySelect");
//                    startActivity(intent);
//                }
//            }
//        });
    }

    /**
     * 暂停所有的下载
     */
    @Override
    public void onStop() {
        super.onStop();
        RequestQueue requestQueue = PSGodRequestQueue.getInstance(RecentActActivity.this)
                .getRequestQueue();
        requestQueue.cancelAll(TAG);
    }

    private void initData() {
        mListView.setRefreshing();
    }

    private Response.Listener<Activities> refreshListener = new Response.Listener<Activities>() {
        @Override
        public void onResponse(Activities response) {
            mPhotoItems.clear();
            if(response.getReplies().size() > 0) {
                mPhotoItems.addAll(response.getReplies());
            }
            mAdapter.notifyDataSetChanged();
            mActs.clear();
            if (response.getActs().size() > 0 && response.getActs().get(0) != null) {
                mActs.addAll(response.getActs());
                if(mHeadView.getVisibility() == View.GONE){
                    mHeadView.setVisibility(View.VISIBLE);
                }
                mTitle.setText(mActs.get(0).getName());
                ImageLoader.getInstance().
                        displayImage(mActs.get(0).getImage_url(),
                                mHeadImg, Constants.DISPLAY_IMAGE_OPTIONS);
            }else{
                mHeadView.setVisibility(View.GONE);
                mListView.setEmptyView(mEmptyView);
            }
            mListView.onRefreshComplete();

            if (response.getReplies().size() < 15) {
                canLoadMore = false;
            } else {
                canLoadMore = true;
            }
            // 保存本次刷新时间到sp
            mLastUpdatedTime = System.currentTimeMillis();
            if (android.os.Build.VERSION.SDK_INT >= 9) {
                RecentActActivity.this
                        .getSharedPreferences(
                                Constants.SharedPreferencesKey.NAME,
                                Context.MODE_PRIVATE).edit()
                        .putLong(mSpKey, mLastUpdatedTime).apply();
            } else {
                RecentActActivity.this
                        .getSharedPreferences(
                                Constants.SharedPreferencesKey.NAME,
                                Context.MODE_PRIVATE).edit()
                        .putLong(mSpKey, mLastUpdatedTime).commit();
            }
            if(progressingDialog.isShowing()){
                progressingDialog.dismiss();
            }
        }
    };


    private Response.Listener<Activities> loadMoreListener = new Response.Listener<Activities>() {
        @Override
        public void onResponse(final Activities response) {
            mPhotoItems.addAll(response.getReplies());
            mAdapter.notifyDataSetChanged();
            mListView.onRefreshComplete();

            mFollowListFooter.setVisibility(View.INVISIBLE);

            if (response.getReplies().size() < 15) {
                canLoadMore = false;
            } else {
                canLoadMore = true;
            }

            if(progressingDialog.isShowing()){
                progressingDialog.dismiss();
            }
        }
    };


    private Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            mListView.onRefreshComplete();
            mFollowListFooter.setVisibility(View.INVISIBLE);

            if(progressingDialog.isShowing()){
                progressingDialog.dismiss();
            }
        }
    };

    public void onEventMainThread(RefreshEvent event) {
        if(event.className.equals(this.getClass().getName())){
            try {
                mListView.setRefreshing(true);
            }catch (NullPointerException ne){}
        }
    }

}
