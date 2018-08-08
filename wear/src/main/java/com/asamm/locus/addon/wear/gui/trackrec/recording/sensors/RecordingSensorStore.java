package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

import android.content.Context;
import android.os.BatteryManager;

import com.asamm.locus.addon.wear.MainApplication;

import static android.content.Context.BATTERY_SERVICE;

/*
 * Created by milan on 03.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class RecordingSensorStore {
    public static HrmValue hrm = new HrmValue();
    private static BatteryValue battery = new BatteryValue();

    public static BatteryValue getBatteryValue() {
        if (!battery.isValid() || System.currentTimeMillis() - battery.timestamp > 15_000) {
            // TODO cejnar debug output
            Context ctx = MainApplication.applicationContext;
            if (ctx != null) {
                locus.api.utils.Logger.logD("RecordingSensorStore", "updating battery info");
                BatteryManager bm = (BatteryManager) ctx.getSystemService(BATTERY_SERVICE);
                int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                battery.setValue(batLevel);
            }
        }
        return battery;
    }
}
