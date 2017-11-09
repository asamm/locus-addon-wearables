package com.asamm.locus.addon.wear;

import android.app.Application;
import android.util.Log;

import com.asamm.locus.addon.wear.gui.CustomActivityOld;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Created by menion on 10. 7. 2014.
 * Class is part of Locus project
 */
public class MainApplicationOld extends Application {

    // tag for logger
    private static final String TAG = "MainApplicationOld";
    

    /**************************************************/
    // STATE OF CURRENT ACTIVITY
    /**************************************************/

    /**
     * Called when activity move to "onStart" state.
     * @param act current activity
     */
    public static void activityOnStart(CustomActivityOld act) {
    }

    public static void activityOnResume(CustomActivityOld act) {
    }
    public static void activityOnStop(CustomActivityOld act) {
    }

    public static CustomActivityOld getCurrentActivity() {
    return null;
    }

    public static boolean existCurrentActivity() {
        return false;
    }

    /**************************************************/
    // APPLICATION REFERENCE
    /**************************************************/

}
