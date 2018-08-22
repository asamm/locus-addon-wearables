/*
 * Created by milan on 06.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

/** Simple container to store debug HRM value */
public class HrmDebugValue extends SensorValue{
    /** HRM value in BPM */
    volatile private float value;
    volatile private int accuracy;


    public HrmDebugValue() {
        this.value = Float.NaN;
        this.timestamp = System.currentTimeMillis();
        accuracy = -2;
    }

    public float getHrmValue() {
        return value;
    }
    public int getHrmAccuracy() {
        return accuracy;
    }

    public void setValue(float value, int accuracy) {
        timestamp = System.currentTimeMillis();
        this.value = value;
        this.accuracy = accuracy;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
