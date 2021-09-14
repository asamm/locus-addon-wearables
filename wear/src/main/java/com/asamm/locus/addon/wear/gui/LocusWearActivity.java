package com.asamm.locus.addon.wear.gui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.application.AppPreferencesManager;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.utils.TriStateLogicEnum;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.custom.MainNavigationDrawer;
import com.asamm.locus.addon.wear.gui.error.AppFailType;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;
import com.google.android.gms.wearable.Node;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import locus.api.utils.Logger;

/**
 * Base class for wearable activities containing basic comm handling and other common features
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class LocusWearActivity extends WearableActivity {

	private static final String TAG = "LocusWearActivity";
	private static final boolean IS_DEBUG_MODE_ENABLED = false;

	public WearActivityState mState = WearActivityState.ON_CREATE;

	protected MainNavigationDrawer mDrawer;
	private ImageView mDrawerCloseArrowImg;
	private TextView mTvNavDrawerTime;
	private Handler mNavDrawerTimeHandler;
	private DateFormat mDateFormat;
	/** Can be set by inheriting activity to skip drawer peek behavior on next resume */
	private boolean ignoreNextDrawerPeek = false;
	private static final int HANDSHAKE_TIMEOUT_MS = 8000;
	private static final int HANDSHAKE_TICK_MS = 400;

	/**
	 * number of ticks of mConnectionFailedTimer
	 */
	private volatile byte ticks = 0;

	/**
	 * activated on start for monitoring initial handshake exchange
	 */
	private CountDownTimer mConnectionFailedTimer;
	private final Object connectionTimerLock = new Object();

	/**
	 * only used on start in connection failed timer to monitor initial handshake request
	 */
	private volatile boolean mIsHandShakeReceived = false;
	/**
	 * only used on start in connection failed timer to monitor initial command request
	 */
	protected volatile boolean mIsInitialRequestReceived = false;

	private volatile AtomicBoolean mGetConnectedNodesSent = new AtomicBoolean(false);
	private volatile AtomicBoolean mHandshakeSent = new AtomicBoolean(false);
	private volatile AtomicBoolean mHandshakeRetrySent = new AtomicBoolean(false);

	private byte dbgClickCounter = 0;
	/**
	 * flag specifying if mobile phone is connected
	 */
	private volatile boolean mIsNodeConnected = false;

	/**
	 * Delegate for handling HW keys actions
	 */
	private LocusWearActivityHwKeyDelegate hwKeyDelegate;

	/**
	 * Each activity should define initial command which is sent automatically onStart()
	 *
	 * @return DataPath or null if activity want to just check connection on start and
	 * not issue any other custom command.
	 */
	protected abstract DataPayload getInitialCommandType();

	/**
	 * Expected type of response for initialCommand
	 */
	protected abstract DataPath getInitialCommandResponseType();

	/**
	 * This method is called only once as a callback, that activity's initial command
	 * got response and that comm is ready
	 */
	protected void onGotInitialCommandResponse() {
	}

	/**
	 * Consumes new data coming from WearListenerService
	 */
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		if (mConnectionFailedTimer != null) {
			if (path == DataPath.PUT_ON_CONNECTED_EVENT) {
				onConnectionFailedTimerTick();
			} else if (path == DataPath.PUT_HAND_SHAKE) {
				mIsHandShakeReceived = true;
				onConnectionFailedTimerTick();
			} else if (path == getInitialCommandResponseType()) {
				mIsInitialRequestReceived = true;
				onConnectionFailedTimerTick();
				onGotInitialCommandResponse();
			}
		}
	}

	protected LocusWearActivityHwKeyDelegate getHwKeyDelegate() {
		if (hwKeyDelegate == null) {
			hwKeyDelegate = LocusWearActivityHwKeyDelegate.Factory.createDelegate(this);
		}
		return hwKeyDelegate;
	}

	public WearActivityState getState() {
		return mState;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Detect that application was started from main application menu
		if (getIntent() != null && getIntent().getAction() != null &&
				"android.intent.action.MAIN".equals(getIntent().getAction()) &&
				(getIntent().getCategories() == null || getIntent().getCategories().contains("android.intent.category.LAUNCHER"))) {
				AppPreferencesManager.persistMapOffsetValues(this, 0, 0);
				AppPreferencesManager.persistMapBearing(this, (short)0);
		}
		this.mState = WearActivityState.ON_CREATE;
		super.onCreate(savedInstanceState);
		Class<? extends LocusWearActivity> c = MainApplication.getLastAppTask(this);
		// dispatch to correct activity - if activities are different, then application was restored
		// do not apply for child activities, which are not persisted when opened
		if (!c.equals(getClass())
				//&& !getClass().equals(ProfileListActivity.class)) {
				&& !isChildLocusWearActivity()) {
			Intent i = new Intent(this, c);
			startActivity(i);
			finish();
		}
	}

	@Override
	protected void onResume() {
		this.mState = WearActivityState.ON_RESUME;
		super.onResume();
		if (mTvNavDrawerTime != null) {
			mNavDrawerTimeHandler = new Handler();
			mNavDrawerTimeHandler.post(new Runnable() {
				@Override
				public void run() {
					String time = mDateFormat.format(new Date());
					mTvNavDrawerTime.setText((time.length() <= 4? " " : "") + time);
					mNavDrawerTimeHandler.postDelayed(this, 999);
				}
			});
		}
		AppPreferencesManager.persistLastActivity(this, getClass());
		hwKeyDelegate = null;
		registerHwKeyActions(getHwKeyDelegate());
		// checks connection and state of initial command, if not ready, initiates countDownTimer
		if (mConnectionFailedTimer == null) {
			startConnectionFailTimer();
		}

		if (mDrawer != null && AppPreferencesManager.isFirstAppStart(this)) {
			AppPreferencesManager.persistFirstAppStart(this);
			new Handler().postDelayed(() -> mDrawer.getController().openDrawer(), 800);
		} else if (mDrawer != null && !ignoreNextDrawerPeek) {
			new Handler().postDelayed(() -> mDrawer.getController().peekDrawer(), 800);
		}
		ignoreNextDrawerPeek = false;
	}

	@Override
	protected void onPause() {
		this.mState = WearActivityState.ON_PAUSE;
		super.onPause();
		if (mNavDrawerTimeHandler != null) {
			mNavDrawerTimeHandler.removeCallbacksAndMessages(null);
		}
	}

	/**
	 * Called by mConnectionFailedTimer tick,
	 * checks state of initial handshaking initiated after on start.
	 *
	 * @return true if handshaking finished successfully
	 */
	protected boolean onConnectionFailedTimerTick() {
		WearCommService wcs = WearCommService.getInstance();
		if (!wcs.isConnected()) {
			wcs.reconnectIfNeeded();
			return false;
		} else if (!mIsNodeConnected) {
			if (!mGetConnectedNodesSent.getAndSet(true)) {
				wcs.getConnectedNodes((result) -> {
					if (result != null && result.getNodes() != null) {
						for (Node node : result.getNodes()) {
							if (node.isNearby()) {
								mIsNodeConnected = true;
								break;
							}
						}
					}
					if (mIsNodeConnected) {
						onConnectionFailedTimerTick();
					} else {
						cancelConnectionFailedTimer();
						((MainApplication) getApplication()).doApplicationFail(AppFailType.CONNECTION_ERROR_NODE_NOT_CONNECTED);
					}
				});
			}
			return false;
		} else if (wcs.isAppInstalledOnDevice() != TriStateLogicEnum.TRUE) {
			// app is probably not installed on the device
			if (wcs.isAppInstalledOnDevice() == TriStateLogicEnum.FALSE) {
				getMainApplication().doApplicationFail(AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE);
			}
			return false;
		}

		// in approx. half of timeout resent requests one more time
		if (ticks == HANDSHAKE_TIMEOUT_MS / 2 / HANDSHAKE_TICK_MS
				&& !mHandshakeRetrySent.getAndSet(true)) {
			Logger.INSTANCE.logD(TAG, "Attempting second handshake");
			if (!mIsHandShakeReceived) {
				wcs.sendCommand(DataPath.GET_HAND_SHAKE);
			}
			if (!mIsInitialRequestReceived) {
				DataPayload p = getInitialCommandType();
				if (p != null) {
					wcs.sendDataItem(p.getPath(), p.getStorable());
				}
			}
		}

		// handle first tick - send hanshake and initial command request
		if (!mHandshakeSent.getAndSet(true) && isMakeHandshakeOnStart()) {
			wcs.sendCommand(DataPath.GET_HAND_SHAKE);
			DataPayload p = getInitialCommandType();
			if (p != null) {
				wcs.sendDataItem(p.getPath(), p.getStorable());
			} else {
				mIsInitialRequestReceived = true;
			}
		}

		boolean result = !isMakeHandshakeOnStart() ||
				(mIsHandShakeReceived && mIsInitialRequestReceived);
		if (result) {
			cancelConnectionFailedTimer();
			onHandShakeFinished();
		}
		return result;

	}

	/**
	 * Called after successful initial communication of the activity
	 */
	protected void onHandShakeFinished() {
	}

	protected void startConnectionFailTimer() {
		if (mConnectionFailedTimer != null || mState == WearActivityState.ON_STOP || mState == WearActivityState.ON_DESTROY)
			return;

		ticks = 0;
		mIsHandShakeReceived = false;
		mIsInitialRequestReceived = false;
		mGetConnectedNodesSent.set(false);
		mHandshakeSent.set(false);
		mHandshakeRetrySent.set(false);

		synchronized (connectionTimerLock) {
			if (mConnectionFailedTimer != null || mState == WearActivityState.ON_STOP || mState == WearActivityState.ON_DESTROY)
				return;

			mConnectionFailedTimer = new CountDownTimer(HANDSHAKE_TIMEOUT_MS, HANDSHAKE_TICK_MS) {
				@Override
				public void onTick(long l) {
					ticks++;
					onConnectionFailedTimerTick();
				}

				@Override
				public void onFinish() {
					Logger.INSTANCE.logE(LocusWearActivity.this.getClass().getSimpleName(), "Connection Failed!");
					cancelConnectionFailedTimer();
					/* Could not establish handshake connection */
					((MainApplication) getApplication()).doApplicationFail(AppFailType.CONNECTION_FAILED);
				}
			};
			mConnectionFailedTimer.start();
		}
	}

	protected void cancelConnectionFailedTimer() {
		synchronized (connectionTimerLock) {
			if (mConnectionFailedTimer != null) {
				mConnectionFailedTimer.cancel();
				mConnectionFailedTimer = null;   // and canceling and nulling timer
			}
		}
	}

	@Override
	protected void onStop() {
		this.mState = WearActivityState.ON_STOP;
		super.onStop();
		cancelConnectionFailedTimer();
	}

	@Override
	protected void onDestroy() {
		this.mState = WearActivityState.ON_DESTROY;
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		this.mState = WearActivityState.ON_START;
		super.onStart();
		mDrawer = findViewById(R.id.navigation_drawer);
		mDrawerCloseArrowImg = findViewById(R.id.imageViewDrawerOpened);
		mTvNavDrawerTime = findViewById(R.id.navDrawerTvTime);
		if (mTvNavDrawerTime != null && IS_DEBUG_MODE_ENABLED) {
			mTvNavDrawerTime.setOnClickListener(view -> {
 				dbgClickCounter++;
 				if (dbgClickCounter == 6) {
 					dbgClickCounter = 0;
 					boolean isDebug = AppPreferencesManager.isDebug(this);
 					AppPreferencesManager.persistIsDebug(this, !isDebug);
					Toast.makeText(this, "Debug mode " + (isDebug? "disabled" : "enabled"), Toast.LENGTH_LONG).show();
				}
            });
		}
		mDateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
	}

	// current activity state
	public enum WearActivityState {
		ON_CREATE,
		ON_START,
		ON_RESUME,
		ON_PAUSE,
		ON_STOP,
		ON_DESTROY

	}

	/**
	 * Handling of item click in main menu/navigation drawer
	 */
	public void handleNavigationDrawerItemClicked(View v) {
		final Class<? extends LocusWearActivity> activityToStart;
		switch (v.getId()) {
			case R.id.navigation_drawer_item_map:
				activityToStart = MapActivity.class;
				break;
			case R.id.navigation_drawer_item_track_rec:
				activityToStart = TrackRecordActivity.class;
				break;
			case R.id.btn_settings:
				activityToStart = MainSettingsActivity.class;
				break;
			default:
				activityToStart = null;
				break;
		}
		startLocusWearActivity(activityToStart);
	}

	protected void startLocusWearActivity(Class<? extends LocusWearActivity> activityToStart) {
		if (mDrawer != null) {
			mDrawer.getController().closeDrawer();
		}
		if (activityToStart == null ||
				this.getClass().getSimpleName().equals(activityToStart.getSimpleName())) {
			return;
		}
		Intent i = new Intent(this, activityToStart);
		AppPreferencesManager.persistLastActivity(this, activityToStart);
		startActivity(i);
	}

	/**
	 * Activities that use request for periodic data ie. Map or active track recording should
	 * override this method and return true.
	 * <p>
	 * Used when transitionig between activities to automatically disable any currently
	 * receiving periodic data if no activity or activity withou periodic data use is shown.
	 *
	 * @return whether activity uses periodic data request
	 */
	public boolean isUsePeriodicData() {
		return false;
	}

	/**
	 * Overriding and returning false means that all handshaking and even initial command request
	 * response are skipped!
	 *
	 * @return if should do initialization hanshake and init communication
	 */
	protected boolean isMakeHandshakeOnStart() {
		return true;
	}

	public MainApplication getMainApplication() {
		return (MainApplication) getApplication();
	}

	@Override
	public void onEnterAmbient(Bundle ambientDetails) {
		super.onEnterAmbient(ambientDetails);
		if (mDrawer != null) {
			mDrawer.setBackgroundColor(Color.BLACK);
			mDrawerCloseArrowImg.setBackgroundColor(Color.BLACK);
		}
	}

	@Override
	public void onExitAmbient() {
		super.onExitAmbient();
		if (mDrawer != null) {
			mDrawer.setBackgroundColor(getColor(R.color.base_primary));
			mDrawerCloseArrowImg.setBackgroundColor(getColor(R.color.nav_drawer_dark_background));
		}
	}

	/**
	 * This function can be overriden in inheriting activities and is called during each on resume.
	 * Activity that wishes to use custom HW button handling should register appropriate listeners
	 * in the provided [delegate]
	 */
	protected abstract void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate);

	/**
	 * @return whether this activity is allowed to run on top of other locus wear activity
	 * (ie. doesn't call finish() on previous activity after this activity is resumed)
	 */
	public boolean isChildLocusWearActivity() {
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getHwKeyDelegate().onKeyDown(keyCode, event) ? true : super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return getHwKeyDelegate().onKeyLongPress(keyCode, event) ? true : super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return getHwKeyDelegate().onKeyUp(keyCode, event) ? true : super.onKeyUp(keyCode, event);
	}

	protected void enableCustomRotatryActions() {
		getHwKeyDelegate().registerDefaultRotaryMotionListener(getWindow().getDecorView().getRootView());
	}
	protected void setIgnoreNextDrawerPeek() {
		ignoreNextDrawerPeek = true;
	}
}
