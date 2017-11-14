package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.fragments.TrackRecordProfileSelectFragment;
import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.containers.EmptyCommand;
import com.assam.locus.addon.wear.common.communication.containers.PeriodicCommand;
import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

import locus.api.utils.Logger;

public class TrackRecordActivity extends LocusWearActivity {

    private static final String TAG = TrackRecordActivity.class.getSimpleName();

    private static final String PREF_REC_STATE = "PREF_REC_STATE";

    private static final int FLIPPER_START_RECORDING_SCREEN_IDX = 0;
    private static final int FLIPPER_RECORDING_RUNNING_SCREEN_IDX = 1;

    private static final int REFRESH_PERIOD_MS = 1000;

    private TrackRecordingStateEnum mRecordingState = null;

    private ViewFlipper mRecViewFlipper;

    @Override
    protected DataPath getInitialCommandType() {
        return DataPath.GET_TRACK_REC;
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

    public void handleClick(View v) {
        Logger.logD(TAG, "sending Reguest");
        WearCommService.getInstance().sendDataItem(DataPath.GET_HAND_SHAKE, new EmptyCommand());
    }

    @Override
    public void consumeNewData(DataPath path, TimeStampStorable data) {
        super.consumeNewData(path, data);
        switch (path) {
            case PUT_TRACK_REC_PROFILE_INFO:
                TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) data;
                Logger.logD(TAG, "Loaded rec profiles " + profiles.getSize());
                break;
            case PUT_TRACK_REC_ICON_INFO:
                TrackProfileIconValue.ValueList icons = (TrackProfileIconValue.ValueList) data;
                Logger.logD(TAG, "Loaded rec icons " + icons.getSize());
                break;
            case PUT_TRACK_REC:
                TrackRecordingValue trv = (TrackRecordingValue) data;
                handlePutTrackRec(trv);
                Logger.logD(TAG, "Loaded track info ");
        }
    }

    private void handlePutTrackRec(TrackRecordingValue trv) {
        if (trv == null || !trv.isInfoAvailable()) {
            WearCommService.getInstance().sendCommand(DataPath.GET_TRACK_REC);
            return;
        }
        TrackRecordingStateEnum receivedState =
                trv.isTrackRecPaused() ? TrackRecordingStateEnum.PAUSED :
                        trv.isTrackRecRecording() ? TrackRecordingStateEnum.RUNNING :
                                TrackRecordingStateEnum.NOT_RECORDING;
        if (receivedState != mRecordingState) {
            if (receivedState == TrackRecordingStateEnum.NOT_RECORDING) {
                mRecViewFlipper.setDisplayedChild(FLIPPER_START_RECORDING_SCREEN_IDX);
                WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA,
                        PeriodicCommand.createStopPeriodicUpdatesCommand());
            } else {
                WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA,
                        new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_ACITIVITY_TRACK_RECORDING,
                                REFRESH_PERIOD_MS));
                mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            }
            mRecordingState = receivedState;
        }

        if (mRecordingState != TrackRecordingStateEnum.NOT_RECORDING) {
            // TODO cejnar update fields with stats
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRecordingState = null;
        mRecViewFlipper.setDisplayedChild(0);

    }

    public void handleStartClick(View v) {
    }

    public void handleStopClick(View v) {
    }

    public void handlePauseClick(View v) {
    }

    public void handleAddWaypointClick(View v) {

    }

    private void requestStateChange(TrackRecordingStateEnum newState) {
        if (mRecordingState == newState || newState == null) {
            return;
        }
        WearCommService wcs = WearCommService.getInstance();
        TrackProfileInfoValue v = getRecordingInfo();

        TimeStampStorable command =
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

}
