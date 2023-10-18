/*
 * Created by milan on 03.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord.recording.sensors

import android.content.Context
import android.os.BatteryManager
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.logger.Logger

object RecordingSensorStore {

    @JvmField
    @Volatile
    var hrmDebug = HrmDebugValue()

    @JvmField
    @Volatile
    var hrm = HrmValue()

    private val battery = BatteryValue()

    @JvmStatic
    val batteryValue: BatteryValue
        get() {
            if (!battery.isValid || System.currentTimeMillis() - battery.timestamp > 30000) {
                try {
                    val bm = MainApplication.app
                            .getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    battery.value = batLevel
                } catch (e: Exception) {
                    Logger.e(e, "RecordingSensorStore", "Battery level read failed")
                    battery.value = BatteryValue.INVALID_VALUE
                }
            }
            return battery
        }
}