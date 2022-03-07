/**
 * Created by menion on 10. 7. 2014.
 * Class is part of Locus project
 */
package com.asamm.locus.addon.wear

import android.app.Application
import android.util.Log
import locus.api.utils.Logger
import locus.api.utils.Logger.registerLogger

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // set logger
        registerLogger(object : Logger.ILogger {

            override fun logI(tag: String, msg: String) {
                Log.i(tag, msg)
            }

            override fun logD(tag: String, msg: String) {
                Log.d(tag, msg)
            }

            override fun logW(tag: String, msg: String) {
                Log.w(tag, msg)
            }

            override fun logE(tag: String, msg: String) {
                Log.e(tag, msg)
            }

            override fun logE(tag: String, msg: String, e: Exception) {
                Log.e(tag, msg, e)
            }
        })
    }
}