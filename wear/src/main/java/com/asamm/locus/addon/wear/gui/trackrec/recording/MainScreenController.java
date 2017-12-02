package com.asamm.locus.addon.wear.gui.trackrec.recording;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.DisableGuiHelper;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;
import com.asamm.locus.addon.wear.gui.trackrec.configuration.TrackRecordActivityConfiguration;

/**
 * Controller class for main/control screen of active track recording
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainScreenController implements TrackRecordingControllerUpdatable {
	private ViewGroup mLayout;

	// recording active screen fields
	private ImageView mImgPauseRecording, mImgStopRecording, mImgAddWaypoint;
	private TrackStatLayout mStatsTop, mStatsBottom;

	private TrackRecordActivityConfiguration mConfig;
	private Drawable mPauseDrawable;

	public MainScreenController(ViewGroup parentViewGroup) {
		LayoutInflater inflater = LayoutInflater.from(parentViewGroup.getContext());
		mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_main, parentViewGroup, false);
		mImgAddWaypoint = mLayout.findViewById(R.id.image_view_track_rec_add_wpt);
		mImgStopRecording = mLayout.findViewById(R.id.image_view_track_rec_stop);
		mImgPauseRecording = mLayout.findViewById(R.id.image_view_track_rec_pause);
		mStatsTop = mLayout.findViewById(R.id.track_main_top);
		mStatsBottom = mLayout.findViewById(R.id.track_main_bottom);
		loadAndInitStats(parentViewGroup.getContext(), mLayout);
		setDisabledDrawables(parentViewGroup.getContext());
	}

	@Override
	public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
		switch (newState) {
			case UNINITIALIZED:
			case REC_WAITING:
				mImgPauseRecording.setImageDrawable(mPauseDrawable);
				setRecScreenEnabled(false);
				break;
			case PAUSED_WAITING:
				mImgPauseRecording.setImageDrawable(context.getDrawable(R.drawable.ic_track_record_pause_pressed));
				setRecScreenEnabled(false);
				break;
			case PAUSED:
			case REC:
				setRecScreenEnabled(true);
				break;
		}
	}

	@Override
	public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
		if (newData == null) return;
		mStatsTop.consumeNewStatistics(newData);
		mStatsBottom.consumeNewStatistics(newData);
	}

	private void setRecScreenEnabled(boolean isEnabled) {
		mImgPauseRecording.setEnabled(isEnabled);
		mImgStopRecording.setEnabled(isEnabled);
		mImgAddWaypoint.setEnabled(isEnabled);
	}

	private void loadAndInitStats(Context context, ViewGroup view) {
		mConfig = TrackRecordActivityConfiguration.loadConfiguration(context);
		mStatsTop.setType(mConfig.getStatConfigAtPosition(0));
		mStatsBottom.setType(mConfig.getStatConfigAtPosition(1));
	}

	@Override
	public ViewGroup getControllersView() {
		return mLayout;
	}

	// TODO cejnar debug only - generate disabled drawables programatically
	private void setDisabledDrawables(Context ctx) {
		Drawable d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_stop));
		mImgStopRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_pause_normal));
		mPauseDrawable = d;
		mImgPauseRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_add_wpt));
		mImgAddWaypoint.setImageDrawable(d);
	}
}
