package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import locus.api.utils.Logger;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by milan on 126.03.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */


public class RecordingSensorManager {
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private SensorEventListener mSensorEventListener;
    private boolean mUseHrSensor = true;
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
    public void startHrSensor(LocusWearActivity owner) {
        // No permission/sensor not available or HR already running
        if (!mUseHrSensor || mSensorEventListener != null)
            return;
        if (mSensorManager == null) {
            mSensorManager = ((SensorManager) owner.getSystemService(SENSOR_SERVICE));
            mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            if (mHeartRateSensor == null) {
                mUseHrSensor = false;
                return;
            }
            mSensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values != null)
                        // TODO cejnar consume data
                        // TODO cejnar start on resume directlz to active recording
                        //SensorManager.SENSOR_STATUS_ACCURACY_LOW
                        if (sensorEvent.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                            RecordingSensorStore.hrm.setValue(sensorEvent.values[0]);
                        }
                        // TODO cejnar debug only
                    RecordingSensorStore.hrm.setValue(sensorEvent.values[0]);
                        Logger.logD("HR", "Measured value " + sensorEvent.values[0]);
                        Logger.logD("HR", "Measured accurayy " + sensorEvent.accuracy);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            Logger.logD("HR", "Registering HR sensor");
            mSensorManager.registerListener(mSensorEventListener, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);

        }
    }

    public void stopHrSensor() {
        if (mSensorEventListener != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
            mSensorManager = null;
        }
    }

    public boolean handlePermissionResult(LocusWearActivity owner, int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == HR_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mUseHrSensor = true;
            } else {
                mUseHrSensor = false;
            }
            return  mUseHrSensor;
        }
        return false;
    }

    public void destroy() {
        stopHrSensor();
    }
}
