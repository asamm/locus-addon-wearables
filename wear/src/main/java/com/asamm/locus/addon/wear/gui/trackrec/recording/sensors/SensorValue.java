package com.asamm.locus.addon.wear.gui.trackrec.recording.sensors;

/*
 * Created by milan on 06.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public abstract class SensorValue {
    /**
     * timestamp of last change of this data
     */
    volatile protected long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    abstract public boolean isValid();
}
