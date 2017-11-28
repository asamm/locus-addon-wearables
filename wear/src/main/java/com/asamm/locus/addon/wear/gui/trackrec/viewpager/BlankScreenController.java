package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class BlankScreenController implements TrackRecordingUpdatable {
	private ViewGroup mLayout;

	BlankScreenController(ViewGroup ctx) {
		mLayout = new LinearLayout(ctx.getContext());
	}

	@Override
	public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
	}

	@Override
	public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
	}

	@Override
	public ViewGroup getRootView() {
		return mLayout;
	}

}
