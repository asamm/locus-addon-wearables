/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

/**
 * Blank screen with a sigle TextView with error message.
 * This screen should never display in normal operation (only present because of NPE prevention)
 */
public class TrackStatsScreenViewEmptyScreen implements TrackStatsScreenView {

    private final ViewGroup mLayout;
    private final TextView tvError;

    TrackStatsScreenViewEmptyScreen(ViewGroup contentView) {
        LayoutInflater inflater = LayoutInflater.from(contentView.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_stats_empty, contentView, false);
        tvError = mLayout.findViewById(R.id.text_view);
    }

    @Override
    public ViewGroup getInflatedLayout() {
        return mLayout;
    }

    @Override
    public void refreshConfiguration(TrackRecScreenConfigDto screenConfig) {
    }

    @Override
    public void handleNewTrackRecData(TrackRecordingValue data) {
    }

    @Override
    public void setAmbientMode(boolean isAmbient) {
        tvError.setTextColor(isAmbient ? mLayout.getContext().getColor(R.color.base_light_primary) : mLayout.getContext().getColor(R.color.base_dark_primary));
    }

}
