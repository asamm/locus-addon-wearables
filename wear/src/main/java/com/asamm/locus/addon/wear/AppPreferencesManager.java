package com.asamm.locus.addon.wear;

import android.content.Context;
import android.preference.PreferenceManager;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

/**
 * Created by Milan Cejnar on 20.11.2017.
 * Asamm Software, s.r.o.
 */

public class AppPreferencesManager {
	public static final String PREF_REC_STATE = "REC_STATE";
	public static final String PREF_PROFILE_NAME = "PROFILE_NAME";

	public static void persistLastRecState(Context ctx, TrackRecordingValue trackRec) {
		if (trackRec == null || !trackRec.isInfoAvailable()) {
			return;
		}
		PreferenceManager.getDefaultSharedPreferences(ctx).edit()
				.putString(PREF_REC_STATE, trackRec.getTrackRecordingState().name())
				.putString(PREF_PROFILE_NAME, trackRec.getTrackRecProfileName())
				.apply();
	}
	public static String getLastTrackRecProfileName(Context ctx){
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_PROFILE_NAME, null);
	}
	public static TrackRecordingStateEnum getLastTrackRecProfileState(Context ctx){
		String name = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_REC_STATE, null);
		TrackRecordingStateEnum result = TrackRecordingStateEnum.NOT_RECORDING;
		try {
			result = TrackRecordingStateEnum.valueOf(name);
		} catch (IllegalArgumentException | NullPointerException e) {
			// Do nothing, result value is preset
		}
		return result;
	}
}
