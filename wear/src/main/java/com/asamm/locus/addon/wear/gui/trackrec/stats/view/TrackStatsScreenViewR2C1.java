/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

import java.util.Arrays;

/**
 * Implementation of a view for 2 by 1 grid statistics screen
 */
public class TrackStatsScreenViewR2C1 extends TrackStatsScreenViewBase {
    private final ViewGroup mLayout;
    private final TrackStatLayout[] statLayouts;
    private TrackStatLayout mTop, mBottom;

    TrackStatsScreenViewR2C1(ViewGroup contentView, int screenIdx, TrackRecScreenConfigDto screenConfig) {
        LayoutInflater inflater = LayoutInflater.from(contentView.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_stats_r2_c1, contentView, false);
        mTop = mLayout.findViewById(R.id.track_stats_top);
        mBottom = mLayout.findViewById(R.id.track_stats_bottom);
        statLayouts = Arrays.asList(mTop, mBottom).toArray(new TrackStatLayout[2]);
        initLayoutsCommon(screenIdx, screenConfig);
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
