package com.asamm.locus.addon.wear;

import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.communication.AppFailCallback;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.features.error.AppFailType;
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordingService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.asamm.logger.Logger;

/**
 * Watchdog class for automated keep alive data request transmission
 * and for monitoring communication of last registered activity.
 * <p>
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public class WatchDog {

    public static final Object WATCHDOG_LOCK = new Object();
    private static volatile WatchDog INSTANCE;
    private static final String TAG = "WatchDog";

    private static final int WD_PERIOD_MS = 1000;
    private static final int WD_START_DELAY_MS = 1000;
    public static final int WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS = 4500;

    private volatile HashMap<String, List<Watched>> watchedActivities;
    private volatile Timer wdTimer;
    private volatile long lastKeepAliveSentTimestamp = -1;
    private Class<? extends LocusWearActivity> currentActivityClass;
    private AppFailCallback appFailCallback;

    public static WatchDog getInstance() {
        if (INSTANCE == null) {
            synchronized (WATCHDOG_LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new WatchDog();
                }
            }
        }
        return INSTANCE;
    }

    private WatchDog() {
        appFailCallback = null;
        watchedActivities = new HashMap<>(5);
        startWdTimer();
        Logger.w(TAG, "Starting watchdog");
    }

    /**
     * Destroy watchDog and all referenced services.
     */
    void destroy() {
        Logger.w(TAG, "Terminating watchdog");
        appFailCallback = null;
        synchronized (WATCHDOG_LOCK) {
            if (wdTimer != null) {
                if (wdTimer != null) {
                    wdTimer.cancel();
                    wdTimer = null;
                }
            }
            WearCommService.destroyInstance();
            INSTANCE = null;
        }
    }

    private void startWdTimer() {
        if (wdTimer != null) {
            return;
        }

        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                doTimerTick();
            }
        };
        synchronized (WATCHDOG_LOCK) {
            wdTimer = new Timer();
            lastKeepAliveSentTimestamp = System.currentTimeMillis() + WD_START_DELAY_MS;
            wdTimer.schedule(t, WD_START_DELAY_MS, WD_PERIOD_MS);
        }
    }

    /**
     * Should be called after new activity is opened
     */
    void onCurrentActivityChanged(Class<? extends LocusWearActivity> previous,
            Class<? extends LocusWearActivity> current) {
        // transition in current activity - clear previous and new activity watchdogs
        if (previous != null) {
            synchronized (WATCHDOG_LOCK) {
                List<Watched> watched = watchedActivities.get(previous.getSimpleName());
                if (watched != null) {
                    watched.clear();
                }
            }
        }
        if (current != null) {
            synchronized (WATCHDOG_LOCK) {
                List<Watched> watched = watchedActivities.get(current.getSimpleName());
                if (watched != null) {
                    watched.clear();
                }
            }
            currentActivityClass = current;
        }
    }

    public void setAppFailCallback(AppFailCallback mAppFailCallback) {
        this.appFailCallback = mAppFailCallback;
    }

    /**
     * Handles tick of WD timer
     */
    private void doTimerTick() {
//        Logger.INSTANCE.logD(TAG, "doTimerTick(), " +
//                "app: " + MainApplication.app + ", " +
//                "trackRec: " + TrackRecordingService.isRunning());
        // seems both trackRec service and main application were killed. Destroy watchdog immediately
        if (MainApplication.app == null && !TrackRecordingService.isRunning()) {
            Logger.w(TAG, "Application termination detected");
            destroy();
            return;
        }

        // handle keep alive transmission first
        long now = System.currentTimeMillis();
        if (now - lastKeepAliveSentTimestamp >= WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS) {
            WearCommService.getInstance().sendCommand(DataPath.TD_KEEP_ALIVE);
            lastKeepAliveSentTimestamp = now;
        }

        // now handle communication watch for activities

        boolean failed = false;
        List<DataPayload> reqsToResend = null;

        synchronized (WATCHDOG_LOCK) {
            if (currentActivityClass == null) {
                return;
            }
            List<Watched> watched = watchedActivities.get(currentActivityClass.getSimpleName());
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
                        reqsToResend = new ArrayList<>(3);
                    }
                    reqsToResend.add(w.mRequest);
                    w.incrementRetryAttempts();
                }
            }
        }
        AppFailCallback onFail = appFailCallback;
        if (failed && onFail != null) {
            Logger.d(TAG, "Watchdog fail event");
            onFail.onAppFail(AppFailType.CONNECTION_FAILED);
        } else if (reqsToResend != null) {
            for (DataPayload p : reqsToResend) {
                Logger.d(TAG, "Watchdog retry request " + p.getPath());
                WearCommService.getInstance().sendDataItem(p.getPath(), p.getStorable());
            }
        }
    }

    /**
     * Should be called immediately after receiving new data from comms
     */
    public void onNewData(DataPath receivedPath, TimeStampStorable value) {
        synchronized (WATCHDOG_LOCK) {
            if (currentActivityClass == null) {
                return;
            }
            List<Watched> watched = watchedActivities.get(currentActivityClass.getSimpleName());
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
        synchronized (WATCHDOG_LOCK) {
            List<Watched> list = watchedActivities.get(className);
            if (list == null) {
                list = new LinkedList<>();
                watchedActivities.put(className, list);
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
