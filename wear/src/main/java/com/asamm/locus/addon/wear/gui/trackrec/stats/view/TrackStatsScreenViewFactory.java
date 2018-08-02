package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

import android.view.ViewGroup;

import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecScreenConfigDto;

/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class TrackStatsScreenViewFactory {
    public static TrackStatsScreenView createViewForConfig(ViewGroup contentView,
                                                           TrackRecScreenConfigDto screenConfig) {
        switch (screenConfig.getScreenType()) {
            case STAT_SCREEN_R2_C2:
                return new TrackStatsScreenViewR2C2(contentView, screenConfig);
            default: {
                return new TrackStatsScreenViewEmptyScreen(contentView);
            }
        }
    }
}
