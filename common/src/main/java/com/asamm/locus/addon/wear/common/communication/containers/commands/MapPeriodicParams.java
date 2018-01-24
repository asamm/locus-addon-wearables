package com.asamm.locus.addon.wear.common.communication.containers.commands;

import android.location.Location;

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
	// version 0
	private int mZoom;
	private int mWidth;
	private int mHeight;

	// version 1
	private boolean mAutoRotate;
	private int mOffsetX;
	private int mOffsetY;
	private int mDensityDpi;
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
							 int offsetX, int offsetY, int densityDpi, boolean isAutoRotate,
							 double lastOffsetLatitude, double lastOffsetLongitude) {
		this.mZoom = mZoom;
		this.mWidth = mWidth;
		this.mHeight = mHeight;
		this.mAutoRotate = isAutoRotate;
		this.mOffsetX = offsetX;
		this.mOffsetY = offsetY;
		this.mDensityDpi = densityDpi;
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
		mDensityDpi = 0;
		mAutoRotate = false;
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
			mDensityDpi = dr.readInt();
			mAutoRotate = dr.readBoolean();
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
		dw.writeInt(mDensityDpi);
		dw.writeBoolean(mAutoRotate);
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
		return mAutoRotate;
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

	public void setLastLatitude(double lastLatitude) {
		mLastLatitude = lastLatitude;
	}

	public double getLastLongitude() {
		return mLastLongitude;
	}

	public void setLastLongitude(double lastLongitude) {
		mLastLongitude = lastLongitude;
	}
}
