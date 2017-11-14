package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.assam.locus.addon.wear.common.communication.DataPath;

public class MapActivity extends LocusWearActivity {

    private TextView mTextView;

    @Override
    protected DataPath getInitialCommandType() {
        return null;
    }

    @Override
    protected DataPath getInitialCommandResponseType() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();
    }


}
