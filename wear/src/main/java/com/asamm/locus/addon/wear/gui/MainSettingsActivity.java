package com.asamm.locus.addon.wear.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.application.AppPreferencesManager;
import com.asamm.locus.addon.wear.application.FeatureConfigEnum;
import com.asamm.locus.addon.wear.application.TrackRecordingService;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.gui.trackrec.recording.sensors.RecordingSensorManager;

/*
 * Created by milan on 10.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class MainSettingsActivity extends LocusWearActivity {

    private Switch switchHrm;

    @Override
    protected DataPayload<EmptyCommand> getInitialCommandType() {
        return null;
    }

    @Override
    protected DataPath getInitialCommandResponseType() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);
        TextView header = findViewById(R.id.text_view_screen_header);
        if (header != null) {
            header.setText(getText(R.string.settings));
        }
        switchHrm = findViewById(R.id.switch_hrm);

        updateSwitchState();
        // Enables Always-on
        //setAmbientEnabled();
    }

    private void updateSwitchState() {
        switchHrm.setChecked(isHrmEnabled());
    }

    private boolean isHrmEnabled() {
        FeatureConfigEnum hrmConfig = AppPreferencesManager.getHrmFeatureConfig(this);
        return hrmConfig == FeatureConfigEnum.ENABLED;
    }

    /**
     * @return false for this activity, no handshaking required
     */
    @Override
    protected boolean isMakeHandshakeOnStart() {
        return false;
    }

    @Override
    public boolean isChildLocusWearActivity() {
        return true; // runs on top of TrackRecording activity, not independetly
    }

    @Override
    public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {

    }

    public void onHrmClicked(View view) {
        if (isHrmEnabled()) {
            AppPreferencesManager.persistHrmFeatureConfig(this, FeatureConfigEnum.DISABLED);
            if (TrackRecordingService.isRunning()) {
                Intent intent = new Intent(this, TrackRecordingService.class);
                intent.setAction(TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE);
                startService(intent);
            }
        } else {
            if (RecordingSensorManager.checkAndRequestBodySensorPermission(this)) {
                enableHrm();
            }
        }
        updateSwitchState();
    }

    private void enableHrm() {
        runOnUiThread(() -> {
            FeatureConfigEnum hrmConfig = AppPreferencesManager.getHrmFeatureConfig(this);
            // TODO cejnar string texts
            if (hrmConfig == FeatureConfigEnum.NO_PERMISSION) {
                if (RecordingSensorManager.checkBodySensorPermission(this)){
                    AppPreferencesManager.persistHrmFeatureConfig(this, FeatureConfigEnum.NOT_AVAILABLE);
                } else {
                    Toast.makeText(this, "Missing permission to read body sensors.", Toast.LENGTH_LONG).show();
                }
            }
            if (hrmConfig == FeatureConfigEnum.NOT_AVAILABLE) {
                FeatureConfigEnum tmpHrmConfig = RecordingSensorManager.recheckSensorAvailability(this);
                // HRM could have been enabled as side effect of calling recheckSensorAvailability()
                if (tmpHrmConfig == FeatureConfigEnum.NOT_AVAILABLE) {
                    // sensor still not available even after recheck
                    Toast.makeText(this, "HRM not available on your device.", Toast.LENGTH_LONG).show();
                }
            }
            if (hrmConfig == FeatureConfigEnum.DISABLED) {
                // sensor should be accesible and is only disabled, simply enable it
                AppPreferencesManager.persistHrmFeatureConfig(this, FeatureConfigEnum.ENABLED);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        RecordingSensorManager.handlePermissionResult(this, requestCode, permissions, grantResults);
        enableHrm();
        updateSwitchState();
    }
}
