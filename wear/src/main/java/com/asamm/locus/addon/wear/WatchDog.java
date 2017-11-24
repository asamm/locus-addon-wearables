package com.asamm.locus.addon.wear;

import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Watchdog class for automated keep alive data request transmission
 * and for monitoring communication of last registered activity.
 *
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public class WatchDog {

	private static final int WD_PERIOD_MS = 1000;
	private static final int WD_START_DELAY_MS = 1000;
	public static final int WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS = 4500;

	private volatile HashMap<String, ArrayList<Watched>> watchedActivities;
	private volatile Timer mWdTimer;
	private volatile long mLastKeepAliveSentTimestamp = -1;


	WatchDog() {
		watchedActivities = new HashMap<>(5);
		startWdTimer();
	}

	WdRequestable prepareWatchForActivity(Class<LocusWearActivity> activity) {
		Watched w = new Watched(this);
		w.setActivity(activity);
		return w;
	}

	void destroy() {
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

	private void doTimerTick() {
		// handle keep alive transmission first
		long now = System.currentTimeMillis();
		if (now - mLastKeepAliveSentTimestamp >= WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS) {
			WearCommService.getInstance().sendCommand(DataPath.GET_KEEP_ALIVE);
			mLastKeepAliveSentTimestamp = now;
		}

		// now handle communication watch for activities
	}

	public void startWatching(Watched w) {
		throw new UnsupportedOperationException("Not yet supported"); // TODO cejnar
	}

	public interface WdStartable {
		void startWatchDog();
	}

	public interface WdExpectable {
		WdStartable expects(DataPath response);
	}

	public interface WdRequestable {
		WdExpectable requests(DataPayload<TimeStampStorable> requestCommand);
	}

	private static class Watched implements WdRequestable, WdExpectable, WdStartable {
		private final WatchDog mWatchDog;
		private Class<LocusWearActivity> mActivity;
		private DataPayload<TimeStampStorable> mRequest;
		private DataPath mExpected;

		private Watched(WatchDog watchDog) {
			this.mWatchDog = watchDog;
		}

		public void setActivity(Class<LocusWearActivity> activity) {
			mActivity = activity;
		}

		@Override
		public void startWatchDog() {
			mWatchDog.startWatching(this);
		}

		@Override
		public WdStartable expects(DataPath response) {
			this.mExpected = response;
			return this;
		}

		@Override
		public WdExpectable requests(DataPayload<TimeStampStorable> requestCommand) {
			this.mRequest = requestCommand;
			return this;
		}
	}

}
