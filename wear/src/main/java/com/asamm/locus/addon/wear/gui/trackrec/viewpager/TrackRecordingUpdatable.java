package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.app.Activity;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public interface TrackRecordingUpdatable {
	void onTrackActivityStateChange(Activity context, TrackRecActivityState newState);

	void onNewTrackRecordingData(Activity context, TrackRecordingValue newData);

	ViewGroup getRootView();

}
