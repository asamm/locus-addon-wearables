package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

import com.asamm.locus.addon.wear.ApplicationState;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import locus.api.utils.Logger;

/**
 * Base class for wearable activities
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class LocusWearActivity extends WearableActivity {

    public WearActivityState mState = WearActivityState.ON_CREATE;
    protected CountDownTimer mConnectionFailedTimer;
    protected volatile boolean mInitialRequestSent = false;
    protected volatile boolean mIsInitialRequestReceived = false;

    /**
     * only used in connection failed timer to ensure handshake request is sent only once per activity start
     */
    private volatile boolean mIshandShakeRequestSent = false;

    /**
     * Each activity should define initial command which is sent automatically onStart()
     *
     * @return DataPath or null if activity want to just check connection on start and
     * not issue any other custom command.
     */
    protected abstract DataPayload getInitialCommandType();

    /**
     * Expected type of response for initialCommand
     *
     * @return
     */
    protected abstract DataPath getInitialCommandResponseType();

    /**
     * This method is called only once as a callback, that activity's initial command
     * got response and that comm is ready
     */
    protected void onGotInitialCommandResponse() {

    }

    /**
     * Consumes new data coming from WearListenerService
     *
     * @param path
     * @param data
     */
    public void consumeNewData(DataPath path, TimeStampStorable data) {
        Logger.logD(getClass().getSimpleName(), "Incoming data " + path);
        if (mConnectionFailedTimer != null) {
            if (path == DataPath.PUT_HAND_SHAKE || path == DataPath.PUT_ON_CONNECTED_EVENT) {
                handleConnectionFailedTimerTick();
            } else if (path == getInitialCommandResponseType()) {
                mIsInitialRequestReceived = true;
                onGotInitialCommandResponse();
                handleConnectionFailedTimerTick();
            }
        }
    }

    public WearActivityState getState() {
        return mState;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.mState = WearActivityState.ON_CREATE;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        this.mState = WearActivityState.ON_RESUME;
        super.onResume();
    }

    @Override
    protected void onPause() {
        this.mState = WearActivityState.ON_PAUSE;
        super.onPause();
    }

    protected boolean handleConnectionFailedTimerTick() {
        boolean result = false;
        WearCommService wcs = WearCommService.getInstance();
        if (!wcs.isConnected()) {
            wcs.reconnectIfNeeded();
        } else if (!getApplicationState().isHandShake()) {
            if (!mIshandShakeRequestSent) {
                mIshandShakeRequestSent = true;
                wcs.sendCommand(DataPath.GET_HAND_SHAKE);
            }
        } else if (!mInitialRequestSent) {
            mInitialRequestSent = true;
            DataPayload p = getInitialCommandType();
            if (p != null) {
                wcs.sendDataItem(p.getPath(), p.getStorable());
            } else {
                mIsInitialRequestReceived = true;
                onGotInitialCommandResponse();
                result = true;
            }
        }
        result |= mIsInitialRequestReceived;
        if (result) {
            cancelConnectionFailedTimer();
            onHandShakeFinished();
        }
        return result;

    }

    /**
     * Called after successful initial communication of the activity
     */
    protected void onHandShakeFinished() {

    }

    /**
     * Called if the activity could not establish connection and receive required data in
     * given time.
     */
    protected void onHandShakeFailed() {

    }
    protected void cancelConnectionFailedTimer() {
        if (mConnectionFailedTimer != null) {
            mConnectionFailedTimer.cancel();
            mIshandShakeRequestSent = false; // negotiating ended clearing state of handshake request
            mConnectionFailedTimer = null;   // and canceling and nulling timer
        }
    }

    @Override
    protected void onStop() {
        this.mState = WearActivityState.ON_STOP;
        super.onStop();
        WearCommService.getInstance().sendDataItem(DataPath.GET_PERIODIC_DATA, PeriodicCommand.createStopPeriodicUpdatesCommand());
        cancelConnectionFailedTimer();
    }

    @Override
    protected void onDestroy() {
        this.mState = WearActivityState.ON_DESTROY;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        this.mState = WearActivityState.ON_START;
        super.onStart();
        // checks connection and state of initial command, if not ready, initiates countDownTimer
        if (!handleConnectionFailedTimerTick()) {
            mConnectionFailedTimer = new CountDownTimer(8000, 400) {
                @Override
                public void onTick(long l) {
                    handleConnectionFailedTimerTick();
                }

                @Override
                public void onFinish() {
                    Logger.logE(LocusWearActivity.this.getClass().getSimpleName(), "Connection Failed!");
                    onHandShakeFailed();
                    // TODO cejnar - connection failed, handle the situation.
                }
            };
            mConnectionFailedTimer.start();
        }
    }

    // current activity state
    public enum WearActivityState {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY;

    }

    protected ApplicationState getApplicationState() {
        return ((MainApplication) this.getApplication()).getState();
    }

    public void handleNavigationDrawerItemClicked(View v) {
        final Class<? extends WearableActivity> activityToStart;
        switch (v.getId()) {
            case R.id.navigation_drawer_item_map:
                activityToStart = MapActivity.class;
                break;
            case R.id.navigation_drawer:
                activityToStart = TrackRecordActivity.class;
                break;
            default:
                activityToStart = null;
                break;
        }
        ((MainApplication)getApplication()).startLocusWearActivity(activityToStart);

    }

}
