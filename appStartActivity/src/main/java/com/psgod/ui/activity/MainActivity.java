package com.psgod.ui.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.android.agoo.client.BaseRegistrar;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.psgod.Constants;
import com.psgod.PSGodApplication;
import com.psgod.R;
import com.psgod.UserPreferences;
import com.psgod.Utils;
import com.psgod.WeakReferenceHandler;
import com.psgod.eventbus.InitEvent;
import com.psgod.eventbus.MyInfoRefreshEvent;
import com.psgod.eventbus.NetEvent;
import com.psgod.eventbus.PushEvent;
import com.psgod.eventbus.RefreshEvent;
import com.psgod.eventbus.UpdateTabStatusEvent;
import com.psgod.network.request.BaseRequest;
import com.psgod.network.request.PSGodErrorListener;
import com.psgod.network.request.PSGodRequestQueue;
import com.psgod.network.request.ReportDeviceInfo;
import com.psgod.receiver.NetReceiver;
import com.psgod.receiver.PushMessageReceiver;
import com.psgod.ui.fragment.HomePageFocusFragment;
import com.psgod.ui.fragment.HomePageFragment;
import com.psgod.ui.fragment.HomePageHotFragment;
import com.psgod.ui.fragment.InprogressPageFragment;
import com.psgod.ui.fragment.MyPageFragment;
import com.psgod.ui.fragment.RecentPageActFragment;
import com.psgod.ui.fragment.RecentPageAsksFragment;
import com.psgod.ui.fragment.RecentPageFragment;
import com.psgod.ui.fragment.RecentPageWorksFragment;
import com.psgod.ui.widget.dialog.CameraPopupwindow;
import com.umeng.message.PushAgent;
import com.umeng.message.entity.UMessage;
import com.umeng.update.UmengUpdateAgent;

import de.greenrobot.event.EventBus;

/**
 * 主界面
 *
 * @author rayalyuan
 *
 */
