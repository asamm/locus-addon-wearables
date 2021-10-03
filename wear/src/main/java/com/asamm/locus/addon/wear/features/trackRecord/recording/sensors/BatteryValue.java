/*
 * Created by milan on 06.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord.recording.sensors;

/** Simple container to store Battery level value */
public class BatteryValue extends SensorValue{
    /** Battery level in [%] */
    private int value;
    public static final int INVALID_VALUE = -1;

    public BatteryValue() {
        this.value = INVALID_VALUE;
        this.timestamp = System.currentTimeMillis();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        timestamp = System.currentTimeMillis();
        this.value = value;
    }

    @Override
    public boolean isValid() {
        return value >= 0 && value <= 100;
    }
}
