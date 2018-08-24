/*
 * Created by milan on 06.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

/** Simple container to store HRM value */
public class HrmValue extends SensorValue{
    private static final int VALUE_TIMEOUT_MS = 7_000;
    /** HRM value in BPM */
    volatile private float value;


    public HrmValue() {
        this.value = Float.NaN;
        this.timestamp = System.currentTimeMillis();
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        timestamp = System.currentTimeMillis();
        this.value = value;
    }

    @Override
    public boolean isValid() {
        return isValidHrm(value) && System.currentTimeMillis() - timestamp < VALUE_TIMEOUT_MS ;
    }

    public static boolean isValidHrm(float value) {
        return value >= 25 && value <= 250;
    }
}
