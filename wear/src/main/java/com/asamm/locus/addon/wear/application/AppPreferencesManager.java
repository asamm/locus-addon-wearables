package com.asamm.locus.addon.wear.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordActivity;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration;

import java.io.IOException;

/**
 * Helper class for mantaining shared preferences for this application
 * <p>
 * Created by Milan Cejnar on 20.11.2017.
 * Asamm Software, s.r.o.
 */

public class AppPreferencesManager {
    public static final String PREF_REC_STATE = "REC_STATE";

    public static final String PREF_PROFILE_NAME = "PROFILE_NAME";
    public static final String PREF_PROFILE_ID = "PROFILE_ID";

    public static final String PREF_DEVICE_ZOOM = "DEV_ZOOM";
    public static final String PREF_WEAR_ZOOM = "WEAR ZOOM";

    public static final String PREF_LAST_ACTIVITY_CLASS_NAME = "PREF_LAST_ACTIVITY_CLASS_NAME";
    private static final String PREF_FIRST_APP_START = "PREF_FIRST_APP_START";

    private static final String PREF_USE_HW_BUTTONS = "PREF_USE_HW_BUTTONS";

    private static final String PREF_MAP_AUTOROTATE_ENABLED = "PREF_MAP_AUTOROTATE_ENABLED";
    private static final String PREF_MAP_OFFSET_X = "PREF_MAP_OFFSET_X";
    private static final String PREF_MAP_OFFSET_Y = "PREF_MAP_OFFSET_Y";
    private static final String PREF_MAP_BEARING = "PREF_MAP_BEARING";

    //statistics screen
    private static final String PREF_STAT_CONFIG = "PREF_STAT_CFG";

    private static final String PREF_USE_HRM = "PREF_USE_HRM";
    private static final String PREF_BOOL_IS_DEBUG = "PREF_BOOL_IS_DEBUG";

    public static void persistLastRecState(Context ctx, TrackRecordingValue trackRec) {
        if (trackRec == null || !trackRec.isInfoAvailable()) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putString(PREF_REC_STATE, trackRec.getTrackRecordingState().name())
                .apply();
    }

    public static void persistLastTrackRecProfile(Context ctx, TrackProfileInfoValue value) {
        if (value == null) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putString(PREF_PROFILE_NAME, value.getName())
                .putLong(PREF_PROFILE_ID, value.getId())
                .commit();
    }

    public static TrackProfileInfoValue getLastTrackRecProfile(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String name = prefs.getString(PREF_PROFILE_NAME, null);
        long id = prefs.getLong(PREF_PROFILE_ID, -1);
        return new TrackProfileInfoValue(id, name);
    }

