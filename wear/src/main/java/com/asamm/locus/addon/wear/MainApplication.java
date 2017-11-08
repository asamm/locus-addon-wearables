package com.asamm.locus.addon.wear;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.wearable.DataItem;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainApplication extends Application implements Application.ActivityLifecycleCallbacks {

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
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Logger.logD(TAG, "Activity created");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!DeviceCommunicationService.isInitialized()) {
            DeviceCommunicationService s = DeviceCommunicationService.initialize(this);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public void handleDataEvent(DataItem dataItem) {

        Logger.logD(TAG, "Got new data change event: " + dataItem.getUri().getPath());
    }
}
