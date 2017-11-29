package com.asamm.locus.addon.wear;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Toast;

import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.MapActivity;
import com.asamm.locus.addon.wear.gui.error.ActivityFail;
import com.asamm.locus.addon.wear.gui.error.AppFailType;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;
import com.google.android.gms.wearable.DataItem;

import java.util.List;
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

	private volatile ApplicationMemoryCache mCache;

	// timer for termination
	private static Timer mTimerTerminate;

	private WatchDog mWatchDog;

	// tag for logger
	private static final String TAG = MainApplication.class.getSimpleName();

	@Override
	public void onCreate() {
		// TODO cejnar debug only
		AppPreferencesManager.debugClear(this);
		AppStorageManager.trimCache(this);

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
		registerActivityLifecycleCallbacks(this);
		setTerminationTimer();
		mCache = new ApplicationMemoryCache(this);
		reconnectIfNeeded();
	}

	/**
	 * Destroy instance of this application.
	 */
	public void onDestroy() {
		Logger.logE(TAG, "destroyInstance()");
		// destroy instance of communication class

		mWatchDog.destroy();
		synchronized (this) {
			if (mWatchDog != null) {
				mWatchDog.destroy();
			}
			mWatchDog = null;
		}
		WearCommService.destroyInstance();
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle bundle) {
		Logger.logD(TAG, "Activity created");
		reconnectIfNeeded();
	}

	@Override
	public void onActivityStarted(Activity activity) {
		if (mWatchDog == null) {
			synchronized (this) {
				if (mWatchDog == null) {
					mWatchDog = new WatchDog();
				}
			}
		}
		reconnectIfNeeded();
	}

	@Override
	public void onActivityResumed(Activity activity) {
		if (!(activity instanceof LocusWearActivity)) {
			activity = null; // Error activity or other non standard activity - treat as null
		}
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
		// no activity is visible any more or currently shown activity does not consume periodic data
		if (mCurrentActivity == null || !mCurrentActivity.isUsePeriodicData()) {
			WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA,
					PeriodicCommand.createStopPeriodicUpdatesCommand());
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
						final HandShakeValue handShakeValue = (HandShakeValue) value;
						if (!validateHandShakeOrFail(handShakeValue)) {
							return;
						}
						break;
					case PUT_MAP:
						mCache.setLastMapData((MapContainer) value);
						break;
					case PUT_TRACK_REC:
						mCache.setLastTrackRecState(this, (TrackRecordingValue) value);
						break;
					case PUT_TRACK_REC_PROFILE_INFO: {
						TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) value;
						if (profiles != null) {
							mCache.setProfiles(profiles.getStorables());
						}
					}
					case PUT_PROFILE_ICON: {
						if (value instanceof TrackProfileIconValue) {
							AppStorageManager.persistIcon(this, (TrackProfileIconValue) value);
						}
						List<TrackProfileInfoValue> profiles = mCache.getProfiles();
						for (TrackProfileInfoValue info : profiles) {
							if (!AppStorageManager.isIconCached(this, info.getId())) {
								WearCommService.getInstance().sendDataItem(DataPath.GET_PROFILE_ICON, new ProfileIconGetCommand(info.getId()));
								break;
							}
						}
					}
					break;
					default:
						break;
				}
				currentActivity.consumeNewData(p, value);
			} else {
				Logger.logW(TAG, "unknown DataItem path " + dataItem.getUri().getPath());
			}
		}
		Logger.logD(TAG, "Got new data change event: " + dataItem.getUri().getPath());
	}

	private LocusWearActivity getCurrentActivity() {
		return mCurrentActivity;
	}

	private boolean validateHandShakeOrFail(HandShakeValue handShakeValue) {
		if (handShakeValue == null || handShakeValue.isEmpty()) {
			WearCommService.getInstance().sendCommand(DataPath.GET_HAND_SHAKE);
			return false;
		}
		if (handShakeValue.getmLocusVersion() < Const.LOCUS_MIN_VERSION_CODE.vcFree) {
			doApplicationFail(AppFailType.UNSUPPORTED_LOCUS_VERSION);
			return false;
		}

		if (!handShakeValue.isPeriodicUpdates()) {
			doApplicationFail(AppFailType.PERIODIC_UPDATES_DISABLED);
			return false;
		}
		return true;
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
			setTerminationTimer();
		}
		mCurrentActivity = act;
	}

	private void setTerminationTimer() {
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

	private void reconnectIfNeeded() {
		WearCommService s = WearCommService.getInstance();
		if (s == null) {
			WearCommService.initialize(this);
		} else {
			s.reconnectIfNeeded();
		}
	}

	public ApplicationMemoryCache getCache() {
		return mCache;
	}

	public void onConnected() {
		LocusWearActivity act = mCurrentActivity;
		if (act != null) {
			act.consumeNewData(DataPath.PUT_ON_CONNECTED_EVENT, null);
		}
	}

	public void onConnectionSuspened() {
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

	public void retryLastTask() {
		String activityName = AppPreferencesManager.getLastActivity(this);
		Class<? extends LocusWearActivity> c = TrackRecordActivity.class;
		if (MapActivity.class.getSimpleName().equals(activityName)) {
			c = MapActivity.class;
		}
		startLocusWearActivity(c);
	}

	public void doApplicationFail(AppFailType reason) {
		Intent i = new Intent(this, ActivityFail.class);
		i.putExtra(ActivityFail.ARG_ERROR_TYPE, reason.name());
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}
}
