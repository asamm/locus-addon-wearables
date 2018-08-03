/*
 * Created by milan on 03.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.model;

/** ID pair identifying unique positin of the TrackStatLayout in the TrackRecording activity */
public class TrackStatViewId {
    private final int screenIdx;
    private final int cellIdx;

    public TrackStatViewId(int screenIdx, int cellIdx) {
        this.screenIdx = screenIdx;
        this.cellIdx = cellIdx;
    }

    public int getScreenIdx() {
        return screenIdx;
    }

    public int getCellIdx() {
        return cellIdx;
    }
}