public class MainActivity extends PSGodBaseActivity implements
		OnCheckedChangeListener {
	private static final String TAG = MainActivity.class.getSimpleName();
	public static final int JUMP_FROM_LOGIN = 100;
	// 是否准备双击退出程序
	private static Boolean isExit = false;

	public static interface IntentParams {
		String KEY_FRAGMENT_ID = "FragmentId";

		String KEY_HOMEPAGE_ID = "HomepageId"; // 首页热门 关注
		String KEY_RECENTPAGE_ID = "RecentpageId"; // 最新求p 作品
		String KEY_INPROGRESS_ID = "InprogressId"; // 进行中 求P 帮P 已完成
		String KEY_NEED_REFRESH = "NeedRefresh"; // 是否需要刷新(true or false)，默认true

		int VALUE_FRAGMENT_ID_HOMEPAGE = R.id.activity_main_tab_home_page;
		int VALUE_FRAGMENT_ID_RECENT = R.id.activity_main_tab_recent;
		int VALUE_FRAGMENT_ID_INPROGRESSING = R.id.activity_main_tab_inprogressing;
		int VALUE_FRAGMENT_ID_USER = R.id.activity_main_tab_user;

		int VALUE_HOMEPAGE_ID_HOT = 0;
		int VALUE_HOMEPAGE_ID_FOCUS = 1;


		// 求p和作品页面
		int VALUE_RECENTPAGE_ID_ACT = 2;
		int VALUE_RECENTPAGE_ID_ASKS =0;
		int VALUE_RECENTPAGE_ID_WORKS = 1;

		// 进行中 求P 帮P 已完成
		int VALUE_INPROGRESS_ID_ASK = 0;
		int VALUE_INPROGRESS_ID_REPLY = 1;
		int VALUE_INPROGRESS_ID_COMPLETE = 2;
	}

	private final int[] TAB_IDS = new int[] { R.id.activity_main_tab_home_page,
			R.id.activity_main_tab_recent,
			R.id.activity_main_tab_inprogressing, R.id.activity_main_tab_user };
	private final Map<Integer, Fragment> MAIN_ACTIVITY_FRAGMENTS = new HashMap<Integer, Fragment>();
	private final int DEFAULT_FRAGMENT_ID = -1;

	private FragmentManager mFragmentManager;
	private RadioGroup mBottomTab;
	private RadioButton mHomeBtn;
	private RadioButton mRecentBtn;
	private RadioButton mInprogressingBtn;

	private ImageView mAddImgView;
	private CameraPopupwindow cameraPopupwindow;

	private RelativeLayout mParent;

	// 小红点区域
	private LinearLayout mTabTipsMessage;

	private int mCurrentFragmentID = DEFAULT_FRAGMENT_ID;
	private WeakReferenceHandler mHandler = new WeakReferenceHandler(this);

	private PushMessageReceiver pushMessageReceiver;

	// 网络状态Receiver
	private NetReceiver mNetReceiver;

	private PushAgent mPushAgent;
	// Umeng device token
	private String mDeviceToken = "";

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBottomTab = (RadioGroup) findViewById(R.id.psgod_rg_tab);

		if(BaseRequest.PSGOD_BASE_URL.equals(BaseRequest.PSGOD_BASE_TEST_URL)){
			mBottomTab.setBackground(getResources().getDrawable(R.color.color_9fc25b));
		}else{
			mBottomTab.setBackground(getResources().getDrawable(R.color.white));
		}

		mHomeBtn = (RadioButton) findViewById(R.id.activity_main_tab_home_page);
		mRecentBtn = (RadioButton) findViewById(R.id.activity_main_tab_recent);
		mInprogressingBtn = (RadioButton) findViewById(R.id.activity_main_tab_inprogressing);
		mAddImgView = (ImageView) findViewById(R.id.activity_main_tab_add);
		mParent = (RelativeLayout) findViewById(R.id.activity_main_parent);

		// 个人页面tab小红点 消息按钮
		mTabTipsMessage = (LinearLayout) findViewById(R.id.psgod_rg_tab_tips_user);

		mBottomTab.setOnCheckedChangeListener(this);
		mFragmentManager = getSupportFragmentManager();
		showFragment(R.id.activity_main_tab_home_page);

		// 启动友盟消息推送
		mPushAgent = PushAgent.getInstance(this);
		mPushAgent.onAppStart();
		mPushAgent.enable();
		mDeviceToken = BaseRegistrar.getRegistrationId(this);

		Intent intent = getIntent();
		// 判断从哪里跳过来 重新登录 才发送umeng deviceToken
		if (intent.hasExtra(Constants.IntentKey.ACTIVITY_JUMP_FROM)) {
			int jumpFrom = intent.getIntExtra(
					Constants.IntentKey.ACTIVITY_JUMP_FROM, 0);
			if (jumpFrom == JUMP_FROM_LOGIN) {
				// 线程循环 直至拿到device token
				mHandler.postDelayed(runnable, 1000);
			}
		}

		// 检测sp中未读消息的数量 更新下方tab栏的状态
		initTabViews();

		initEvents();

		// 初始化广播监听
		initReceiver();

		// umeng应用自动更新
		UmengUpdateAgent.update(this);
		EventBus.getDefault().register(this);
	}

	// MainActivity获得焦点的时候 更新userFragment内的数据
	@Override
	public void onStart() {
		super.onStart();
//		updateUserFragmentData();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(pushMessageReceiver);
		unregisterReceiver(mNetReceiver);
		EventBus.getDefault().unregister(this);
		super.onDestroy();
	}

	private void removeAllFragment(){
		List<Fragment> list =  getSupportFragmentManager().getFragments();
		if(list != null) {
			for (Fragment fragment : list) {
				if(fragment != null) {
					FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
					transaction.remove(fragment);
					transaction.commit();
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	// 检测sp中未读消息的数量 更新下方tab栏的状态
	public void initTabViews() {
		// 获取sp中未读消息的总数量
		int mUnreadMessageCount = UserPreferences.PushMessage
				.getPushMessageCount(UserPreferences.PushMessage.PUSH_COMMENT)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_REPLY)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_FOLLOW)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_SYSTEM)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_LIKE);

		if (mUnreadMessageCount > 0) {
			mTabTipsMessage.setVisibility(View.VISIBLE);
		}
	}

	public void initReceiver() {
		// 注册消息推送监听
		pushMessageReceiver = new PushMessageReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PUSH_MESSAGE_BROADCAST");
		registerReceiver(pushMessageReceiver, filter);

		// 注册网络监听
		mNetReceiver = new NetReceiver();
		IntentFilter netIntentFilter = new IntentFilter();
		netIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mNetReceiver, netIntentFilter);
	}

	// EventBus 接收网络变化事件
	public void onEventMainThread(NetEvent event) {
		// TODO 处理网络中断情况 提示方式
		// 网络中断情况
		if (!event.getIsNet()) {
			Toast.makeText(MainActivity.this, "网络连接不可用，请稍后重试",
					Toast.LENGTH_LONG).show();
		}
	}

	// EventBus 处理推送事件
	public void onEventMainThread(PushEvent event) {
		if (event.pushObjectType == PushEvent.TYPE_ACT_MAIN) {
			// 推送消息的类型和数量
			int mType = event.getPushType();
			int mCount = event.getPushCount();

			// 更新各类推送本地的数量
			updatePushData(mType, mCount);

			// 更新底部Tab小红点数量
			setTabBarTip(mType, mCount);
		}
	}

	// EventBus 更新下方Tab栏状态 (暂时保留 点击消息页面分类时调用)
	public void onEventMainThread(UpdateTabStatusEvent event) {
		// 判断消息页面sp中未读数量 若为零 则更新状态栏的小红点状态

		int fragmentMessageCount = UserPreferences.PushMessage
				.getPushMessageCount(UserPreferences.PushMessage.PUSH_COMMENT)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_REPLY)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_FOLLOW)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_SYSTEM)
				+ UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_LIKE);

		// if (fragmentMessageCount == 0
		// && mTabTipsMessage.getVisibility() == View.VISIBLE) {
		//
		// }
		mTabTipsMessage.setVisibility(View.INVISIBLE);
	}

	// 更新userFragment内的数据 如头像 各种数量等等
	private void updateUserFragmentData() {
		EventBus.getDefault().post(new InitEvent());
	}

	// 更新对应页面中未读消息的数量
	public void updatePageState(int type, int count) {
		EventBus.getDefault().post(
				new PushEvent(type, count, PushEvent.TYPE_FRAG_PAGE));
	}

	// 更新各类推送本地的数量
	public void updatePushData(int type, int count) {
		// 设置是否点击 我的 消息按钮
		Constants.IS_MESSAGE_NEW_PUSH_CLICK = false;

		if (type != -1) {
			switch (type) {
			case Constants.PUSH_MESSAGE_COMMENT:
				int commentCount = UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_COMMENT);
				commentCount = count + commentCount;

				// 更新页面内红点的数量
				updatePageState(type, commentCount);
				break;

			case Constants.PUSH_MESSAGE_REPLY:
				int replyCount = UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_REPLY);
				replyCount = count + replyCount;

				// 更新页面内红点的数量
				updatePageState(type, replyCount);
				break;

			case Constants.PUSH_MESSAGE_FOLLOW:
				int followCount = UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_FOLLOW);
				followCount = count + followCount;

				// 更新页面内红点的数量
				updatePageState(type, followCount);
				break;

			// case Constants.PUSH_MESSAGE_INVITE:
			// int inviteCount = UserPreferences.PushMessage
			// .getPushMessageCount(UserPreferences.PushMessage.PUSH_INVITE);
			// inviteCount = count + inviteCount;
			//
			// UserPreferences.PushMessage.setPushMessageCount(
			// UserPreferences.PushMessage.PUSH_INVITE, inviteCount);
			// // 更新页面内红点的数量
			// updatePageState(type, inviteCount);
			// break;

			case Constants.PUSH_MESSAGE_LIKE:
				int likeCount = UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_LIKE);
				likeCount = likeCount + count;

				// 更新页面内红点的数量
				updatePageState(type, likeCount);
				break;

			case Constants.PUSH_MESSAGE_SYSTEM:
				int systemCount = UserPreferences.PushMessage
						.getPushMessageCount(UserPreferences.PushMessage.PUSH_SYSTEM);
				systemCount = count + systemCount;

				// 更新页面内红点的数量
				updatePageState(type, systemCount);
				break;

			default:
				break;
			}
		}
	}

	private void setTabBarTip(int t, int c) {
		mTabTipsMessage.setVisibility(View.VISIBLE);
	}

	private void initEvents() {

		// 双击最近tab自动刷新
		mRecentBtn.setOnTouchListener(new OnTouchListener() {
			int count = 0;
			int firClick = 0;
			int secClick = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					count++;
					if (count == 1) {
						firClick = (int) System.currentTimeMillis();
					} else if (count == 2) {
						secClick = (int) System.currentTimeMillis();
						if (secClick - firClick < 1200) {
							// 双击事件 下拉刷新首页热门列表
							Intent intent = new Intent(MainActivity.this,
									MainActivity.class);
							intent.putExtra(
									MainActivity.IntentParams.KEY_FRAGMENT_ID,
									MainActivity.IntentParams.VALUE_FRAGMENT_ID_RECENT);
							if (Constants.CURRENT_RECENTPAGE_TAB == 2) {
//								intent.putExtra(
//										MainActivity.IntentParams.KEY_RECENTPAGE_ID,
//										IntentParams.VALUE_RECENTPAGE_ID_ACT);
								EventBus.getDefault().post(new RefreshEvent(RecentPageActFragment.class.getName()));
							}else if (Constants.CURRENT_RECENTPAGE_TAB == 0) {
//								intent.putExtra(
//										MainActivity.IntentParams.KEY_RECENTPAGE_ID,
//										MainActivity.IntentParams.VALUE_RECENTPAGE_ID_ASKS);
								EventBus.getDefault().post(new RefreshEvent(RecentPageAsksFragment.class.getName()));
							}else if (Constants.CURRENT_RECENTPAGE_TAB == 1){
//								intent.putExtra(
//										MainActivity.IntentParams.KEY_RECENTPAGE_ID,
//										MainActivity.IntentParams.VALUE_RECENTPAGE_ID_WORKS);
								EventBus.getDefault().post(new RefreshEvent(RecentPageWorksFragment.class.getName()));
							}

//							intent.putExtra(
//									MainActivity.IntentParams.KEY_NEED_REFRESH,
//									true);
//							startActivity(intent);
						}
						count = 0;
						firClick = 0;
						secClick = 0;
					}
				}
				return false;
			}
		});

		// 双击首页tab刷新
		mHomeBtn.setOnTouchListener(new OnTouchListener() {
			int count = 0;
			int firClick = 0;
			int secClick = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					count++;
					if (count == 1) {
						firClick = (int) System.currentTimeMillis();
					} else if (count == 2) {
						secClick = (int) System.currentTimeMillis();
						if (secClick - firClick < 1200) {
							// 双击事件 下拉刷新首页热门列表
//							Intent intent = new Intent(MainActivity.this,
//									MainActivity.class);
//							intent.putExtra(
//									MainActivity.IntentParams.KEY_FRAGMENT_ID,
//									MainActivity.IntentParams.VALUE_FRAGMENT_ID_HOMEPAGE);
							if (Constants.CURRENT_HOMEPAGE_TAB == 0) {
//								intent.putExtra(
//										MainActivity.IntentParams.KEY_HOMEPAGE_ID,
//										MainActivity.IntentParams.VALUE_HOMEPAGE_ID_HOT);
								EventBus.getDefault().post(new RefreshEvent(HomePageHotFragment.class.getName()));
							}
							if (Constants.CURRENT_HOMEPAGE_TAB == 1) {
//								intent.putExtra(
//										MainActivity.IntentParams.KEY_HOMEPAGE_ID,
//										MainActivity.IntentParams.VALUE_HOMEPAGE_ID_FOCUS);
								EventBus.getDefault().post(new RefreshEvent(HomePageFocusFragment.class.getName()));
							}
//							intent.putExtra(
//									MainActivity.IntentParams.KEY_NEED_REFRESH,
//									true);
//							startActivity(intent);
						}
						count = 0;
						firClick = 0;
						secClick = 0;
					}
				}
				return false;
			}
		});

		// 双击进行中tab刷新
		mInprogressingBtn.setOnTouchListener(new OnTouchListener() {
			int count = 0;
			int firClick = 0;
			int secClick = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (MotionEvent.ACTION_DOWN == event.getAction()) {
					count++;
					if (count == 1) {
						firClick = (int) System.currentTimeMillis();
					} else if (count == 2) {
						secClick = (int) System.currentTimeMillis();
						if (secClick - firClick < 1200) {
							// 双击事件 下拉刷新首页热门列表
							Intent intent = new Intent(MainActivity.this,
									MainActivity.class);
							intent.putExtra(
									MainActivity.IntentParams.KEY_FRAGMENT_ID,
									MainActivity.IntentParams.VALUE_FRAGMENT_ID_INPROGRESSING);
							if (Constants.CURRENT_INPROGRESS_TAB == 0) {
								intent.putExtra(
										MainActivity.IntentParams.KEY_INPROGRESS_ID,
										MainActivity.IntentParams.VALUE_INPROGRESS_ID_ASK);
							} else if (Constants.CURRENT_INPROGRESS_TAB == 1) {
								intent.putExtra(
										MainActivity.IntentParams.KEY_INPROGRESS_ID,
										MainActivity.IntentParams.VALUE_INPROGRESS_ID_REPLY);
							} else if (Constants.CURRENT_INPROGRESS_TAB == 2) {
								intent.putExtra(
										MainActivity.IntentParams.KEY_INPROGRESS_ID,
										MainActivity.IntentParams.VALUE_INPROGRESS_ID_COMPLETE);
							}

							intent.putExtra(
									MainActivity.IntentParams.KEY_NEED_REFRESH,
									true);
							startActivity(intent);
						}
						count = 0;
						firClick = 0;
						secClick = 0;
					}
				}
				return false;
			}
		});

		mAddImgView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showCameraPopupwindow(v); // 弹出选择上传求P还是作品的PopupWindow
			}
		});
	}

	private void showCameraPopupwindow(View view) {
		if (null == cameraPopupwindow) {
			cameraPopupwindow = new CameraPopupwindow(this);
		}

		cameraPopupwindow.showCameraPopupwindow(view);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		final Intent mIntent;
		if (intent == null) {
			mIntent = new Intent();
		} else {
			mIntent = intent;
		}
		int fragmentId = mIntent.getIntExtra(IntentParams.KEY_FRAGMENT_ID,
				DEFAULT_FRAGMENT_ID);
		if (fragmentId != DEFAULT_FRAGMENT_ID) {
			mBottomTab.check(fragmentId);
			Fragment mFragment = getFragment(fragmentId);
			if (fragmentId == R.id.activity_main_tab_home_page) {
				HomePageFragment fragment = (HomePageFragment) getFragment(R.id.activity_main_tab_home_page);
				fragment.onNewIntent(mIntent);
			}
			if (fragmentId == R.id.activity_main_tab_recent) {
				RecentPageFragment recentFragment = (RecentPageFragment) getFragment(R.id.activity_main_tab_recent);
				recentFragment.onNewIntent(mIntent);
			}
			if (fragmentId == R.id.activity_main_tab_inprogressing) {
				InprogressPageFragment inprogressFragment = (InprogressPageFragment) getFragment(fragmentId);
				// showFragment(R.id.activity_main_tab_inprogressing);
				inprogressFragment.onNewIntent(mIntent);
			}
		}
		boolean isFinishActivity = mIntent.getBooleanExtra(
				Constants.IntentKey.IS_FINISH_ACTIVITY, false);
		mIntent.removeExtra(Constants.IntentKey.IS_FINISH_ACTIVITY);
		if (isFinishActivity) {
			String destActivityName = mIntent
					.getStringExtra(Constants.IntentKey.DEST_ACTIVITY_NAME);
			if (!TextUtils.isEmpty(destActivityName)) {
				Intent newIntent = new Intent();
				newIntent.setClassName(MainActivity.this, destActivityName);
				Bundle extras = mIntent.getExtras();
				if (extras != null) {
					newIntent.putExtras(extras);
				}
				startActivity(newIntent);
				finish();
			}
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		showFragment(checkedId);

		if (checkedId == R.id.activity_main_tab_recent) {
			// 若关注了新的用户 则自动刷新我的关注页面
			// if (Constants.IS_FOCUS_FRAGMENT_CREATED &&
			// Constants.IS_FOLLOW_NEW_USER) {
			// FocusFragment focusFragment = (FocusFragment)
			// getFragment(R.id.activity_main_tab_recent);
			// focusFragment.setRefreshing();
			// }
		} else if (checkedId == R.id.activity_main_tab_user) {
			mTabTipsMessage.setVisibility(View.INVISIBLE);
			EventBus.getDefault().post(new MyInfoRefreshEvent(MyPageFragment.class.getSimpleName()));
		}
	}

	// 初始化进程 检查device token是否存在
	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (mDeviceToken.equals("")) {
				mDeviceToken = BaseRegistrar
						.getRegistrationId(MainActivity.this);
				mHandler.postDelayed(runnable, 1000);
			} else {
				reportDeviceToken(mDeviceToken);
			}
		}
	};

	// 获取设备的mac地址
	public String getMacAddress() {
		String macAddress = null, ip = null;
		WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
		if (null != info) {
			macAddress = info.getMacAddress();
		}
		return macAddress;
	}

	private void reportDeviceToken(String token) {
		// 获取到deviceToken之后关闭循环
		// mHandler.removeCallbacks(runnable);

		String version = Utils.getAppVersion(this);

		ReportDeviceInfo.Builder builder = new ReportDeviceInfo.Builder()
				.setToken(token).setMac(getMacAddress())
				.setName(android.os.Build.MODEL)
				.setOs(android.os.Build.VERSION.RELEASE).setVersion(version)
				.setListener(reportDeviceInfoListener)
				.setErrorListener(errorListener);

		ReportDeviceInfo request = builder.build();

		request.setTag(TAG);
		RequestQueue requestQueue = PSGodRequestQueue.getInstance(
				MainActivity.this).getRequestQueue();
		requestQueue.add(request);
	}

	private Listener<Boolean> reportDeviceInfoListener = new Listener<Boolean>() {
		@Override
		public void onResponse(Boolean response) {
		}
	};

	private PSGodErrorListener errorListener = new PSGodErrorListener() {
		@Override
		public void handleError(VolleyError error) {
			// TODO Auto-generated method stub
		}
	};

	private void showFragment(int id) {
		if (mCurrentFragmentID == id) {
			return;
		}

		if (id == R.id.activity_main_tab_home_page) {
			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		} else {
			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		}

		FragmentTransaction fragmentTransaction = mFragmentManager
				.beginTransaction();

		// 隐藏正在显示的Fragment
		Fragment fromFragment = getFragment(mCurrentFragmentID);
		if (fromFragment != null) {
			fragmentTransaction.hide(fromFragment);
		}

		// 显示要显示的Fragment
		Fragment toFragment = getFragment(id);
		if (toFragment.isAdded()) {
			fragmentTransaction.show(toFragment).commit();
		} else {
			fragmentTransaction.add(R.id.psgod_fl_content, toFragment).commit();
		}

		mCurrentFragmentID = id;
	}

	private Fragment getFragment(int id) {
		Fragment fragment = MAIN_ACTIVITY_FRAGMENTS.get(id);
		Log.v("test", "getFragment");
		if (fragment == null) {
			switch (id) {
			case R.id.activity_main_tab_home_page:
				fragment = new HomePageFragment();
				break;
			case R.id.activity_main_tab_recent:
				fragment = new RecentPageFragment();
				break;
			case R.id.activity_main_tab_inprogressing:
				fragment = new InprogressPageFragment();
				break;
			case R.id.activity_main_tab_user:
				fragment = new MyPageFragment();
				break;
			}
			MAIN_ACTIVITY_FRAGMENTS.put(id, fragment);
		}
		return fragment;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exitApp();
		}
		return false;
	}

	private void exitApp() {
		Timer tExit = null;
		if (isExit == false) {
			// 准备退出状态
			isExit = true;

			Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT)
					.show();
			// showToast(new PSGodToast("再按一次退出程序"));
			tExit = new Timer();
			tExit.schedule(new TimerTask() {
				@Override
				public void run() {
					// 取消准备状态
					isExit = false;
				}
			}, 2000);
		} else {
			finish();
			System.exit(0);
		}
	}

	/**
	 * 暂停所有的下载
	 */
	@Override
	public void onStop() {
		super.onStop();
		RequestQueue requestQueue = PSGodRequestQueue.getInstance(this)
				.getRequestQueue();
		requestQueue.cancelAll(this);
	}

	public static void startNewActivityAndFinishAllBefore(Context context,
			String destActivityName, Bundle extras) {
		Intent intent = new Intent(context, MainActivity.class);
		if (extras != null) {
			intent.putExtras(extras);
		}
		intent.putExtra(Constants.IntentKey.DEST_ACTIVITY_NAME,
				destActivityName);
		intent.putExtra(Constants.IntentKey.IS_FINISH_ACTIVITY, true);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
