package com.asamm.locus.addon.wear.utils

import android.content.Context
import android.os.PowerManager
import com.asamm.logger.Logger
import java.lang.Exception

/**
 * Base constructor.
 */
class WakeLockManager(val tag: String) {

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
                    Logger.e(null, tag,"Could not create wake lock (null).")
                    return
                }
            }

            // acquire wakelock
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire()
                    if (!it.isHeld) {
                        Logger.e(null, tag,"Could not acquire wake lock.")
                    }
                }
            }
        } catch (e: RuntimeException) {
            Logger.e(e, tag, "acquireWakeLock()")
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
                Logger.e(e, tag, "releaseWakeLock()")
            }
        }

        // clear reference
        wakeLock = null
    }
}
