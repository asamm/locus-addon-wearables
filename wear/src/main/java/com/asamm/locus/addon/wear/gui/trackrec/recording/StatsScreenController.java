package com.asamm.locus.addon.wear.gui.trackrec.recording;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;
import com.asamm.locus.addon.wear.gui.trackrec.configuration.TrackRecordActivityConfiguration;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class StatsScreenController implements TrackRecordingControllerUpdatable {
	private ViewGroup mLayout;

	// recording active screen fields
	private TrackStatLayout mTopLeft, mTopRight, mBottomLeft, mBottomRight;

	private final int mScreenIdx;
	public StatsScreenController(ViewGroup ctx, int screenIdx) {
		this.mScreenIdx = screenIdx;
		LayoutInflater inflater = LayoutInflater.from(ctx.getContext());
		mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_stats, ctx, false);
		mTopLeft = mLayout.findViewById(R.id.track_stats_top_left);
		mTopRight = mLayout.findViewById(R.id.track_stats_top_right);
		mBottomLeft = mLayout.findViewById(R.id.track_stats_bottom_left);
		mBottomRight = mLayout.findViewById(R.id.track_stats_bottom_right);
		loadAndInitStats(ctx.getContext(), mLayout);
	}

	@Override
	public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {

	}

	@Override
	public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
		if (newData == null) return;
		mTopLeft.consumeNewStatistics(newData);
		mTopRight.consumeNewStatistics(newData);
		mBottomLeft.consumeNewStatistics(newData);
		mBottomRight.consumeNewStatistics(newData);
	}

	private void loadAndInitStats(Context context, ViewGroup view) {
		TrackRecordActivityConfiguration mConfig = TrackRecordActivityConfiguration.loadConfiguration(context);
		int idxOffset = -2 + (mScreenIdx << 2);
		mTopRight.setType(mConfig.getStatConfigAtPosition(idxOffset));
		mTopLeft.setType(mConfig.getStatConfigAtPosition(idxOffset + 1));
		mBottomRight.setType(mConfig.getStatConfigAtPosition(idxOffset + 2));
		mBottomLeft.setType(mConfig.getStatConfigAtPosition(idxOffset + 3));
	}

	@Override
	public ViewGroup getControllersView() {
		return mLayout;
	}

}
