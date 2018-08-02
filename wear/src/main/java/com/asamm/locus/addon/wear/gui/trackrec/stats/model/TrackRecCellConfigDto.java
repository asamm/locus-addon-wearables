/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.model;

import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecStatTypeEnum;

import java.io.IOException;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Configuration for a single statistics cell(view)
 */
public class TrackRecCellConfigDto extends Storable {
    /**
     * position(cell) index marking the position at the given statistics screen
     */
    private byte positionIdx;
    private TrackRecStatTypeEnum contentType;

    public TrackRecCellConfigDto(byte positionIdx, TrackRecStatTypeEnum contentType) {
        this();
        this.positionIdx = positionIdx;
        this.contentType = contentType;
    }

    public TrackRecCellConfigDto() {
        super();
    }

    public TrackRecCellConfigDto(byte[] data) throws IOException {
        super(data);
    }

    public byte getPositionIdx() {
        return positionIdx;
    }

    public TrackRecStatTypeEnum getContentType() {
        return contentType;
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        positionIdx = 0;
        contentType = TrackRecStatTypeEnum.BLANK;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        byte[] bytes = dr.readBytes(2);
        positionIdx = bytes[0];
        contentType = TrackRecStatTypeEnum.getById(bytes[1]);
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        dw.write(positionIdx);
        dw.write(contentType.getId());
    }
}
