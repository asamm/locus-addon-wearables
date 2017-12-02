package com.asamm.locus.addon.wear;

import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.communication.AppFailCallback;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.error.AppFailType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import locus.api.utils.Logger;

/**
 * Watchdog class for automated keep alive data request transmission
 * and for monitoring communication of last registered activity.
 * <p>
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public class WatchDog {

	private static final String TAG = "WatchDog";

	private static final int WD_PERIOD_MS = 1000;
	private static final int WD_START_DELAY_MS = 1000;
	public static final int WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS = 4500;

	private volatile HashMap<String, List<Watched>> mWatchedActivities;
	private volatile Timer mWdTimer;
	private volatile long mLastKeepAliveSentTimestamp = -1;
	private Class<? extends LocusWearActivity> mCurrentActivityClass;
	private AppFailCallback mAppFailCallback;

	WatchDog(AppFailCallback failCallback) {
		mAppFailCallback = failCallback;
		mWatchedActivities = new HashMap<>(5);
		startWdTimer();
	}

	void destroy() {
		mAppFailCallback = null;
		if (mWdTimer != null) {
			synchronized (this) {
				if (mWdTimer != null) {
					mWdTimer.cancel();
					mWdTimer = null;
				}
			}
		}
	}

	private void startWdTimer() {
		if (mWdTimer != null)
			return;
		TimerTask t = new TimerTask() {
			@Override
			public void run() {
				doTimerTick();
			}
		};
		synchronized (this) {
			mWdTimer = new Timer();
			mLastKeepAliveSentTimestamp = System.currentTimeMillis() + WD_START_DELAY_MS;
			mWdTimer.schedule(t, WD_START_DELAY_MS, WD_PERIOD_MS);
		}
	}

	/**
	 * Should be called after new activity is opened
	 *
	 * @param previous
	 * @param current
	 */
	void onCurrentActivityChanged(Class<? extends LocusWearActivity> previous,
								  Class<? extends LocusWearActivity> current) {
		// transition in current activity - clear previous and new activity watchdogs
		if (previous != null) {
			synchronized (mWatchedActivities) {
				List<Watched> watched = mWatchedActivities.get(previous.getSimpleName());
				if (watched != null) {
					watched.clear();
				}
			}
		}
		if (current != null) {
			synchronized (mWatchedActivities) {
				List<Watched> watched = mWatchedActivities.get(current.getSimpleName());
				if (watched != null) {
					watched.clear();
				}
			}
			mCurrentActivityClass = current;
		}
	}

	/**
	 * Handles tick of WD timer
	 */
	private void doTimerTick() {
		// handle keep alive transmission first
		long now = System.currentTimeMillis();
		if (now - mLastKeepAliveSentTimestamp >= WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS) {
			WearCommService.getInstance().sendCommand(DataPath.GET_KEEP_ALIVE);
			mLastKeepAliveSentTimestamp = now;
		}

		// now handle communication watch for activities

		boolean failed = false;
		List<DataPayload> reqsToResend = null;

		synchronized (mWatchedActivities) {
			if (mCurrentActivityClass == null) {
				return;
			}
			List<Watched> watched = mWatchedActivities.get(mCurrentActivityClass.getSimpleName());
			if (watched == null) {
				return;
			}
			Iterator<Watched> it = watched.iterator();
			while (it.hasNext()) {
				Watched w = it.next();
				w.addTimeoutMs(WD_PERIOD_MS);
				if (w.isTimedOut()) {
					failed = true;
					break;
				} else if (w.isRequestResendConditionMet()) {
					if (reqsToResend == null) {
						reqsToResend = new ArrayList<DataPayload>(3);
					}
					reqsToResend.add(w.mRequest);
					w.incrementRetryAttempts();
				}
			}
		}
		AppFailCallback onFail = mAppFailCallback;
		if (failed && onFail != null) {
			Logger.logD(TAG, "Watchdog fail event");
			onFail.onAppFail(AppFailType.CONNECTION_FAILED);
		} else if (reqsToResend != null) {
			for (DataPayload p : reqsToResend) {
				Logger.logD(TAG, "Watchdog retry request " + p);
				WearCommService.getInstance().sendDataItem(p.getPath(), p.getStorable());
			}
		}
	}

	/**
	 * Should be called immediately after receiving new data from comms
	 *
	 * @param receivedPath
	 */
	void onNewData(DataPath receivedPath, TimeStampStorable value) {
		synchronized (mWatchedActivities) {
			if (mCurrentActivityClass == null) {
				return;
			}
			List<Watched> watched = mWatchedActivities.get(mCurrentActivityClass.getSimpleName());
			if (watched == null) {
				return;
			}
			Iterator<Watched> it = watched.iterator();
			while (it.hasNext()) {
				Watched w = it.next();
				if (w.mExpected == receivedPath) {
					if (w.mResponsePredicate == null || w.mResponsePredicate.test(value)) {
						it.remove();
						return;
					}
				}
			}
		}
	}

	/**
	 * Adds watch for specified Activity requests
	 *
	 * @param clazz         type of activity
	 * @param request       exact payload which has been sent and should trigger expected data,
	 *                      may be resend after some time by watch dog
	 * @param expected      expected incoming (answer) data
	 * @param timeOutToFail timeout in ms after which application fails if expected response is not received
	 */
	public void startWatching(Class<? extends LocusWearActivity> clazz,
							  DataPayload<? extends TimeStampStorable> request, DataPath expected, long timeOutToFail) {
		startWatchingWithCondition(clazz, request, expected, timeOutToFail, null);
	}

	/**
	 * Adds watch for specified Activity requests. Expected response data must also satisfy given
	 * predicate condition to be accepted.
	 *
	 * @param clazz             type of activity
	 * @param request           exact payload which has been sent and should trigger expected data,
	 *                          may be resend after some time by watch dog
	 * @param expected          expected incoming (answer) data
	 * @param timeOutToFail     timeout in ms after which application fails if expected response is not received
	 * @param responsePredicate additional condition on expected data to be accepted
	 */
	public void startWatchingWithCondition(Class<? extends LocusWearActivity> clazz,
										   DataPayload<? extends TimeStampStorable> request, DataPath expected,
										   long timeOutToFail, WatchDogPredicate responsePredicate) {
		final Watched newWatched = new Watched(clazz, request, expected, timeOutToFail, responsePredicate);
		final String className = clazz.getSimpleName();
		synchronized (mWatchedActivities) {
			List<Watched> list = mWatchedActivities.get(className);
			if (list == null) {
				list = new LinkedList<>();
				mWatchedActivities.put(className, list);
			}
			Iterator<Watched> it = list.iterator();
			// check if list already contains similar watchable
			while (it.hasNext()) {
				Watched w = it.next();
				if (!w.equals(newWatched)) {
					continue;
				}
				// If I got new watchable with predicate and old similar watcheble
				// doesnt have predicate, then prefer new watch condition with predicate
				// as it is more specific.
				if (w.mResponsePredicate == null && newWatched.mResponsePredicate != null) {
					it.remove();
					break;
				} else {
					return; // this kind of response is already expected, return
				}
			}
			list.add(newWatched);

		}
	}

	/**
	 * Container representing comm transmission that is being watched for given activity
	 */
	private static class Watched {
		private final Class<? extends LocusWearActivity> mActivity;
		private final DataPayload<? extends TimeStampStorable> mRequest;
		private final DataPath mExpected;
		private final long mTimeoutToFail;
		private long mCurrentTimeout;
		private byte mRetryAttempts;
		private WatchDogPredicate<TimeStampStorable> mResponsePredicate;

		private Watched(Class<? extends LocusWearActivity> clazz,
						DataPayload<? extends TimeStampStorable> request, DataPath expectedResponse,
						long timeoutToFail, WatchDogPredicate<TimeStampStorable> responsePredicate) {
			this.mActivity = clazz;
			this.mRequest = request;
			this.mExpected = expectedResponse;
			this.mTimeoutToFail = timeoutToFail;
			mCurrentTimeout = 0;
			mRetryAttempts = 0;
			this.mResponsePredicate = responsePredicate;
		}

		private void addTimeoutMs(long timeToAddMs) {
			mCurrentTimeout += timeToAddMs;
		}

		private void incrementRetryAttempts() {
			mRetryAttempts++;
		}

		private boolean isTimedOut() {
			return mCurrentTimeout >= mTimeoutToFail;
		}

		private boolean isRequestResendConditionMet() {
			return mRetryAttempts == 0 && ((mTimeoutToFail >>> 1) <= mCurrentTimeout);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Watched watched = (Watched) o;

			return mExpected == watched.mExpected;
		}

		@Override
		public int hashCode() {
			return mExpected.hashCode();
		}
	}
}
