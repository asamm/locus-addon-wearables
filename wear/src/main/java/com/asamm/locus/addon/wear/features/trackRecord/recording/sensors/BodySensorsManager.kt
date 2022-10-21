/*
 * Created by Milan Cejnar on 26.03.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord.recording.sensors

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum
import locus.api.utils.Logger
import kotlin.math.abs

/**
 * Helper class for interfacing watch sensors
 */
class BodySensorsManager {

    @Volatile
    private var sensorEventListener: SensorEventListener? = null

    fun startHrSensor(ctx: Context, isRestart: Boolean): Boolean {
        // No permission/sensor not available or HR already running
        if (PreferencesEx.hrmFeatureConfigState !== FeatureConfigEnum.ENABLED) return false
        var hrm: Sensor? = null
        val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager != null) {
            hrm = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        }
        if (hrm == null) {
            Logger.logE(
                TAG, "Failed to get sensor in startHrSensor(). " +
                        "Sensor manager is " + (if (sensorManager == null) "" else " not ") + " null."
            )
            return false
        }

        synchronized(this) {
            if (sensorEventListener != null) {
                Logger.logD(TAG, "mSensorEventListener already registered")
                return true
            }
            sensorEventListener = object : SensorEventListener {

                override fun onSensorChanged(sensorEvent: SensorEvent) {
                    if (sensorEvent.sensor.type == Sensor.TYPE_HEART_RATE
                        && sensorEvent.values != null
                        && sensorEvent.values.isNotEmpty()
                    ) {
                        val value = sensorEvent.values[0]
                        if (HrmValue.isValidHrm(value)
                            && sensorEvent.accuracy >= SensorManager.SENSOR_STATUS_UNRELIABLE
                        ) {
                            RecordingSensorStore.hrm.value = value
                        } else {
                            RecordingSensorStore.hrm.value = 0f
                        }
                        RecordingSensorStore.hrmDebug.setValue(value, sensorEvent.accuracy)
                    } else {
                        RecordingSensorStore.hrmDebug.setValue(0f, -3)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
            }
            sensorManager?.registerListener(
                sensorEventListener, hrm,
                if (isRestart) SensorManager.SENSOR_DELAY_NORMAL else 1400000
            )
        }
        return true
    }

    fun stopHrSensor(ctx: Context) {
        if (sensorEventListener == null) return
        val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            Logger.logE(TAG, "Failed to get sensorManager in stopHrSensor(). ")
        } else {
            Logger.logD(TAG, "Removing HRM listener")
            synchronized(this) {
                if (sensorEventListener != null) {
                    sensorManager.unregisterListener(sensorEventListener)
                    sensorEventListener = null
                }
            }
        }
    }

    fun destroy(ctx: Context) {
        stopHrSensor(ctx)
    }

    companion object {

        private const val TAG = "BodySensorsManager"

        private val HR_REQUEST_CODE = abs(Manifest.permission.BODY_SENSORS.hashCode())

        fun checkBodySensorsPermission(ctx: Context): Boolean {
            return (ContextCompat.checkSelfPermission(ctx, Manifest.permission.BODY_SENSORS)
                    == PackageManager.PERMISSION_GRANTED)
        }

        fun checkAndRequestBodySensorsPermission(ctx: Context): Boolean {
            return if (!checkBodySensorsPermission(ctx)) {
                PreferencesEx.hrmFeatureConfigState = FeatureConfigEnum.NO_PERMISSION
                if (ctx is Activity) {
                    ActivityCompat.requestPermissions(
                        ctx,
                        arrayOf(Manifest.permission.BODY_SENSORS),
                        HR_REQUEST_CODE
                    )
                }
                false
            } else {
                true
            }
        }

        /**
         * Should only be called if app has permission to read body sensors
         */
        fun recheckSensorAvailability(ctx: Context): FeatureConfigEnum {
            val currentState = PreferencesEx.hrmFeatureConfigState
            if (currentState !== FeatureConfigEnum.NOT_AVAILABLE) {
                Logger.logW(TAG, "recheckSensorAvailability(), called with state  FeatureConfigEnum.NOT_AVAILABLE ")
                // Other state than not available, do not check, should have not been called at all.
                return currentState
            }

            // get current state
            val newState = getHrmSensorState(ctx)
            if (newState === FeatureConfigEnum.ENABLED) {
                PreferencesEx.hrmFeatureConfigState = newState
            }
            return newState
        }

        fun handlePermissionResult(
            owner: LocusWearActivity,
            requestCode: Int
        ): FeatureConfigEnum {
            Logger.logD(TAG, "handlePermissionResult($owner, $requestCode)")
            if (requestCode == HR_REQUEST_CODE) {
                val newState = getHrmSensorState(owner)
                PreferencesEx.hrmFeatureConfigState = newState
                return newState
            }
            return FeatureConfigEnum.NOT_AVAILABLE
        }

        private fun getHrmSensorState(ctx: Context): FeatureConfigEnum {
            return try {
                // get reference to sensor
                var hearRateSensor: Sensor? = null
                val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                if (sensorManager != null) {
                    hearRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                }

                // check state
                if (hearRateSensor != null) {
                    if (checkBodySensorsPermission(ctx)) {
                        FeatureConfigEnum.ENABLED
                    } else {
                        FeatureConfigEnum.DISABLED
                    }
                } else {
                    FeatureConfigEnum.NOT_AVAILABLE
                }
            } catch (e: Exception) {
                Logger.logE(TAG, "getHrmSensorState($ctx)", e)
                FeatureConfigEnum.NOT_AVAILABLE
            }
        }
    }
}