    public static TrackRecordingStateEnum getLastTrackRecProfileState(Context ctx) {
        String name = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REC_STATE, null);
        TrackRecordingStateEnum result = TrackRecordingStateEnum.NOT_RECORDING;
        try {
            result = TrackRecordingStateEnum.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            // Do nothing, result value is preset
        }
        return result;
    }

    /**
     * Sets deviceZoom and wearZoom values if not null. Null values are ignored and not stored.
     *
     * @param ctx
     * @param deviceZoom
     * @param wearZoom
     */
    public static void persistZoomValues(Context ctx, Integer deviceZoom, Integer wearZoom) {
        SharedPreferences.Editor prefsEditor =
                PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        if (deviceZoom != null) {
            prefsEditor.putInt(PREF_DEVICE_ZOOM, deviceZoom.intValue());
        }
        if (wearZoom != null) {
            prefsEditor.putInt(PREF_WEAR_ZOOM, wearZoom.intValue());
        }
        prefsEditor.apply();
    }

    /**
     * @return pair of values - deviceZoom first, wearZomm second
     */
    public static Pair<Integer, Integer> getZoomValues(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Integer deviceZoom = prefs.getInt(PREF_DEVICE_ZOOM, Const.ZOOM_UNKOWN);
        Integer wearZoom = prefs.getInt(PREF_WEAR_ZOOM, Const.ZOOM_UNKOWN);
        return Pair.of(deviceZoom, wearZoom);
    }

    public static void persistLastActivity(Context ctx, Class<? extends LocusWearActivity> c) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putString(PREF_LAST_ACTIVITY_CLASS_NAME, c.getSimpleName())
                .apply();
    }

    public static String getLastActivity(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(PREF_LAST_ACTIVITY_CLASS_NAME, TrackRecordActivity.class.getSimpleName());
    }

    public static void persistFirstAppStart(Context ctx) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putBoolean(PREF_FIRST_APP_START, false)
                .apply();
    }

    public static boolean isFirstAppStart(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PREF_FIRST_APP_START, true);
    }

    /**
     * Deletes all shared preferences
     *
     * @param ctx
     */
    public static void debugClear(Context ctx) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().clear().commit();
    }

    public static boolean isUseHwButtons(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PREF_USE_HW_BUTTONS, true);
    }

    public static void persistUseHwButons(Context ctx, boolean useHwButons) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putBoolean(PREF_USE_HW_BUTTONS, useHwButons)
                .apply();
    }

    public static boolean isMapAutoRotateEnabled(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PREF_MAP_AUTOROTATE_ENABLED, false);
    }

    public static void persistMapAutoRotateEnabled(Context ctx, boolean mapAutorotateEnabled) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putBoolean(PREF_MAP_AUTOROTATE_ENABLED, mapAutorotateEnabled)
                .apply();
    }

    public static short getMapBearing(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return (short) prefs.getInt(PREF_MAP_BEARING, 0);
    }

    public static void persistMapBearing(Context ctx, short mapBearing) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putInt(PREF_MAP_BEARING, mapBearing)
                .apply();
    }

    /**
     * Sets map offset x and y values if not null. Null values are ignored and not stored.
     *
     * @param ctx
     * @param offsetX
     * @param offsetY
     */
    public static void persistMapOffsetValues(Context ctx, Integer offsetX, Integer offsetY) {
        SharedPreferences.Editor prefsEditor =
                PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        if (offsetX != null) {
            prefsEditor.putInt(PREF_MAP_OFFSET_X, offsetX.intValue());
        }
        if (offsetY != null) {
            prefsEditor.putInt(PREF_MAP_OFFSET_Y, offsetY.intValue());
        }
        prefsEditor.apply();
    }

    /**
     * @return pair of values - offsetX, offsetY
     */
    public static Pair<Integer, Integer> getMapOffsetValues(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Integer offsetX = prefs.getInt(PREF_MAP_OFFSET_X, 0);
        Integer offsetY = prefs.getInt(PREF_MAP_OFFSET_Y, 0);
        return Pair.of(offsetX, offsetY);
    }

    public static void persistStatsScreenConfiguration(Context ctx, TrackRecordActivityConfiguration config) {
        SharedPreferences.Editor prefsEditor =
                PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        prefsEditor.putString(PREF_STAT_CONFIG, Base64.encodeToString(config.getAsBytes(), Base64.DEFAULT));
        prefsEditor.apply();
    }

    public static TrackRecordActivityConfiguration.CfgContainer getStatsScreenConfiguration(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String stringConfig = prefs.getString(PREF_STAT_CONFIG, null);
        if (stringConfig == null) {
            return null;
        } else {
            try {
                return new TrackRecordActivityConfiguration.CfgContainer(Base64.decode(stringConfig, Base64.DEFAULT));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void persistHrmFeatureConfig(Context ctx, FeatureConfigEnum configState) {
        if (configState == null)
            return;

        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putInt(PREF_USE_HRM, configState.getId())
                .commit();
    }

    public static FeatureConfigEnum getHrmFeatureConfig(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int featureConfigID = prefs.getInt(PREF_USE_HRM, FeatureConfigEnum.NO_PERMISSION.getId());
        return FeatureConfigEnum.getById(featureConfigID);
    }
    @SuppressLint("ApplySharedPref")
    public static void persistIsDebug(Context ctx, boolean isDebug) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit()
                .putBoolean(PREF_BOOL_IS_DEBUG, isDebug)
                .apply();
    }

    public static boolean isDebug(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean isDebug = prefs.getBoolean(PREF_BOOL_IS_DEBUG, false);
        return isDebug;
    }

}
