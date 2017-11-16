package com.assam.locus.addon.wear.common.communication.containers.commands;

import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */

public class MapPeriodicParams extends TimeStampStorable implements PeriodicCommand.PeriodicExtra {
    private double mLon;
    private double mLat;
    private int mZoom;
    private int mWidth;
    private int mHeight;

    public MapPeriodicParams() {
        super();
    }
    public MapPeriodicParams(byte[] arr) throws IOException{
        super(arr);
    }

    public MapPeriodicParams(double mLon, double mLat, int mZoom, int mWidth, int mHeight) {
        this.mLon = mLon;
        this.mLat = mLat;
        this.mZoom = mZoom;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        super.reset();
        mLon = 0;
        mLat = 0;
        mZoom = 0;
        mWidth = 0;
        mHeight = 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        mLon = dr.readDouble();
        mLat = dr.readDouble();
        mZoom = dr.readInt();
        mWidth = dr.readInt();
        mHeight = dr.readInt();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeDouble(mLon);
        dw.writeDouble(mLat);
        dw.writeInt(mZoom);
        dw.writeInt(mWidth);
        dw.writeInt(mHeight);
    }

    public double getLon() {
        return mLon;
    }

    public double getLat() {
        return mLat;
    }

    public int getZoom() {
        return mZoom;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
