package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

import android.app.ApplicationErrorReport;

import com.asamm.locus.addon.wear.common.utils.Pair;

/*
 * Created by milan on 03.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class RecordingSensorStore {
    public static HrmValue hrm = new HrmValue();
    /** TODO cejnar
     * BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
     int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
     * */
    public static BatteryValue battery = new BatteryValue();
}
