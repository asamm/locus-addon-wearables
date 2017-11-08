package com.asamm.locus.addon.wear;

import android.app.Application;
import android.util.Log;

import locus.api.utils.Logger;

/**
 * Created by menion on 10. 7. 2014.
 * Class is part of Locus project
 */
public class MainApplication extends Application {

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
    }
}
