package com.asamm.locus.addon.wear;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;

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
	public static final String PREF_PROFILE_DESC = "PROFILE_DESC";

	public static final String PREF_DEVICE_ZOOM = "DEV_ZOOM";
	public static final String PREF_WEAR_ZOOM = "WEAR ZOOM";

	public static final String PREF_LAST_ACTIVITY_CLASS_NAME = "PREF_LAST_ACTIVITY_CLASS_NAME";

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
				.putString(PREF_PROFILE_DESC, value.getDesc())
				.apply();
	}

	public static TrackProfileInfoValue getLastTrackRecProfile(Context ctx) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String name = prefs.getString(PREF_PROFILE_NAME, null);
		String desc = prefs.getString(PREF_PROFILE_DESC, null);
		long id = prefs.getLong(PREF_PROFILE_ID, -1);
		return new TrackProfileInfoValue(id, name, desc);
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
		Integer deviceZoom = prefs.getInt(PREF_DEVICE_ZOOM, Const.ZOOM_UNKONWN);
		Integer wearZoom = prefs.getInt(PREF_WEAR_ZOOM, Const.ZOOM_UNKONWN);
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
}
