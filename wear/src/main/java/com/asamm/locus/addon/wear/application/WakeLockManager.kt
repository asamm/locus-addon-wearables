package com.asamm.locus.addon.wear.application

import android.content.Context
import android.os.PowerManager
import locus.api.utils.Logger
import java.lang.Exception

/**
 * Base constructor.
 */
public class WakeLockManager(val tag: String) {

    // wake-lock itself
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Acquire wake lock right now.
     */
    fun acquireWakeLock(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            // prepare wakeLock
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
                if (wakeLock == null) {
                    Logger.logE(tag,"Could not create wake lock (null).")
                    return
                }
            }

            // acquire wakelock
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire()
                    if (!it.isHeld) {
                        Logger.logE(tag,"Could not acquire wake lock.")
                    }
                }
            }
        } catch (e: RuntimeException) {
            Logger.logE(tag, "acquireWakeLock()", e)
        }
    }

    /**
     * Releases the wake lock if it's currently held.
     */
    fun releaseWakeLock() {
        // release wakeLock
        wakeLock?.takeIf { it.isHeld }?.apply {
            try {
                release()
            } catch (e: Exception) {
                Logger.logE(tag, "releaseWakeLock()", e)
            }
        }

        // clear reference
        wakeLock = null
    }
}
