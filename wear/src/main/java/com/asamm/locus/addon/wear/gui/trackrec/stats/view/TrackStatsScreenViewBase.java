/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

/**
 * Some common handling for implementations of TrackStatsScreenView
 */
abstract class TrackStatsScreenViewBase implements TrackStatsScreenView {
    protected TrackRecordingValue lastData = null;

    /**
     * @return in order array of inflated statistics views(cells)
     */
    protected abstract TrackStatLayout[] getStatLayouts();

    @Override
    public void setAmbientMode(boolean isAmbient) {
        for (TrackStatLayout layout : getStatLayouts()) {
            layout.setAmbientMode(isAmbient);
        }
    }

    @Override
    public void refreshConfiguration(TrackRecScreenConfigDto screenConfig) {
        int cellIdx = 0;
        for (TrackStatLayout layout : getStatLayouts()) {
            layout.setType(screenConfig.getCellTypeAtIdx(cellIdx++));
        }
        handleNewTrackRecData(lastData);
    }

    @Override
    public void handleNewTrackRecData(TrackRecordingValue newData) {
        if (newData == null) return;
        lastData = newData;
        for (TrackStatLayout layout : getStatLayouts()) {
            layout.consumeNewStatistics(lastData);
        }
    }

    protected void initLayoutsCommon(int screenIdx, TrackRecScreenConfigDto screenConfig) {
        int cellIdx = 0;
        for (TrackStatLayout layout : getStatLayouts()) {
            layout.setTrackStatViewPositionId(screenIdx, cellIdx++);
        }
        refreshConfiguration(screenConfig);
    }
}
