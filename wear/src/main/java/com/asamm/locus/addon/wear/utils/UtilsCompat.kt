package com.asamm.locus.addon.wear.utils

import android.os.Build

/**
 * Helper methods for Android compatibility with older versions & its testing.
 */
object UtilsCompat {

    /**
     * Check if current device is Android 11.0+.
     */
    val isAPI30orMore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}