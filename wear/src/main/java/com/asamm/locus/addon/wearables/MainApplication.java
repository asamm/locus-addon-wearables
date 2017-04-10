package com.asamm.locus.addon.wearables;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.asamm.locus.addon.wearables.gui.CustomActivity;
import com.asamm.locus.addon.wearables.gui.MainMenuActivity;
import com.asamm.locus.addon.wearables.gui.MapActivity;
import com.asamm.locus.addon.wearables.gui.TrackRecordActivity;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Created by menion on 10. 7. 2014.
 * Class is part of Locus project
 */
public class MainApplication extends Application {

    // tag for logger
    private static final String TAG = "MainApplication";
    private static CustomActivity m_LastActivity;

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
    }

    /**
     * Destroy instance of this application.
     */
    public void onDestroy() {
        Logger.logE(TAG, "onDestroy()");
        // destroy instance of communication class
        DeviceCommunication.destroyInstance();
    }

    /**************************************************/
    // STATE OF CURRENT ACTIVITY
    /**************************************************/

    /**
     * Called when activity move to "onCreate" state.
     * @param act current activity
     */
    public static void activityOnCreate(CustomActivity act) {
        initializeDeviceCommunication(act);
    }

    /**
     * Called when activity move to "onStart" state.
     * @param act current activity
     */
    public static void activityOnStart(CustomActivity act) {
        initializeDeviceCommunication(act);

        // register activity
        DeviceCommunication.getInstance().checkConnection(act);
    }


    public static void activityOnDestroyed( Activity act) {
        m_LastActivity = null;
    }
    /**
     * Called when activity move to "onResume" state.
     * @param act current activity
     */
    public static void activityOnResume(CustomActivity act) {
        // set current activity
        Log.d(TAG, "activityOnResume old " + getCurrentActivity());
        Log.d(TAG, "activityOnResume new " + act);
        Log.d(TAG, "activityOnResume last " + m_LastActivity);
        if ( !(act instanceof MainMenuActivity) )
            m_LastActivity = act;
        CustomActivity oldAct = getCurrentActivity();
        if (oldAct == null || oldAct == act) {
            // just set current activity, for sure
            setCurrentActivity(act);
            Log.d(TAG, "activityOnResume: 1");
            if ( m_LastActivity instanceof MapActivity || m_LastActivity instanceof TrackRecordActivity )
            {
                Log.d(TAG, "activityOnResume: 2");
                Intent intent;
                if ( m_LastActivity instanceof TrackRecordActivity)
                  intent = new Intent(act, TrackRecordActivity.class);
                else
                  intent = new Intent(act, MapActivity.class);

                act.startActivity(intent);
            }

        } else {
            // check state of old custom activity
            if (oldAct.getCurrentState() == CustomActivity.State.ON_START ||
                    oldAct.getCurrentState() == CustomActivity.State.ON_PAUSE ||
                    oldAct.getCurrentState() == CustomActivity.State.ON_STOP) {
                setCurrentActivity(act);
                Log.d(TAG, "activityOnResume: 2");
            }
        }
    }

    /**
     * Called when activity move to "onStop" state.
     * @param act current activity
     */
    public static void activityOnStop(CustomActivity act) {
        // activity is not visible
        if (getCurrentActivity() == act) {
            setCurrentActivity(null);
        }
    }

    /**
     * Initialize instance of device communication if not yet exists.
     * @param act current activity
     */
    private static void initializeDeviceCommunication(CustomActivity act) {
        // recreate communication class
        if (DeviceCommunication.getInstance() == null) {
            // get reference to application
            final MainApplication app = (MainApplication)
                    act.getApplication();

            // create instance of "device"
            DeviceCommunication.initialize(app);
        }
    }

    /**************************************************/
    // APPLICATION REFERENCE
    /**************************************************/

    // reference to current activity
    private static CustomActivity mCurrentActivity;

    // timer for termination
    private static Timer mTimerTerminate;

    /**
     * Check if any activity is still active (registered).
     * @return <code>true</code> if any activity is still registered
     */
    public static boolean existCurrentActivity() {
        return mCurrentActivity != null;
    }

    /**
     * Get current registered activity. In case, no activity is active, return at
     * least main activity of whole application.
     * @return current activity
     */
    public static CustomActivity getCurrentActivity() {
        return mCurrentActivity;
    }

    /**
     * Set reference to current activity.
     * @param act current activity
     */
    private static void setCurrentActivity(CustomActivity act) {
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

            // get reference to application
            final MainApplication app = (MainApplication)
                    mCurrentActivity.getApplication();

            // start timer
            TimerTask terminateTask = new TimerTask() {

                @Override
                public void run() {
                    app.onDestroy();
                }
            };

            // execute timer
            mTimerTerminate = new Timer();
            mTimerTerminate.schedule(terminateTask,
                    TimeUnit.SECONDS.toMillis(10));
        }
        mCurrentActivity = act;
    }
}
