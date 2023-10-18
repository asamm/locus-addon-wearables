/**
 * Created by menion on 10. 7. 2014.
 * Class is part of Locus project
 */
package com.asamm.locus.addon.wear

import android.app.Application
import android.util.Log
import com.asamm.logger.Logger

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // set logger
        Logger.registerLogger(object : Logger.ILogger {

            override fun logD(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.d(tag, msg)
            }

            override fun logI(tag: String, msg: String, vararg args: Any) {
                Log.i(tag, msg)
            }

            override fun logV(tag: String, msg: String, vararg args: Any) {
                Log.i(tag, msg)
            }

            override fun logW(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.w(tag, msg)
            }

            override fun logE(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.e(tag, msg, ex)
            }
        })
    }
}