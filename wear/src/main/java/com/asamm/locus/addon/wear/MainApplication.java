package com.asamm.locus.addon.wear;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Switch;

import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;
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

    private LocusWearActivity mCurrentActivity;

    private ApplicationState mState;

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
        mState = new ApplicationState();
        reconnectIfNeeded();
        registerActivityLifecycleCallbacks(this);
    }

    /**
     * Destroy instance of this application.
     */
    public void onDestroy() {
        Logger.logE(TAG, "onDestroy()");
        // destroy instance of communication class
        DeviceCommunicationOld.destroyInstance();
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
            if (p != null) {
                TimeStampStorable value = p.createStorableForPath(dataItem);
                switch (p) {
                    case PUT_HAND_SHAKE:
                        mState.setHandShakeValue(value.asHandShakeValue());
                        break;
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
    }

    public void onConnectionSuspened() {
        mState.setConnected(false);
        reconnectIfNeeded();
    }
}
