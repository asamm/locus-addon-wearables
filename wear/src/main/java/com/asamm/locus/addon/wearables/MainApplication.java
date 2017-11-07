package com.asamm.locus.addon.wearables;

import android.app.Application;
import android.util.Log;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainApplication extends Application {

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
}
