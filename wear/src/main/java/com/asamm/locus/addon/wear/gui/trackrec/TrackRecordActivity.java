
package com.asamm.locus.addon.wear.gui.trackrec;

import android.app.FragmentManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import com.asamm.locus.addon.wear.AppPreferencesManager;
import com.asamm.locus.addon.wear.AppStorageManager;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.custom.DisableGuiHelper;
import com.asamm.locus.addon.wear.gui.trackrec.viewpager.TrackRecordPagerAdapter;
import com.asamm.locus.addon.wear.gui.trackrec.viewpager.VerticalViewPagerTransition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

	private static final int FLIPPER_START_RECORDING_SCREEN_IDX = 0;
	private static final int FLIPPER_RECORDING_RUNNING_SCREEN_IDX = 1;

	private static final int REFRESH_PERIOD_MS = 1000;

	private TrackRecordingValue model;

	private ViewFlipper mRecViewFlipper;

	private ViewPager mPager;
	private TrackRecordPagerAdapter pagerController;

	// start recording screen fields
	private ImageView mImgStartRecording;

	// model
	private volatile TrackProfileInfoValue.ValueList profileList;

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
		mImgStartRecording = findViewById(R.id.img_track_rec_start);


		mRecViewFlipper = findViewById(R.id.trackRecordViewFlipper);
		initViewPager();

		setDisabledDrawables();
		// Enables Always-on
		setAmbientEnabled();
	}

	private void initViewPager(){
		mPager = findViewById(R.id.pager);
		pagerController = new TrackRecordPagerAdapter(this);
		mPager.setPageTransformer(false, new VerticalViewPagerTransition());
		mPager.setAdapter(pagerController);
	}
	private void setDisabledDrawables() {
		Drawable d = DisableGuiHelper.getImageWithDisabled(this,
				BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_96_track_recording_rec));
		mImgStartRecording.setImageDrawable(d);
	}

	@Override
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		super.consumeNewData(path, data);
		switch (path) {
			case PUT_TRACK_REC_PROFILE_INFO:
				TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) data;
				TrackRecordActivity.this.profileList = profiles;
				runOnUiThread(() -> onNewProfilesReceived(profiles));
				Logger.logD(TAG, "Loaded rec profiles " + profiles.getSize());
				break;
			case PUT_PROFILE_ICON:
				final TrackProfileIconValue icon = (TrackProfileIconValue) data;
				runOnUiThread(() -> onNewIconReceived(icon));
				break;
			case PUT_TRACK_REC:
				TrackRecordingValue trv = (TrackRecordingValue) data;
				onPutTrackRec(trv);
				Logger.logD(TAG, "Loaded track info ");
				break;
			case PUT_ADD_WAYPOINT:
				MainApplication.showToast(this, getResources().getString(R.string.waypoint_added));
				break;
		}
	}

	private void onNewProfilesReceived(TrackProfileInfoValue.ValueList profiles) {
		TrackRecordProfileSelectFragment f = getSelectProfileFragment();
		TrackProfileInfoValue selectedProfile = f.getProfile();
		boolean isProfileMissing = selectedProfile == null || !profiles.getStorables().contains(selectedProfile); // TODO cejnar test the second condition branch
		if (isProfileMissing) {
			f.setParameters(profiles.getStorables().get(0), null);
		}
		doIconsInCacheCheck(profiles.getStorables());
	}

	private void onNewIconReceived(TrackProfileIconValue icon) {
		AppStorageManager.persistIcon(this, icon);

		TrackRecordProfileSelectFragment f = getSelectProfileFragment();
		TrackProfileInfoValue selectedProfile = f.getProfile();
		if (selectedProfile != null && selectedProfile.getId() == icon.getId()) {
			f.setParameters(selectedProfile, icon);
		}
		if (profileList != null) {
			doIconsInCacheCheck(profileList.getStorables());
		}
	}

	private void doIconsInCacheCheck(List<TrackProfileInfoValue> profiles) {
		for (TrackProfileInfoValue info : profiles) {
			if (!AppStorageManager.isIconCached(this, info.getId())) {
				WearCommService.getInstance().sendDataItem(DataPath.GET_PROFILE_ICON, new ProfileIconGetCommand(info.getId()));
				break;
			}
		}
	}

	private void onPutTrackRec(final TrackRecordingValue trv) {
		if (trv == null || !trv.isInfoAvailable()) {
			DataPayload p = getInitialCommandType();
			WearCommService.getInstance().sendDataItem(p.getPath(), p.getStorable());
			return;
		}
		runOnUiThread(() -> {
			stateMachine.update(trv);
			pagerController.onNewTrackRecordingData(this, trv);
			if (isRecScreenVisible()) {
				refreshStatistics(trv);
			}
		});
	}

	private void refreshStatistics(TrackRecordingValue trv) {

	}

	private boolean isRecScreenVisible() {
		return mRecViewFlipper.getDisplayedChild() == FLIPPER_RECORDING_RUNNING_SCREEN_IDX;
	}

	@Override
	protected void onStart() {
		TrackProfileInfoValue profileInfo = AppPreferencesManager.getLastTrackRecProfile(this);
		TrackRecordingStateEnum lastState = AppPreferencesManager.getLastTrackRecProfileState(this);
		if (profileInfo.getName() != null) {
			TrackRecordProfileSelectFragment f = getSelectProfileFragment();
			f.setParameters(profileInfo, null);
		}
		// initialize starting model from persistence
		model = new TrackRecordingValue(true,
				lastState != TrackRecordingStateEnum.NOT_RECORDING,
				lastState == TrackRecordingStateEnum.PAUSED, profileInfo.getName(), null);
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
		AppPreferencesManager.persistLastTrackRecProfile(this, getSelectProfileFragment().getProfile());
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
		if (stateMachine.getCurrentState() == REC) {
			sendStateChangeRequest(TrackRecordingStateEnum.PAUSED);
			stateMachine.transitionTo(PAUSED_WAITING);
		} else if (stateMachine.getCurrentState() == PAUSED) {
			sendStateChangeRequest(TrackRecordingStateEnum.RECORDING);
			stateMachine.transitionTo(REC_WAITING);
		}
	}

	public void handleAddWaypointClick(View v) {
		WearCommService.getInstance().sendCommand(DataPath.GET_ADD_WAYPOINT);
	}

	private TrackRecordProfileSelectFragment getSelectProfileFragment() {
		return (TrackRecordProfileSelectFragment) getFragmentManager()
				.findFragmentById(R.id.fragment_track_rec_profile_select);
	}

	private TrackProfileInfoValue getRecordingInfo() {
		TrackRecordProfileSelectFragment f = getSelectProfileFragment();
		if (f != null) {
			return f.getProfile();
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



	private void setIdleScreenEnabled(boolean isEnabled) {
		mImgStartRecording.setEnabled(isEnabled);
		getSelectProfileFragment().setEnabled(isEnabled);
	}

	private void transitionToRecState() {
		pagerController.onTrackActivityStateChange(this, stateMachine.getCurrentState());
		mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);
		Logger.logD(TAG, "setting rec screen");
	}

	private void transitionToIdlestate() {
		setIdleScreenEnabled(false);
		mRecViewFlipper.setDisplayedChild(FLIPPER_START_RECORDING_SCREEN_IDX);
		Logger.logD(TAG, "setting idle screen");
	}

	private void transitionToPauseState() {
		transitionToRecState();
	}

	private void enableIdleScreen() {
		setIdleScreenEnabled(true);
		Logger.logD(TAG, "Enabling idle screen");
	}

	private void enableRecScreen() {
		pagerController.onTrackActivityStateChange(this, stateMachine.getCurrentState());
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
							TrackRecordActivity.this::transitionToIdlestate);
					uninitializedTransitions.put(REC_WAITING,
							TrackRecordActivity.this::transitionToRecState);
					uninitializedTransitions.put(PAUSED_WAITING,
							TrackRecordActivity.this::transitionToPauseState);

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
