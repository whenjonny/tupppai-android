package com.psgod.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.psgod.R;
import com.psgod.UserPreferences;
import com.psgod.eventbus.RefreshEvent;
import com.psgod.ui.activity.MovieActivity;

import de.greenrobot.event.EventBus;

/**
 * 影视fragment
 * Created by xiaoluo on 2016/5/20.
 */
public class MovieFragment extends BaseFragment implements OnClickListener{

    public Context mContext;
    private WebView mWebview;
    private TextView mWebtitle;
    private String mCookieMOVIE = null;
    private String mToken;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie, container, false);
        EventBus.getDefault().register(this);
        mContext = getActivity();

        getCookie();
        initView(view);
        return view;
    }

    //取得带cookie的url
    private void getCookie() {
        mToken = UserPreferences.TokenVerify.getToken();
        mCookieMOVIE = "http://wechupin.com/index-app.html?c=" + mToken +"&from=android#app/playcategory";
        System.out.println("cookieurl" + mCookieMOVIE + "\n");
    }

    private class MovieWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            super.shouldOverrideUrlLoading(view, url);

            //以url的最后12位字符做为判断，当新地址最后不是playcategory时，才会进行跳转
            String mStr = url.substring(url.length() - 12, url.length());

            if (!mStr.equals("playcategory")) {
                //重新拼接新URL
                //只取点击到的地址的前半部和后半部，去除其中的c=XXX和from=XXX
                String StrUrl = url.substring(url.indexOf("#") + 1, url.length());
                String mUrl = "http://wechupin.com/index-app.html#" + StrUrl;
                //新地址传递跳转
                Intent intent = new Intent();
                intent.setClass(mContext, MovieActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("Url", mUrl);
                intent.putExtras(bundle);
                mContext.startActivity(intent);
            }
            return true;
        }
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            mWebtitle.setText(view.getTitle());
        }
    }

    private void initView(View view) {
        mWebview = new WebView(mContext);
        mWebview = (WebView) view.findViewById(R.id.fragment_movie_webview);
        mWebtitle = (TextView) view.findViewById(R.id.webview_title);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.setWebViewClient(new MovieWebViewClient());
        mWebview.loadUrl(mCookieMOVIE);
    }

    // evenbus，用于从Activity返回原来的Fragment时，调用重新载入webview
    // 此处是为了解决原webview随着点击而跳转的问题
    public void onEventMainThread(RefreshEvent event) {
        if(event.className.equals(this.getClass().getName())){
            try {
                mWebview.loadUrl(mCookieMOVIE);
            } catch (NullPointerException nu) {
            } catch (Exception e) {
            }
        }
    }

    public void onClick(View v) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}

