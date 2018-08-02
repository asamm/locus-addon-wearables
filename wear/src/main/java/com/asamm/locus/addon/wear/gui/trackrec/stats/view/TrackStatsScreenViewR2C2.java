package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class TrackStatsScreenViewR2C2 implements TrackStatsScreenView {
    private final ViewGroup mLayout;
    private TrackStatLayout mTopLeft, mTopRight, mBottomLeft, mBottomRight;
    private TrackRecordingValue lastData = null;

    TrackStatsScreenViewR2C2(ViewGroup contentView, TrackRecScreenConfigDto screenConfig) {
        LayoutInflater inflater = LayoutInflater.from(contentView.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_stats_r2_c2, contentView, false);
        mTopLeft = mLayout.findViewById(R.id.track_stats_top_left);
        mTopRight = mLayout.findViewById(R.id.track_stats_top_right);
        mBottomLeft = mLayout.findViewById(R.id.track_stats_bottom_left);
        mBottomRight = mLayout.findViewById(R.id.track_stats_bottom_right);
        refreshConfiguration(screenConfig);
    }

    @Override
    public ViewGroup getInflatedLayout() {
        return mLayout;
    }

    @Override
    public void refreshConfiguration(TrackRecScreenConfigDto screenConfig) {
        mTopRight.setType(screenConfig.getCellTypeAtIdx(0));
        mTopLeft.setType(screenConfig.getCellTypeAtIdx(1));
        mBottomLeft.setType(screenConfig.getCellTypeAtIdx(2));
        mBottomRight.setType(screenConfig.getCellTypeAtIdx(3));
        handleNewTrackRecData(lastData);
    }

    @Override
    public void handleNewTrackRecData(TrackRecordingValue newData) {
        if (newData == null) return;
        lastData = newData;
        mTopLeft.consumeNewStatistics(lastData);
        mTopRight.consumeNewStatistics(lastData);
        mBottomLeft.consumeNewStatistics(lastData);
        mBottomRight.consumeNewStatistics(lastData);
    }

    @Override
    public void setAmbientMode(boolean isAmbient) {
        mBottomLeft.setAmbientMode(isAmbient);
        mBottomRight.setAmbientMode(isAmbient);
        mTopLeft.setAmbientMode(isAmbient);
        mTopRight.setAmbientMode(isAmbient);
    }
}
