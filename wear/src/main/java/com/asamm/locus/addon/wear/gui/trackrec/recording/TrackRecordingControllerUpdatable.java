package com.asamm.locus.addon.wear.gui.trackrec.recording;

import android.app.Activity;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;

/**
 * Common interface to all "controllers" that work with active track recording data and statistics
 * <p>
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public interface TrackRecordingControllerUpdatable {
	/**
	 * Called when track recording state changed
	 *
	 * @param context
	 * @param newState
	 */
	void onTrackActivityStateChange(Activity context, TrackRecActivityState newState);

	/**
	 * called after receival of new track recording data and statistics
	 *
	 * @param context
	 * @param newData
	 */
	void onNewTrackRecordingData(Activity context, TrackRecordingValue newData);

	/**
	 * @return root view which is represented by this controller
	 */
	ViewGroup getControllersView();

}
