package com.asamm.locus.addon.wear.features.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.lifecycle.MutableLiveData
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue
import com.asamm.locus.addon.wear.features.map.MapActivity
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordActivity
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration.CfgContainer
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum
import java.io.IOException

/**
 * Helper class for maintaining shared preferences for this application
 */
object PreferencesEx {

    /**
     * Reference to preferences system.
     */
    private val prefs: SharedPreferences by lazy {
        MainApplication.app.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    }

    // PREFERENCE KEYS

    // CORE
    private const val PREF_LAST_ACTIVITY_CLASS_NAME = "PREF_LAST_ACTIVITY_CLASS_NAME"
    private const val PREF_MAP_AUTOROTATE_ENABLED = "PREF_MAP_AUTOROTATE_ENABLED"

    // MAP
    private const val PREF_MAP_BEARING = "PREF_MAP_BEARING"
    private const val PREF_MAP_OFFSET_X = "PREF_MAP_OFFSET_X"
    private const val PREF_MAP_OFFSET_Y = "PREF_MAP_OFFSET_Y"
    private const val PREF_DEVICE_ZOOM = "DEV_ZOOM"

    //*************************************************
    // PREFERENCES
    //*************************************************

    /**
     * Last visible activity.
     */
    var lastActivity: Class<out LocusWearActivity>
        get() {
            val activityName = prefs.getString(PREF_LAST_ACTIVITY_CLASS_NAME, "")
            return if (MapActivity::class.java.simpleName == activityName) {
                MapActivity::class.java
            } else {
                TrackRecordActivity::class.java
            }
        }
        set(value) {
            prefs.edit()
                .putString(PREF_LAST_ACTIVITY_CLASS_NAME, value.simpleName)
                .apply()
        }

    // MAP

    var mapAutoRotateEnabled: Boolean
        get() {
            return prefs.getBoolean(PREF_MAP_AUTOROTATE_ENABLED, false)
        }
        set(value) {
            prefs.edit()
                .putBoolean(PREF_MAP_AUTOROTATE_ENABLED, value)
                .apply()
        }

    var mapBearing: Short
        get() {
            return prefs.getInt(PREF_MAP_BEARING, 0).toShort()
        }
        set(value) {
            prefs.edit()
                .putInt(PREF_MAP_BEARING, value.toInt())
                .apply()
        }

    var mapOffsetX: Int
        get() {
            return prefs.getInt(PREF_MAP_OFFSET_X, 0)
        }
        set(value) {
            prefs.edit()
                .putInt(PREF_MAP_OFFSET_X, value)
                .apply()
        }

    var mapOffsetY: Int
        get() {
            return prefs.getInt(PREF_MAP_OFFSET_Y, 0)
        }
        set(value) {
            prefs.edit()
                .putInt(PREF_MAP_OFFSET_Y, value)
                .apply()
        }

    var mapZoom: Int
        get() {
            return prefs.getInt(PREF_DEVICE_ZOOM, Const.ZOOM_UNKOWN.toInt())
        }
        set(value) {
            prefs.edit()
                .putInt(PREF_DEVICE_ZOOM, value)
                .apply()
        }

    // TODO refactor preferences below

    private const val PREF_REC_STATE = "REC_STATE"
    private const val PREF_PROFILE_NAME = "PROFILE_NAME"
    private const val PREF_PROFILE_ID = "PROFILE_ID"
    private const val PREF_FIRST_APP_START = "PREF_FIRST_APP_START"
    private const val PREF_USE_HW_BUTTONS = "PREF_USE_HW_BUTTONS"

    //statistics screen
    private const val PREF_STAT_CONFIG = "PREF_STAT_CFG"
    private const val PREF_USE_HRM = "PREF_USE_HRM"
    private const val PREF_BOOL_IS_DEBUG = "PREF_BOOL_IS_DEBUG"

    @JvmStatic
    fun persistLastRecState(trackRec: TrackRecordingValue?) {
        if (trackRec == null || !trackRec.isInfoAvailable) {
            return
        }
        prefs.edit()
            .putString(PREF_REC_STATE, trackRec.trackRecordingState.name)
            .apply()
    }

    @JvmStatic
    fun persistLastTrackRecProfile(value: TrackProfileInfoValue?) {
        if (value == null) {
            return
        }
        prefs.edit()
            .putString(PREF_PROFILE_NAME, value.name)
            .putLong(PREF_PROFILE_ID, value.id)
            .apply()
    }

    @JvmStatic
    fun getLastTrackRecProfile(): TrackProfileInfoValue {
        val name = prefs.getString(PREF_PROFILE_NAME, null)
        val id = prefs.getLong(PREF_PROFILE_ID, -1)
        return TrackProfileInfoValue(id, name)
    }

    @JvmStatic
    fun getLastTrackRecProfileState(): TrackRecordingStateEnum {
        val name = prefs.getString(PREF_REC_STATE, null)
        var result = TrackRecordingStateEnum.NOT_RECORDING
        try {
            result = TrackRecordingStateEnum.valueOf(name!!)
        } catch (e: IllegalArgumentException) {
            // Do nothing, result value is preset
        } catch (e: NullPointerException) {
        }
        return result
    }

    fun persistFirstAppStart() {
        prefs.edit()
            .putBoolean(PREF_FIRST_APP_START, false)
            .apply()
    }

    fun isFirstAppStart(): Boolean {
        return prefs.getBoolean(PREF_FIRST_APP_START, true)
    }

    @JvmStatic
    fun isUseHwButtons(): Boolean {
        return prefs.getBoolean(PREF_USE_HW_BUTTONS, true)
    }

    fun persistUseHwButtons(useHwButtons: Boolean) {
        prefs.edit()
            .putBoolean(PREF_USE_HW_BUTTONS, useHwButtons)
            .apply()
    }

    @JvmStatic
    fun persistStatsScreenConfiguration(config: TrackRecordActivityConfiguration) {
        prefs.edit()
            .putString(PREF_STAT_CONFIG, Base64.encodeToString(config.asBytes, Base64.DEFAULT))
            .apply()
    }

    @JvmStatic
    fun getStatsScreenConfiguration(): CfgContainer? {
        val stringConfig = prefs.getString(PREF_STAT_CONFIG, null)
        return if (stringConfig == null) {
            null
        } else {
            try {
                CfgContainer(Base64.decode(stringConfig, Base64.DEFAULT))
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    var hrmFeatureConfigState: FeatureConfigEnum
        get() {
            val featureConfigID = prefs.getInt(PREF_USE_HRM, FeatureConfigEnum.NO_PERMISSION.id)
            return FeatureConfigEnum.getById(featureConfigID)
        }
        set(value) {
            hrmFeatureConfigStateLd.value = value
            prefs.edit()
                .putInt(PREF_USE_HRM, value.id)
                .apply()
        }

    val hrmFeatureConfigStateLd: MutableLiveData<FeatureConfigEnum> by lazy {
        MutableLiveData(hrmFeatureConfigState)
    }

    // DEBUG PART

    var isDebug: Boolean
        get() {
            return prefs.getBoolean(PREF_BOOL_IS_DEBUG, false)
        }
        set(value) {
            prefs.edit()
                .putBoolean(PREF_BOOL_IS_DEBUG, value)
                .apply()
        }

    /**
     * Deletes all shared preferences
     */
    fun debugClear() {
        prefs.edit().clear().apply()
    }
}