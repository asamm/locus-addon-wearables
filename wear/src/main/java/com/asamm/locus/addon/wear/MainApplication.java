package com.asamm.locus.addon.wear;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Toast;

import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.google.android.gms.wearable.DataItem;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainApplication extends Application implements Application.ActivityLifecycleCallbacks {

	private volatile LocusWearActivity mCurrentActivity;

	private volatile ApplicationState mState;

	// timer for termination
	private static Timer mTimerTerminate;

	// tag for logger
	private static final String TAG = MainApplication.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();

		// set logger
		Logger.registerLogger(new Logger.ILogger() {
			@Override
			public void logI(String tag, String msg) {
				Log.i(tag, msg);
			}

			@Override
			public void logD(String tag, String msg) {
				Log.d(tag, msg);
			}

			@Override
			public void logW(String tag, String msg) {
				Log.w(tag, msg);
			}

			@Override
			public void logE(String tag, String msg) {
				Log.e(tag, msg);
			}

			@Override
			public void logE(String tag, String msg, Exception e) {
				Log.e(tag, msg, e);
			}
		});

		// notify about create of app
		Logger.logE(TAG, "onCreate()");
		mState = new ApplicationState(this);
		reconnectIfNeeded();
		registerActivityLifecycleCallbacks(this);
	}

	/**
	 * Destroy instance of this application.
	 */
	public void onDestroy() {
		Logger.logE(TAG, "destroyInstance()");
		// destroy instance of communication class
		WearCommService.destroyInstance();
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle bundle) {
		Logger.logD(TAG, "Activity created");
		reconnectIfNeeded();
	}

	@Override
	public void onActivityStarted(Activity activity) {
		reconnectIfNeeded();
	}

	@Override
	public void onActivityResumed(Activity activity) {
		// set current activity
		LocusWearActivity oldAct = mCurrentActivity;
		if (oldAct == null || oldAct == activity) {
			// just set current activity, for sure
			setCurrentActivity(activity);
		} else {
			// check state of old custom activity
			switch (oldAct.getState()) {
				case ON_START:
				case ON_PAUSE:
				case ON_STOP:
					setCurrentActivity(activity);
			}
		}
	}

	@Override
	public void onActivityPaused(Activity activity) {

	}

	@Override
	public void onActivityStopped(Activity activity) {
		// activity is not visible
		if (mCurrentActivity == activity) {
			setCurrentActivity(null);
		}
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

	}

	@Override
	public void onActivityDestroyed(Activity activity) {

	}

	public void handleDataEvent(DataItem dataItem) {
		final LocusWearActivity currentActivity = mCurrentActivity;
		if (currentActivity != null) {
			DataPath p = DataPath.valueOf(dataItem);
			Logger.logD(TAG, "Received " + p);
			if (p != null) {
				TimeStampStorable value = WearCommService.getInstance().createStorableForPath(p, dataItem);
				switch (p) {
					case PUT_HAND_SHAKE:
						mState.setHandShakeValue((HandShakeValue) value);
						break;
					case PUT_MAP:
						mState.setLastMapData((MapContainer) value);
						break;
					case PUT_TRACK_REC:
						mState.setLastTrackRecState((TrackRecordingValue) value);
				}
				currentActivity.consumeNewData(p, value);
			} else {
				Logger.logW(TAG, "unknown DataItem path " + dataItem.getUri().getPath());
			}
		}
		Logger.logD(TAG, "Got new data change event: " + dataItem.getUri().getPath());
	}

	/**
	 * Set reference to current activity.
	 *
	 * @param activity current activity
	 */
	private void setCurrentActivity(Activity activity) {
		LocusWearActivity act = (LocusWearActivity) activity;
		Logger.logD(TAG, "setCurrentActivity(" + act + ")");

		// if new activity is registered, end timer
		if (act != null && mTimerTerminate != null) {
			mTimerTerminate.cancel();
			mTimerTerminate = null;
		}
		// register activity
		if (mCurrentActivity == null && act != null) {
			Logger.logW(TAG, " - application restored");
		} else if (mCurrentActivity != null && act == null) {
			Logger.logW(TAG, " - application terminated");

			// start timer
			TimerTask terminateTask = new TimerTask() {
				@Override
				public void run() {
					MainApplication.this.onDestroy();
				}
			};

			// execute timer
			mTimerTerminate = new Timer();
			mTimerTerminate.schedule(terminateTask,
					TimeUnit.SECONDS.toMillis(10));
		}
		mCurrentActivity = act;
	}

	private void reconnectIfNeeded() {
		WearCommService s = WearCommService.getInstance();
		if (s == null) {
			WearCommService.initialize(this);
		} else {
			s.reconnectIfNeeded();
		}
	}

	public ApplicationState getState() {
		return mState;
	}

	public void onConnected() {
		mState.setConnected(true);
		LocusWearActivity act = mCurrentActivity;
		if (act != null) {
			act.consumeNewData(DataPath.PUT_ON_CONNECTED_EVENT, null);
		}
	}

	public void onConnectionSuspened() {
		mState.setConnected(false);
		reconnectIfNeeded();
	}

	public void startLocusWearActivity(Class<? extends WearableActivity> activityToStart) {
		if (activityToStart == null ||
				(mCurrentActivity != null && mCurrentActivity.getClass().getSimpleName().equals(activityToStart.getSimpleName()))) {
			return;
		}
		Intent i = new Intent(this, activityToStart);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

	public static void showToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}
}
