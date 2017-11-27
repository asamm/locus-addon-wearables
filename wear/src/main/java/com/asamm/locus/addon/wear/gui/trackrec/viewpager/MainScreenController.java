package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
	private ViewGroup layout;

	// recording active screen fields
	private ImageView mImgPauseRecording;
	private ImageView mImgStopRecording;
	private ImageView mImgAddWaypoint;
	private TrackRecordActivityConfiguration config;
	private Drawable pauseDrawable;

	MainScreenController(Activity ctx) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		final ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_main, null);
		mImgAddWaypoint = layout.findViewById(R.id.image_view_track_rec_add_wpt);
		mImgStopRecording = layout.findViewById(R.id.image_view_track_rec_stop);
		mImgPauseRecording = layout.findViewById(R.id.image_view_track_rec_pause);
		loadAndInitStats(ctx, layout);
		setDisabledDrawables(ctx);
	}
	@Override
	public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
		switch (newState) {
			case PAUSED_WAITING:
				mImgPauseRecording.setImageDrawable(((ImageView)context.findViewById(R.id.img_track_rec_start)).getDrawable());
			case UNINITIALIZED:
			case REC_WAITING:
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
		FragmentManager fm = context.getFragmentManager();
		int[] ids = {R.id.track_main_top, R.id.track_main_bottom};
		for (int id : ids) {
			TrackStatLayout f = ((TrackStatLayout) fm.findFragmentById(id));
			if (f != null) {
				f.consumeNewStatistics(newData);
			}
		}
	}

	private void setRecScreenEnabled(boolean isEnabled) {
		mImgPauseRecording.setEnabled(isEnabled);
		mImgStopRecording.setEnabled(isEnabled);
		mImgAddWaypoint.setEnabled(isEnabled);
	}

	private void loadAndInitStats(Activity context, ViewGroup view) {
		FrameLayout fragTop = view.findViewById(R.id.track_main_top);

		config = new TrackRecordActivityConfiguration(); // TODO cejnar Load from preferences on start
		fm.beginTransaction()
				.replace(R.id.track_main_top, TrackStatLayout.newInstance(config.getStatConfig()[0], false))
				.replace(R.id.track_main_bottom, TrackStatLayout.newInstance(config.getStatConfig()[1],true))
				.commit();
	}

	@Override
	public ViewGroup getRootView() {
		return layout;
	}

	// TODO cejnar debug only - generate disabled drawables programatically
	private void setDisabledDrawables(Context ctx) {
		Drawable d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_stop));
		mImgStopRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_pause));
		pauseDrawable = d;
		mImgPauseRecording.setImageDrawable(d);

		d = DisableGuiHelper.getImageWithDisabled(ctx,
				BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_96_track_recording_add_wpt));
		mImgAddWaypoint.setImageDrawable(d);
	}
}
