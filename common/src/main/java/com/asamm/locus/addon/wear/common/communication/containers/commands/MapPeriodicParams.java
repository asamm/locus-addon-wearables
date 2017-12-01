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
	private int mZoom;
	private int mWidth;
	private int mHeight;

	public MapPeriodicParams() {
		super();
	}

	public MapPeriodicParams(byte[] arr) throws IOException {
		super(arr);
	}

	public MapPeriodicParams(int mZoom, int mWidth, int mHeight) {
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
		mZoom = 0;
		mWidth = 0;
		mHeight = 0;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mZoom = dr.readInt();
		mWidth = dr.readInt();
		mHeight = dr.readInt();
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeInt(mZoom);
		dw.writeInt(mWidth);
		dw.writeInt(mHeight);
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
