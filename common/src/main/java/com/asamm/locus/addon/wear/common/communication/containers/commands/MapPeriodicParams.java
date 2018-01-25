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
	private static final byte FLG_AUTOROTATE = 0x1;
	// version 0
	private int mZoom;
	private int mWidth;
	private int mHeight;

	// version 1
	/**
	 * Flags:
	 * 0x1 - AutoRotate
	 */
	private byte bFlags;
	private short mDiagonal;
	private int mOffsetX;
	private int mOffsetY;
	private short mDensityDpi;
	// nonzero when offset is used
	private double mLastLatitude;
	private double mLastLongitude;

	public MapPeriodicParams() {
		super();
	}

	public MapPeriodicParams(byte[] arr) throws IOException {
		super(arr);
	}

	public MapPeriodicParams(int mZoom, int mWidth, int mHeight,
							 int offsetX, int offsetY, int densityDpi,
							 boolean isAutoRotate, int diagonal,
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
	}

	@Override
	protected int getVersion() {
		return 1;
	}

	@Override
	public void reset() {
		super.reset();
		mZoom = 0;
		mWidth = 0;
		mHeight = 0;
		// version 1
		mOffsetX = 0;
		mOffsetY = 0;
		mDiagonal = 0;
		bFlags = 0;
		mLastLongitude = 0;
		mLastLatitude = 0;

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

}
