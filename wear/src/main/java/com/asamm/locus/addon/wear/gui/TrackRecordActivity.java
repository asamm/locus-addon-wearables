package com.asamm.locus.addon.wear.gui;

import android.app.NotificationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.wear.widget.drawer.WearableDrawerView;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.fragments.TrackRecordProfileSelectFragment;

import locus.api.utils.Logger;

public class TrackRecordActivity extends LocusWearActivity {

	private static final String TAG = TrackRecordActivity.class.getSimpleName();

	private static final String PREF_REC_STATE = "PREF_REC_STATE";

	private static final int FLIPPER_START_RECORDING_SCREEN_IDX = 0;
	private static final int FLIPPER_RECORDING_RUNNING_SCREEN_IDX = 1;

	private static final int REFRESH_PERIOD_MS = 1000;

	private TrackRecordingValue model;
	private ViewFlipper mRecViewFlipper;
	private volatile TrackProfileInfoValue.ValueList profileList;
	private volatile TrackProfileIconValue.ValueList profileIcons;


	@Override
	protected DataPayload getInitialCommandType() {
		return new DataPayload(DataPath.GET_TRACK_REC, new EmptyCommand());
	}

	@Override
	protected DataPath getInitialCommandResponseType() {
		return DataPath.PUT_TRACK_REC;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_record_activity);
		mRecViewFlipper = findViewById(R.id.trackRecordViewFlipper);
		// Enables Always-on
		setAmbientEnabled();
	}

	private void handleModelChanged() {
		// TODO cejnar update state of components depending on current model
	}

	@Override
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		super.consumeNewData(path, data);
		switch (path) {
			case PUT_TRACK_REC_PROFILE_INFO:
				TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) data;
				TrackRecordActivity.this.profileList = profiles;
				handleModelChanged();
				Logger.logD(TAG, "Loaded rec profiles " + profiles.getSize());
				break;
			case PUT_TRACK_REC_ICON_INFO:
				TrackProfileIconValue.ValueList icons = (TrackProfileIconValue.ValueList) data;
				profileIcons = icons;
				handleModelChanged();
				Logger.logD(TAG, "Loaded rec icons " + icons.getSize());
				break;
			case PUT_TRACK_REC:
				TrackRecordingValue trv = (TrackRecordingValue) data;
				handlePutTrackRec(trv);
				Logger.logD(TAG, "Loaded track info ");
				break;
			case PUT_ADD_WAYPOINT:
				MainApplication.showToast();
				break;
		}
	}

	private void handlePutTrackRec(final TrackRecordingValue trv) {
		if (trv == null || !trv.isInfoAvailable()) {
			WearCommService.getInstance().sendCommand(DataPath.GET_TRACK_REC);
			return;
		}
		TrackRecordingStateEnum recordingState = model == null ? null : model.getTrackRecordingState();
		final TrackRecordingStateEnum newRecordingState = trv.getTrackRecordingState();

		final int requestScreenTransitionIdx;
		if (newRecordingState != recordingState) {
			if (newRecordingState == TrackRecordingStateEnum.NOT_RECORDING) {
				requestScreenTransitionIdx = FLIPPER_START_RECORDING_SCREEN_IDX;
				WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA,
						PeriodicCommand.createStopPeriodicUpdatesCommand());
			} else {
				WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA,
						new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_ACITIVITY_TRACK_RECORDING,
								REFRESH_PERIOD_MS));
				requestScreenTransitionIdx = FLIPPER_RECORDING_RUNNING_SCREEN_IDX;
				Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
			}
		} else {
			requestScreenTransitionIdx = -1;
		}
		model = trv;

		if (requestScreenTransitionIdx >= 0 || newRecordingState != TrackRecordingStateEnum.NOT_RECORDING) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (requestScreenTransitionIdx >= 0) {
						mRecViewFlipper.setDisplayedChild(requestScreenTransitionIdx);
					}
					if (newRecordingState != TrackRecordingStateEnum.NOT_RECORDING) {
						updateTrackData(trv);
					}
				}
			});
		}
	}

	private void updateTrackData(TrackRecordingValue data) {
		// TODO cejnar
	}

	@Override
	protected void onStart() {
		super.onStart();
		model = null;
		// TODO cejnar preferences ?
		mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);

	}

	public void handleStartClick(View v) {
		requestStateChange(TrackRecordingStateEnum.RECORDING);
	}

	public void handleStopClick(View v) {
		requestStateChange(TrackRecordingStateEnum.NOT_RECORDING);
	}

	public void handlePauseClick(View v) {
		requestStateChange(TrackRecordingStateEnum.PAUSED);
	}

	public void handleAddWaypointClick(View v) {
		WearCommService.getInstance().sendCommand(DataPath.GET_ADD_WAYPOINT);
	}

	/**
	 * Sends request to change recording state (ie. START/STOP/PAUSE) recording
	 * to the device.
	 *
	 * @param newState
	 */
	private void requestStateChange(TrackRecordingStateEnum newState) {
		final TrackRecordingValue model = this.model;
		if (newState == null || (model != null && model.getTrackRecordingState() == newState)) {
			return;
		}
		WearCommService wcs = WearCommService.getInstance();
		TrackProfileInfoValue v = getRecordingInfo();

		TrackRecordingStateChangeValue command =
				new TrackRecordingStateChangeValue(newState, v != null ? v.getName() : null);
		wcs.sendDataItem(DataPath.PUT_TRACK_REC_STATE_CHANGE, command);
	}

	private void persistRecordingState(TrackRecordingStateEnum state) {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putString(PREF_REC_STATE, state == null ? TrackRecordingStateEnum.NOT_RECORDING.name() : state.name())
				.apply();
	}

	private TrackProfileInfoValue getRecordingInfo() {
		TrackRecordProfileSelectFragment f =
				(TrackRecordProfileSelectFragment) getFragmentManager()
						.findFragmentById(R.id.trackRecProfileSelectFragment);
		if (f != null) {
			return f.getmProfile();
		}
		return null;
	}

	@Override
	protected void onHandShakeFinished() {
		super.onHandShakeFinished();
		WearCommService wcs = WearCommService.getInstance();
		wcs.sendCommand(DataPath.GET_TRACK_REC_PROFILES);
	}

	public TrackProfileInfoValue.ValueList getProfileList() {
		return profileList;
	}

	public TrackProfileIconValue.ValueList getProfileIcons() {
		return profileIcons;
	}
}
