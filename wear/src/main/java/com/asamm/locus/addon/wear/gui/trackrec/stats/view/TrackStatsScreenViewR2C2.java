/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

import java.util.Arrays;

/**
 * Implementation of a view for 2 by 2 grid statistics screen
 */
public class TrackStatsScreenViewR2C2 extends TrackStatsScreenViewBase {

    private final ViewGroup mLayout;
    private final TrackStatLayout[] statLayouts;
    private TrackStatLayout mTopLeft, mTopRight, mBottomLeft, mBottomRight;
    private TrackRecordingValue lastData = null;

    TrackStatsScreenViewR2C2(ViewGroup contentView, TrackRecScreenConfigDto screenConfig) {
        LayoutInflater inflater = LayoutInflater.from(contentView.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_stats_r2_c2, contentView, false);
        mTopLeft = mLayout.findViewById(R.id.track_stats_top_left);
        mTopRight = mLayout.findViewById(R.id.track_stats_top_right);
        mBottomLeft = mLayout.findViewById(R.id.track_stats_bottom_left);
        mBottomRight = mLayout.findViewById(R.id.track_stats_bottom_right);
        statLayouts = Arrays.asList(mTopRight, mTopLeft, mBottomLeft, mBottomRight).toArray(new TrackStatLayout[4]);
        refreshConfiguration(screenConfig);
    }

    @Override
    public ViewGroup getInflatedLayout() {
        return mLayout;
    }

    @Override
    protected TrackStatLayout[] getStatLayouts() {
        return statLayouts;
    }
}
