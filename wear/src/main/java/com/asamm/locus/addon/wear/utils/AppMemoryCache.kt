/**
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.utils

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue
import com.asamm.locus.addon.wear.features.settings.PreferencesEx.persistLastRecState

/**
 * Container class serves as a memory cache to keep some received data while the app is running.
 */
class AppMemoryCache internal constructor(c: Context) {

    private val screenDimension = Point()

    private var mapContainer: MapContainer? = null

    val screenWidth: Int
        get() = screenDimension.x

    val screenHeight: Int
        get() = screenDimension.y

    fun setLastTrackRecState(value: TrackRecordingValue?) {
        persistLastRecState(value)
    }

    var lastMapData: MapContainer?
        get() = mapContainer
        set(mapContainer) {
            if (mapContainer != null) {
                this.mapContainer = mapContainer
            }
        }

    /**
     * Container for recording profiles.
     */
    var profiles: List<TrackProfileInfoValue> = emptyList()

    init {
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        display.getSize(screenDimension)
    }
}