/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.model;

/**
 * Enum describing available statistics screen layouts
 * Notation #Rows_#Columns
 */
public enum TrackStatsViewScreenType {
    /**
     * Should never be a part of active configuration, used as safe fallback instead of null
     */
    STAT_SCREEN_BLANK((byte) 0, 0),
    /**
     * Screen with 2 rows and 2 columns
     */
    STAT_SCREEN_R2_C2((byte) 1, 4),
    /**
     * Screen with 2 rows and 1 column, also used for MainScreen which has only left half of screen available
     */
    STAT_SCREEN_R2_C1((byte) 2, 2);

    private byte id;
    /**
     * number of positions which are available to display data on this screen
     */
    private int positionsCount;

    TrackStatsViewScreenType(byte id, int positionsCount) {
        this.id = id;
        this.positionsCount = positionsCount;
    }

    public byte getId() {
        return id;
    }

    public int getPositionsCount() {
        return positionsCount;
    }

    public static TrackStatsViewScreenType getById(byte id) {
        TrackStatsViewScreenType[] tmpVals = values();
        for (TrackStatsViewScreenType it : tmpVals) {
            if (it.id == id) return it;
        }
        return STAT_SCREEN_BLANK;
    }
}
