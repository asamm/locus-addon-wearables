package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;

import locus.api.objects.Storable;

/**
 * Base class for wearable activities
 *
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class LocusWearActivity extends WearableActivity {

    public WearActivityState mState = WearActivityState.ON_CREATE;

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

    @Override
    protected void onStop() {
        this.mState = WearActivityState.ON_STOP;
        super.onStop();
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

    public abstract void consumeNewData(DataPath path, TimeStampStorable data);
}
