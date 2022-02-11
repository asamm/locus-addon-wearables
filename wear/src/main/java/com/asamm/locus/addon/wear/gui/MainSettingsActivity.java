/*
 * Created by milan on 10.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.application.PreferencesEx;
import com.asamm.locus.addon.wear.application.FeatureConfigEnum;
import com.asamm.locus.addon.wear.application.TrackRecordingService;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.RecordingSensorManager;

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
    }

    private void updateSwitchState() {
        FeatureConfigEnum hrmConfig = PreferencesEx.getHrmFeatureConfig();
        switchHrm.setChecked(hrmConfig == FeatureConfigEnum.ENABLED);
        switchHrm.setTextColor(hrmConfig != FeatureConfigEnum.NOT_AVAILABLE ? getColor(R.color.base_dark_primary) : getColor(R.color.base_light_disabled));
    }

    private boolean isHrmEnabled() {
        FeatureConfigEnum hrmConfig = PreferencesEx.getHrmFeatureConfig();
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
            PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.DISABLED);
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
            FeatureConfigEnum hrmConfig = PreferencesEx.getHrmFeatureConfig();
            if (hrmConfig == FeatureConfigEnum.NO_PERMISSION) {
                if (RecordingSensorManager.checkBodySensorPermission(this)) {
                    PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.NOT_AVAILABLE);
                } else {
                    Toast.makeText(this, getString(R.string.err_no_hrm_permission), Toast.LENGTH_LONG).show();
                }
            }
            if (hrmConfig == FeatureConfigEnum.NOT_AVAILABLE) {
                FeatureConfigEnum tmpHrmConfig = RecordingSensorManager.recheckSensorAvailability(this);
                // HRM could have been enabled as side effect of calling recheckSensorAvailability()
                if (tmpHrmConfig == FeatureConfigEnum.NOT_AVAILABLE) {
                    // sensor still not available even after recheck
                    Toast.makeText(this, getString(R.string.err_hrm_sensor_not_available), Toast.LENGTH_LONG).show();
                }
            }
            if (hrmConfig == FeatureConfigEnum.DISABLED) {
                // sensor should be accesible and is only disabled, simply enable it
                PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.ENABLED);
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
