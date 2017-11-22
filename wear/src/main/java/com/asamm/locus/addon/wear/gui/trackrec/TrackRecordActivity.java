package com.asamm.locus.addon.wear.gui.trackrec;

import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

import com.asamm.locus.addon.wear.AppPreferencesManager;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import java.util.Arrays;
import java.util.HashMap;

import locus.api.utils.Logger;

import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.IDLE;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.IDLE_WAITING;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.PAUSED;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.PAUSED_WAITING;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.REC;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.REC_WAITING;
import static com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState.UNINITIALIZED;

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
		PeriodicCommand cmd = new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_TRACK_RECORDING,
				REFRESH_PERIOD_MS);
		return new DataPayload(DataPath.GET_PERIODIC_DATA, cmd);
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

	@Override
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		super.consumeNewData(path, data);
		switch (path) {
			case PUT_TRACK_REC_PROFILE_INFO:
				TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) data;
				TrackRecordActivity.this.profileList = profiles;
				Logger.logD(TAG, "Loaded rec profiles " + profiles.getSize());
				break;
			case PUT_TRACK_REC_ICON_INFO:
				TrackProfileIconValue.ValueList icons = (TrackProfileIconValue.ValueList) data;
				profileIcons = icons;
				Logger.logD(TAG, "Loaded rec icons " + icons.getSize());
				break;
			case PUT_TRACK_REC:
				TrackRecordingValue trv = (TrackRecordingValue) data;
				handlePutTrackRec(trv);
				Logger.logD(TAG, "Loaded track info ");
				break;
			case PUT_ADD_WAYPOINT:
				MainApplication.showToast(this, getResources().getString(R.string.waypoint_added));
				break;
		}
	}

	private void handlePutTrackRec(final TrackRecordingValue trv) {
		if (trv == null || !trv.isInfoAvailable()) {
			DataPayload p = getInitialCommandType();
			WearCommService.getInstance().sendDataItem(p.getPath(), p.getStorable());
			return;
		}
		runOnUiThread(() -> {
			stateMachine.update(trv);
		});
	}

	private void updateTrackData(TrackRecordingValue data) {
		// TODO cejnar
	}

	@Override
	protected void onStart() {
		String profileName = AppPreferencesManager.getLastTrackRecProfileName(this);
		TrackRecordingStateEnum lastState = AppPreferencesManager.getLastTrackRecProfileState(this);
		// initialize starting model from persistence
		model = new TrackRecordingValue(true,
				lastState != TrackRecordingStateEnum.NOT_RECORDING,
				lastState == TrackRecordingStateEnum.PAUSED, profileName, null);
		if (lastState != TrackRecordingStateEnum.NOT_RECORDING) {
			transitionToRecState();
		} else {
			transitionToIdlestate();
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		AppPreferencesManager.persistLastRecState(this, model);
	}

	public void handleStartClick(View v) {
		sendStateChangeRequest(TrackRecordingStateEnum.RECORDING);
		stateMachine.transitionTo(REC_WAITING);
	}

	public void handleStopClick(View v) {
		sendStateChangeRequest(TrackRecordingStateEnum.NOT_RECORDING);
		stateMachine.transitionTo(IDLE_WAITING);
	}

	public void handlePauseClick(View v) {
		sendStateChangeRequest(TrackRecordingStateEnum.PAUSED);
		stateMachine.transitionTo(PAUSED_WAITING);
	}

	public void handleAddWaypointClick(View v) {
		WearCommService.getInstance().sendCommand(DataPath.GET_ADD_WAYPOINT);
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

	@Override
	public boolean isUsePeriodicData() {
		return true;
	}

	private void sendStateChangeRequest(TrackRecordingStateEnum newState) {
		WearCommService wcs = WearCommService.getInstance();
		TrackProfileInfoValue v = getRecordingInfo();
		TrackRecordingStateChangeValue command =
				new TrackRecordingStateChangeValue(newState, v != null ? v.getName() : null);
		wcs.sendDataItem(DataPath.PUT_TRACK_REC_STATE_CHANGE, command);
		DataPayload p = getInitialCommandType();
		wcs.sendDataItem(p.getPath(), p.getStorable());

	}

	private void transitionToRecState() {
		mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);
		Logger.logD(TAG, "setting rec screen");
	}

	private void transitionToIdlestate() {
		mRecViewFlipper.setDisplayedChild(FLIPPER_START_RECORDING_SCREEN_IDX);
		Logger.logD(TAG, "setting idle screen");
	}

	private void transitionToPauseState() {
		transitionToRecState();
	}

	private void enableIdleScreen() {
		Logger.logD(TAG, "Enabling idle screen");
	}

	private void enableRecScreen() {
		Logger.logD(TAG, "Enabling rec screen");
	}

	private void enablePausedScreen() {
		enableRecScreen();
	}


	private interface StateMachine<STATE extends Enum<STATE>, MODEL> {
		STATE getCurrentState();

		void update(MODEL model);

		void transitionTo(STATE newState);
	}

	private StateMachine<TrackRecActivityState, TrackRecordingValue> stateMachine =
			new StateMachine<TrackRecActivityState, TrackRecordingValue>() {
				private TrackRecActivityState mCurrentState = UNINITIALIZED;
				private HashMap<TrackRecActivityState, HashMap<TrackRecActivityState, Runnable>> mTransitionsFunctions;

				{
					TrackRecActivityState[] values = TrackRecActivityState.values();
					mTransitionsFunctions = new HashMap<>(values.length);
					for (TrackRecActivityState s : values) {
						mTransitionsFunctions.put(s, new HashMap<>());
					}

					// transitions for state uninitialized
					HashMap<TrackRecActivityState, Runnable> uninitializedTransitions
							= mTransitionsFunctions.get(UNINITIALIZED);
					uninitializedTransitions.put(IDLE_WAITING,
							TrackRecordActivity.this::enableIdleScreen);
					uninitializedTransitions.put(REC_WAITING,
							TrackRecordActivity.this::enableRecScreen);
					uninitializedTransitions.put(PAUSED_WAITING,
							TrackRecordActivity.this::enablePausedScreen);

					// transitions to waiting states from main states
					for (TrackRecActivityState toDual : Arrays.asList(IDLE, PAUSED, REC)) {
						final TrackRecActivityState to = toDual.getOtherWaitingState();
						for (TrackRecActivityState from : Arrays.asList(IDLE, PAUSED, REC)) {
							if (from == toDual)
								continue; // cant go back from enabled state to its wating state
							HashMap<TrackRecActivityState, Runnable> m = mTransitionsFunctions.get(from);
							if (to == IDLE_WAITING)
								m.put(to, TrackRecordActivity.this::transitionToIdlestate);
							else if (to == PAUSED_WAITING)
								m.put(to, TrackRecordActivity.this::transitionToPauseState);
							else if (to == REC_WAITING)
								m.put(to, TrackRecordActivity.this::transitionToRecState);
						}
					}

					// transitions to main state from their respecive waiting state
					mTransitionsFunctions.get(IDLE_WAITING).put(IDLE, TrackRecordActivity.this::enableIdleScreen);
					mTransitionsFunctions.get(REC_WAITING).put(REC, TrackRecordActivity.this::enableRecScreen);
					mTransitionsFunctions.get(PAUSED_WAITING).put(PAUSED, TrackRecordActivity.this::enablePausedScreen);
				}

				@Override
				public TrackRecActivityState getCurrentState() {
					return mCurrentState;
				}

				public void transitionTo(TrackRecActivityState newState) {
					Runnable action = mTransitionsFunctions.get(mCurrentState).get(newState);
					if (action != null) {
						action.run();
					}
					mCurrentState = newState;
				}

				@Override
				public void update(TrackRecordingValue trv) {
					model = trv;

					TrackRecordingStateEnum newRecordingState = trv.getTrackRecordingState();

					// State machine transition conditions
					if (mCurrentState == UNINITIALIZED) {
						transitionTo(newRecordingState == TrackRecordingStateEnum.NOT_RECORDING ?
								IDLE_WAITING :
								newRecordingState == TrackRecordingStateEnum.PAUSED ?
										PAUSED_WAITING : REC_WAITING);
						// immediately perform one more state machine step to skip current waiting state
						if (mCurrentState != UNINITIALIZED) {
							update(trv);
						}
					} else if (mCurrentState.isWaitingState() && newRecordingState == mCurrentState.getExpectedState()) {
						// received data I was waiting for, transition to enabled state
						transitionTo(mCurrentState.getOtherWaitingState());
					} else if (!mCurrentState.isWaitingState()) {
						TrackRecordingStateEnum expectedState =
								mCurrentState.getOtherWaitingState().getExpectedState();
						if (newRecordingState != expectedState) {
							switch (newRecordingState) {
								case PAUSED:
									transitionTo(PAUSED_WAITING);
									break;
								case RECORDING:
									transitionTo(REC_WAITING);
									break;
								case NOT_RECORDING:
									transitionTo(IDLE_WAITING);
							}
						}
					}
				}
			};
}
