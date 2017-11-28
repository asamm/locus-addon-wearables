package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

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
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivityConfiguration;
import com.asamm.locus.addon.wear.gui.trackrec.TrackStatLayout;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainScreenController implements TrackRecordingUpdatable{
	private ViewGroup mLayout;

	// recording active screen fields
	private ImageView mImgPauseRecording, mImgStopRecording, mImgAddWaypoint;
	private TrackStatLayout mStatsTop, mStatsBottom;

	private TrackRecordActivityConfiguration mConfig;
	private Drawable mPauseDrawable;

	MainScreenController(ViewGroup ctx) {
		LayoutInflater inflater = LayoutInflater.from(ctx.getContext());
		mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_main, ctx, false);
		mImgAddWaypoint = mLayout.findViewById(R.id.image_view_track_rec_add_wpt);
		mImgStopRecording = mLayout.findViewById(R.id.image_view_track_rec_stop);
		mImgPauseRecording = mLayout.findViewById(R.id.image_view_track_rec_pause);
		mStatsTop = mLayout.findViewById(R.id.track_main_top);
		mStatsBottom = mLayout.findViewById(R.id.track_main_bottom);
		loadAndInitStats(ctx.getContext(), mLayout);
		setDisabledDrawables(ctx.getContext());
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
				mImgPauseRecording.setImageDrawable(((ImageView)context.findViewById(R.id.img_track_rec_start)).getDrawable());
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
		mConfig = new TrackRecordActivityConfiguration(); // TODO cejnar Load from preferences on
		mStatsTop.setType(mConfig.getStatConfig()[0]);
		mStatsBottom.setType(mConfig.getStatConfig()[1]);
	}

	@Override
	public ViewGroup getRootView() {
		return mLayout;
	}

	// TODO cejnar debug only - generate disabled drawables programatically
	private void setDisabledDrawables(Context ctx) {
		Drawable d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_stop));
		mImgStopRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_pause));
		mPauseDrawable = d;
		mImgPauseRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_add_wpt));
		mImgAddWaypoint.setImageDrawable(d);
	}
}
