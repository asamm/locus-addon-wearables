/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */

package com.asamm.locus.addon.wear.features.trackRecord.stats.model;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Storable container holding statistics configuration for a single screen.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class TrackRecScreenConfigDto extends Storable {

    private List<TrackRecCellConfigDto> cellConfigs = new ArrayList<>(4);
    private TrackStatsViewScreenType screenType = TrackStatsViewScreenType.STAT_SCREEN_BLANK;

    public TrackRecScreenConfigDto() {
        super();
    }

    public TrackRecScreenConfigDto(TrackStatsViewScreenType screenType, List<TrackRecCellConfigDto> cellConfigs) {
        this();
        this.screenType = screenType;
        this.cellConfigs = cellConfigs;
    }

    public TrackRecScreenConfigDto(byte[] data) throws IOException {
        super();
        read(data);
    }

    public TrackStatsViewScreenType getScreenType() {
        return screenType;
    }

    public TrackStatTypeEnum getCellTypeAtIdx(int cellIdx) {
        if (cellIdx < 0 || cellIdx >= cellConfigs.size()) {
            return TrackStatTypeEnum.BLANK;
        } else {
            return cellConfigs.get(cellIdx).getContentType();
        }
    }

    public void setCellTypeAtIdx(int cellIdx, TrackStatTypeEnum newType) {
        if (cellIdx >= 0 && cellIdx < cellConfigs.size()) {
            cellConfigs.get(cellIdx).setContentType(newType);
        }
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        screenType = TrackStatsViewScreenType.getById(dr.readBytes(1)[0]);
        cellConfigs = dr.readListStorable(TrackRecCellConfigDto.class);
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        dw.write(screenType.getId());
        dw.writeListStorable(cellConfigs);
    }

    public static TrackRecScreenConfigDto createEmptyScreenConfig() {
        return new TrackRecScreenConfigDto();
    }
}