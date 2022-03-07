/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
package com.asamm.locus.addon.wear.common.communication

/**
 * Common constants to use for both device and wear modules
 */
interface Const {
    companion object {

        const val ZOOM_UNKOWN = Byte.MIN_VALUE

        const val ZOOM_MIN = 0

        const val ZOOM_MAX = 22

        const val ZOOM_DEFAULT = 15

        // minimal version code of Locus
        const val LOCUS_MIN_VERSION_CODE = 755

        const val ANDROID_MARKET_APP_URI = "market://details?id=com.asamm.locus.addon.wear"
    }
}