package com.asamm.locus.addon.wear.gui;

import android.support.wearable.activity.WearableActivity;

import com.assam.locus.addon.wear.common.communication.DataPath;

import locus.api.objects.Storable;

/**
 * Base class for wearable activities
 *
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class LocusWearActivity extends WearableActivity {

    public WearActivityState mState = WearActivityState.ON_CREATE;

    public abstract void consumeNewData(DataPath path, Storable data);

    public WearActivityState getState() {
        return mState;
    }

    public void setState(WearActivityState mState) {
        this.mState = mState;
    }

    // current activity state
    public enum WearActivityState {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY
    }
}
