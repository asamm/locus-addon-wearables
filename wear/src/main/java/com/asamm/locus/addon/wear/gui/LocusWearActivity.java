package com.asamm.locus.addon.wear.gui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.ImageView;

import com.asamm.locus.addon.wear.AppPreferencesManager;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.custom.MainNavigationDrawer;
import com.asamm.locus.addon.wear.gui.error.AppFailType;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;

import locus.api.utils.Logger;

/**
 * Base class for wearable activities containing basic comm handling and other common features
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class LocusWearActivity extends WearableActivity {

	private static final String TAG = "LocusWearActivity";
	public WearActivityState mState = WearActivityState.ON_CREATE;

	protected MainNavigationDrawer mDrawer;
	private ImageView mDrawerCloseArrowImg;
	private static final int HANDSHAKE_TIMEOUT_MS = 6000;
	private static final int HANDSHAKE_TICK_MS = 400;

	/**
	 * number of ticks of mConnectionFailedTimer
	 */
	private volatile byte ticks = 0;
	/**
	 * activated on start for monitoring initial handshake exchange
	 */
	protected CountDownTimer mConnectionFailedTimer;
	/**
	 * only used on start in connection failed timer to monitor initial handshake request
	 */
	private volatile boolean mIsHandShakeReceived = false;
	/**
	 * only used on start in connection failed timer to monitor initial command request
	 */
	protected volatile boolean mIsInitialRequestReceived = false;

	/**
	 * Each activity should define initial command which is sent automatically onStart()
	 *
	 * @return DataPath or null if activity want to just check connection on start and
	 * not issue any other custom command.
	 */
	protected abstract DataPayload getInitialCommandType();

	/**
	 * Expected type of response for initialCommand
	 *
	 * @return
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
	 *
	 * @param path
	 * @param data
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

	public WearActivityState getState() {
		return mState;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		this.mState = WearActivityState.ON_CREATE;
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		this.mState = WearActivityState.ON_RESUME;
		super.onResume();
		if (mDrawer != null && AppPreferencesManager.isFirstAppStart(this)) {
			AppPreferencesManager.persistFirstAppStart(this);
			new Handler().postDelayed(() -> mDrawer.getController().openDrawer(), 800);
		}
	}

	@Override
	protected void onPause() {
		this.mState = WearActivityState.ON_PAUSE;
		super.onPause();
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
		}

		// in approx. half of timeout resent requests one more time
		if (ticks == HANDSHAKE_TIMEOUT_MS / 2 / HANDSHAKE_TICK_MS) {
			Logger.logD(TAG, "Attempting second handshake");
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
		if (ticks == 0 && isMakeHandshakeOnStart()) {
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

	/**
	 * Called if the activity could not establish connection and receive required data in
	 * given time.
	 */
	public void onCommunicationFailed() {
		((MainApplication) getApplication()).doApplicationFail(AppFailType.CONNECTION_FAILED);
	}

	protected void cancelConnectionFailedTimer() {
		if (mConnectionFailedTimer != null) {
			mConnectionFailedTimer.cancel();
			mConnectionFailedTimer = null;   // and canceling and nulling timer
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
		AppPreferencesManager.persistLastActivity(this, getClass());

		super.onStart();

		mDrawer = findViewById(R.id.navigation_drawer);
		mDrawerCloseArrowImg = findViewById(R.id.imageViewDrawerOpened);
		// checks connection and state of initial command, if not ready, initiates countDownTimer
		if (!onConnectionFailedTimerTick()) {
			ticks = 0;
			mConnectionFailedTimer = new CountDownTimer(HANDSHAKE_TIMEOUT_MS, HANDSHAKE_TICK_MS) {
				@Override
				public void onTick(long l) {
					ticks++;
					onConnectionFailedTimerTick();
				}

				@Override
				public void onFinish() {
					Logger.logE(LocusWearActivity.this.getClass().getSimpleName(), "Connection Failed!");
					cancelConnectionFailedTimer();
					onCommunicationFailed();
				}
			};
			mConnectionFailedTimer.start();
		}
	}

	// current activity state
	public enum WearActivityState {
		ON_CREATE,
		ON_START,
		ON_RESUME,
		ON_PAUSE,
		ON_STOP,
		ON_DESTROY;

	}

	/**
	 * Handling of item click in main menu/navigation drawer
	 *
	 * @param v
	 */
	public void handleNavigationDrawerItemClicked(View v) {
		final Class<? extends WearableActivity> activityToStart;
		switch (v.getId()) {
			case R.id.navigation_drawer_item_map:
				activityToStart = MapActivity.class;
				break;
			case R.id.navigation_drawer_item_track_rec:
				activityToStart = TrackRecordActivity.class;
				break;
			default:
				activityToStart = null;
				break;
		}
		((MainApplication) getApplication()).startLocusWearActivity(activityToStart);
		if (mDrawer != null) {
			mDrawer.getController().closeDrawer();
		}
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
}
