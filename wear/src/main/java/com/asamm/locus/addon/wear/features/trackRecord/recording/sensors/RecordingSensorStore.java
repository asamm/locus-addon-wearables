package com.asamm.locus.addon.wear.features.trackRecord.recording.sensors;

import android.content.Context;
import android.os.BatteryManager;

import com.asamm.locus.addon.wear.MainApplication;

import locus.api.utils.Logger;

import static android.content.Context.BATTERY_SERVICE;

/*
 * Created by milan on 03.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class RecordingSensorStore {
    public static volatile HrmDebugValue hrmDebug = new HrmDebugValue();
    public static volatile HrmValue hrm = new HrmValue();
    private static volatile BatteryValue battery = new BatteryValue();

    public static BatteryValue getBatteryValue() {
        if (!battery.isValid() || System.currentTimeMillis() - battery.timestamp > 30_000) {
            try {
                Context ctx = MainApplication.applicationContext;
                if (ctx != null) {
                    BatteryManager bm = (BatteryManager) ctx.getSystemService(BATTERY_SERVICE);
                    int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    battery.setValue(batLevel);
                }

            } catch (Exception e) {
                Logger.INSTANCE.logE("RecordingSensorStore", "Battery level read failed", e);
                battery.setValue(BatteryValue.INVALID_VALUE);
            }
        }
        return battery;
    }
}
