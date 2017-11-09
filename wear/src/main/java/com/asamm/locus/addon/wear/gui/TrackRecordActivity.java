package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.containers.EmptyCommand;

import locus.api.utils.Logger;

public class TrackRecordActivity extends WearableActivity {

    private static final String TAG = TrackRecordActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_record);

        // Enables Always-on
        setAmbientEnabled();
    }

    public void handleClick(View v) {
        Logger.logD(TAG, "sending Reguest");
        WearCommService.getInstance().sendDataItem(DataPath.GET_HAND_SHAKE, new EmptyCommand());
    }
}
