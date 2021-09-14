package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Paramaters specifying size and zoom of requested map
 * <p>
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */

public class MapPeriodicParams extends TimeStampStorable implements PeriodicCommand.PeriodicExtra {
    /**
     * code for mBearing property which indicates that device is free to use its last known bearing
     */
    public static final short APPLY_DEVICE_BEARING = Short.MIN_VALUE;
    private static final byte FLG_AUTOROTATE = 0x1;
    // version 0
    private int mZoom = 0;
    private int mWidth = 0;
    private int mHeight = 0;

    // version 1
    /**
     * Flags:
     * 0x1 - AutoRotate
     */
    private byte bFlags = 0;
    private short mBearing;
    private short mDiagonal = 0;
    private int mOffsetX = 0;
    private int mOffsetY = 0;
    private short mDensityDpi;
    // nonzero when offset is used
    private double mLastLatitude = 0;
    private double mLastLongitude = 0;

    public MapPeriodicParams() {
        super();
    }

    public MapPeriodicParams(int mZoom, int mWidth, int mHeight,
                             int offsetX, int offsetY, int densityDpi,
                             boolean isAutoRotate, short bearing, int diagonal,
                             double lastOffsetLatitude, double lastOffsetLongitude) {
        this.mZoom = mZoom;
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.bFlags = isAutoRotate ? FLG_AUTOROTATE : 0;
        this.mOffsetX = offsetX;
        this.mOffsetY = offsetY;
        this.mDensityDpi = (short) densityDpi;
        this.mDiagonal = (short) diagonal;
        this.mLastLatitude = lastOffsetLatitude;
        this.mLastLongitude = lastOffsetLongitude;
        this.mBearing = bearing;
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        mZoom = dr.readInt();
        mWidth = dr.readInt();
        mHeight = dr.readInt();

        if (version >= 1) {
            mOffsetX = dr.readInt();
            mOffsetY = dr.readInt();
            mDensityDpi = dr.readShort();
            mDiagonal = dr.readShort();
            bFlags = dr.readBytes(1)[0];
            mLastLatitude = dr.readDouble();
            mLastLongitude = dr.readDouble();
            mBearing = dr.readShort();
        }
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeInt(mZoom);
        dw.writeInt(mWidth);
        dw.writeInt(mHeight);

        // version 1
        dw.writeInt(mOffsetX);
        dw.writeInt(mOffsetY);
        dw.writeShort(mDensityDpi);
        dw.writeShort(mDiagonal);
        dw.write(bFlags);
        dw.writeDouble(mLastLatitude);
        dw.writeDouble(mLastLongitude);
        dw.writeShort(mBearing);
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

    public boolean isAutoRotate() {
        return (bFlags & FLG_AUTOROTATE) != 0;
    }

    public int getDiagonal() {
        return mDiagonal;
    }

    public int getOffsetX() {
        return mOffsetX;
    }

    public int getOffsetY() {
        return mOffsetY;
    }

    public int getDensityDpi() {
        return mDensityDpi;
    }

    public double getLastLatitude() {
        return mLastLatitude;
    }

    public double getLastLongitude() {
        return mLastLongitude;
    }

    public short getBearing() {
        return mBearing;
    }
}
