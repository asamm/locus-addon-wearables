/*
 * Created by Milan Cejnar on 26.03.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord.recording.sensors;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.asamm.locus.addon.wear.application.PreferencesEx;
import com.asamm.locus.addon.wear.application.FeatureConfigEnum;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import locus.api.utils.Logger;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Helper class for interfacing watch sensors
 */
public class RecordingSensorManager {
    private static final String TAG = "RecordingSensorManager";
    private static final int HR_REQUEST_CODE = Math.abs(Manifest.permission.BODY_SENSORS.hashCode());
    private volatile SensorEventListener mSensorEventListener;

    public static boolean checkBodySensorPermission(Context owner) {
        return ContextCompat.checkSelfPermission(owner, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkAndRequestBodySensorPermission(Context owner) {
        if (ContextCompat.checkSelfPermission(owner, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.NO_PERMISSION);
            if (owner instanceof Activity) {
                // Permission is not granted
                ActivityCompat.requestPermissions((Activity) owner,
                        new String[]{Manifest.permission.BODY_SENSORS}, HR_REQUEST_CODE);
            }
            return false;
        } else {
            return true;
        }
    }

    public boolean startHrSensor(Context ctx, boolean isRestart) {
        // No permission/sensor not available or HR already running
        if (PreferencesEx.getHrmFeatureConfig() != FeatureConfigEnum.ENABLED)
            return false;

        Sensor hrm = null;
        SensorManager sensorManager = ((SensorManager) ctx.getSystemService(SENSOR_SERVICE));
        if (sensorManager != null) {
            hrm = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }

        if (hrm == null) { // should not happen
            Logger.INSTANCE.logE(TAG, "Failed to get sensor in startHrSensor(). " +
                    "Sensor manager is " + (sensorManager == null ? "" : " not ") + " null.");
            return false;
        }

        synchronized (this) {
            if (mSensorEventListener != null) {
                Logger.INSTANCE.logD(TAG, "mSensorEventListener already registered");
                return true;
            }
            mSensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE &&
                            sensorEvent.values != null && sensorEvent.values.length > 0) {
                        float val = sensorEvent.values[0];
                        if (HrmValue.isValidHrm(val) && sensorEvent.accuracy >= SensorManager.SENSOR_STATUS_UNRELIABLE) {
                            RecordingSensorStore.hrm.setValue(val);
                        } else {
                            RecordingSensorStore.hrm.setValue(0);
                        }
                        RecordingSensorStore.hrmDebug.setValue(val, sensorEvent.accuracy);
                    } else {
                        RecordingSensorStore.hrmDebug.setValue(0, -3);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            sensorManager.registerListener(mSensorEventListener, hrm, isRestart ? SensorManager.SENSOR_DELAY_NORMAL : 1_400_000);
        }
        return true;
    }

    public void stopHrSensor(Context ctx) {
        if (mSensorEventListener == null)
            return;

        SensorManager sensorManager = ((SensorManager) ctx.getSystemService(SENSOR_SERVICE));
        if (sensorManager == null) { // should not happen
            Logger.INSTANCE.logE(TAG, "Failed to get sensorManager in stopHrSensor(). ");
        } else {
            Logger.INSTANCE.logD(TAG, "Removing HRM listener");
            synchronized (this) {
                if (mSensorEventListener != null) {
                    sensorManager.unregisterListener(mSensorEventListener);
                    mSensorEventListener = null;
                }
            }
        }
    }

    /**
     * Should only be called if app has permission to read body sensors
     */
    public static FeatureConfigEnum recheckSensorAvailability(Context ctx) {
        FeatureConfigEnum currentState = PreferencesEx.getHrmFeatureConfig();
        if (currentState != FeatureConfigEnum.NOT_AVAILABLE) {
            Logger.INSTANCE.logW(TAG, "recheckSensorAvailability() called with state  FeatureConfigEnum.NOT_AVAILABLE ");
            return currentState; // Other state than not available, do not check, should have not been called at all.
        }
        try {
            Sensor hearRateSensor = null;
            SensorManager sensorManager = ((SensorManager) ctx.getSystemService(SENSOR_SERVICE));
            if (sensorManager != null) {
                hearRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            }
            FeatureConfigEnum hrmConfig = hearRateSensor != null ? FeatureConfigEnum.ENABLED : FeatureConfigEnum.NOT_AVAILABLE;
            if (hrmConfig == FeatureConfigEnum.ENABLED) {
                // great, HRM has become accessible, enable it
                PreferencesEx.persistHrmFeatureConfig(hrmConfig);
            }
            return hrmConfig;
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "Error in recheckSensorAvailability(), e");
            return FeatureConfigEnum.NOT_AVAILABLE;
        }
    }

    public static FeatureConfigEnum handlePermissionResult(LocusWearActivity owner, int requestCode, String[] permissions, int[] grantResults) {
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
            PreferencesEx.persistHrmFeatureConfig(hrmConfig);
            return hrmConfig;
        }
        return FeatureConfigEnum.NOT_AVAILABLE;
    }

    public void destroy(Context ctx) {
        stopHrSensor(ctx);
    }
}
