package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Command/request to start sending specifyied periodic data udpates
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class PeriodicCommand extends TimeStampStorable {

	private static final String TAG = "PeriodicCommand";
	public static final int PERIOD_STOP = 0;

	/**
	 * Activities supporting periodic messages
	 */
	public static final byte IDX_PERIODIC_STOP_ALL = 0;
	public static final byte IDX_PERIODIC_TRACK_RECORDING = 1;
	public static final byte IDX_PERIODIC_MAP = 2;

	private byte mPeriodicActivityId;
	private int mPeriodMs;
	private TimeStampStorable mExtra;

	public PeriodicCommand() {
		super();
	}

	public PeriodicCommand(byte[] arr) throws IOException {
		super(arr);
	}

	public PeriodicCommand(byte periodicActivityId, int periodMs) {
		this(periodicActivityId, periodMs, null);
	}

	public <E extends TimeStampStorable & PeriodicExtra> PeriodicCommand(byte periodicActivityId, int periodMs, E extra) {
		this();
		this.mPeriodicActivityId = periodicActivityId;
		this.mPeriodMs = periodMs;
		if (extra != null) {
			mExtra = extra;
		}
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	public byte getmPeriodicActivityId() {
		return mPeriodicActivityId;
	}

	public int getmPeriodMs() {
		return mPeriodMs;
	}

	public boolean isStopRequest() {
		return mPeriodMs == PERIOD_STOP;
	}

	public <E extends TimeStampStorable> E getExtra() {
		return (E) mExtra;
	}

	@Override
	public void reset() {
		super.reset();
		mPeriodicActivityId = IDX_PERIODIC_STOP_ALL;
		mPeriodMs = PERIOD_STOP;
		mExtra = null;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mPeriodicActivityId = dr.readBytes(1)[0];
		mPeriodMs = dr.readInt();
		if (dr.readBoolean()) {
			try {
				if (mPeriodicActivityId == IDX_PERIODIC_MAP) {
					mExtra = (MapPeriodicParams) dr.readStorable(MapPeriodicParams.class);
				} else {
					mExtra = (TimeStampStorable) dr.readStorable(TimeStampStorable.class);
				}
			} catch (Exception e) {
				Logger.logE(TAG, "Failed to instantiate extra data.", e);
				mExtra = null;
			}
		} else {
			mExtra = null;
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.write(mPeriodicActivityId);
		dw.writeInt(mPeriodMs);
		boolean serializeExtra = mExtra != null
				&& mPeriodicActivityId == IDX_PERIODIC_MAP;
		dw.writeBoolean(serializeExtra);
		if (serializeExtra) {
			dw.writeStorable(mExtra);

		}
	}

	public static PeriodicCommand createStopPeriodicUpdatesCommand() {
		return new PeriodicCommand(IDX_PERIODIC_STOP_ALL, PERIOD_STOP);
	}

	/**
	 * Tagging interface which designates containers meant as Periodic command extra
	 */
	public interface PeriodicExtra {
	}

}
