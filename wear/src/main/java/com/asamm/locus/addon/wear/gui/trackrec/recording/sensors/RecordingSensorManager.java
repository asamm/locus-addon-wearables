/*
 * Created by Milan Cejnar on 26.03.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.asamm.locus.addon.wear.application.AppPreferencesManager;
import com.asamm.locus.addon.wear.application.FeatureConfigEnum;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import locus.api.utils.Logger;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Helper class for interfacing watch sensors
 */
public class RecordingSensorManager {
    private static final String TAG = "RecordingSensorManager";
    private SensorEventListener mSensorEventListener;
    private static final int HR_REQUEST_CODE = Math.abs(Manifest.permission.BODY_SENSORS.hashCode());

    public boolean checkAndRequestBodySensorPermission(LocusWearActivity owner) {
        if (ContextCompat.checkSelfPermission(owner, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(owner,
                    new String[]{Manifest.permission.BODY_SENSORS}, HR_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    public boolean startHrSensor(Context ctx) {
        // No permission/sensor not available or HR already running
        if (AppPreferencesManager.getHrmFeatureConfig(ctx) != FeatureConfigEnum.ENABLED
                || mSensorEventListener != null)
            return false;

        Sensor hrm = null;
        SensorManager sensorManager = ((SensorManager) ctx.getSystemService(SENSOR_SERVICE));
        if (sensorManager != null) {
            hrm = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }

        if (hrm == null) { // should not happen
            Logger.logE(TAG, "Failed to get sensor in startHrSensor(). " +
                    "Sensor manager is " + (sensorManager == null ? "" : " not ") + " null.");
            return false;
        }

        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.values != null)
                    // TODO cejnar accuracy check
                    //SensorManager.UNRELIABLE
                    if (sensorEvent.accuracy >= SensorManager.SENSOR_STATUS_NO_CONTACT) {
                        RecordingSensorStore.hrm.setValue(sensorEvent.values[0]);
                    }
                Logger.logD("HRM", "Measured value " + sensorEvent.values[0]);
                Logger.logD("HRM", "Measured accurayy " + sensorEvent.accuracy);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        Logger.logD("HR", "Registering HR sensor");
        sensorManager.registerListener(mSensorEventListener, hrm, SensorManager.SENSOR_DELAY_NORMAL);
        return true;
    }

    public void stopHrSensor(Context ctx) {
        if (mSensorEventListener == null)
            return;

        SensorManager sensorManager = ((SensorManager) ctx.getSystemService(SENSOR_SERVICE));
        if (sensorManager == null) { // should not happen
            Logger.logE(TAG, "Failed to get sensorManager in stopHrSensor(). ");
        } else {
            sensorManager.unregisterListener(mSensorEventListener);
        }
    }

    public FeatureConfigEnum handlePermissionResult(LocusWearActivity owner, int requestCode, String[] permissions, int[] grantResults) {
        final FeatureConfigEnum hrmConfig;
        if (requestCode == HR_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Sensor hearRateSensor = null;
                SensorManager sensorManager = ((SensorManager) owner.getSystemService(SENSOR_SERVICE));
                if (sensorManager != null) {
                    hearRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
                }
                hrmConfig = hearRateSensor != null ? FeatureConfigEnum.ENABLED : FeatureConfigEnum.NOT_AVAILABLE;
            } else {
                hrmConfig = FeatureConfigEnum.DISABLED;
            }
            AppPreferencesManager.persistHrmFeatureConfig(owner, hrmConfig);
            return hrmConfig;
        }
        return FeatureConfigEnum.NOT_AVAILABLE;
    }

    public void destroy(Context ctx) {
        stopHrSensor(ctx);
    }
}
