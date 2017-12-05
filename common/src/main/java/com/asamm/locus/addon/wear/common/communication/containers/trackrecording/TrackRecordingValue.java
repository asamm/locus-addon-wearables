package com.asamm.locus.addon.wear.common.communication.containers.trackrecording;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.android.utils.LocusInfo;
import locus.api.objects.Storable;
import locus.api.objects.extra.TrackStats;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 13.11.2017.
 * Asamm Software, s.r.o.
 */

public class TrackRecordingValue extends TimeStampStorable {
	private static final String TAG = TrackRecordingValue.class.getSimpleName();

	private boolean infoAvailable;
	// is track record enabled
	private boolean trackRecRecording;
	// if track record is enabled, is running or paused
	private boolean trackRecPaused;
	// name of active track recording profile
	private String trackRecProfileName;
	// complete track statistics
	private TrackStats trackRecStats;

	/** various other data relevant for track recording */
	private ExtendedTrackInfo mExtendedTrackInfo;

	private byte mUnitsPacked1;
	private byte mUnitsPacked2;
	private byte mUnitsPacked3;

	public TrackRecordingValue() {
		super();
	}

	public TrackRecordingValue(byte[] arr) throws IOException {
		super(arr);
	}

	public TrackRecordingValue(boolean infoAvailable, boolean isTrackRecording, boolean isPaused,
							   String trackRecProfileName, TrackStats trackStats, LocusInfo info,
							   ExtendedTrackInfo ext) {
		super();
		this.infoAvailable = infoAvailable;
		this.trackRecRecording = isTrackRecording;
		this.trackRecPaused = isPaused;
		this.trackRecProfileName = trackRecProfileName;
		this.trackRecStats = trackStats;
		if (info != null) {
			fillUnitsFormatInfo(info);
		}
		this.mExtendedTrackInfo = ext != null ? ext : new ExtendedTrackInfo();
	}
	// TODO cejnar test serialization & units packing
	private void fillUnitsFormatInfo(LocusInfo info) {
		mUnitsPacked1 = (byte) (info.getUnitsFormatTemperature()
				| info.getUnitsFormatSlope() << 2
				| info.getUnitsFormatEnergy() << 4
				| info.getUnitsFormatAltitude() << 6);
		mUnitsPacked2 = (byte) (info.getUnitsFormatLength() | info.getUnitsFormatArea() << 4);
		mUnitsPacked3 = (byte) (info.getUnitsFormatAngle()
				| info.getUnitsFormatWeight() << 3
				| info.getUnitsFormatSpeed() << 5);
	}

	public int getUnitsFormatAltitude() {
		return mUnitsPacked1 >>> 6 & 0x3;
	}

	public int getUnitsFormatAngle() {
		return mUnitsPacked3 & 0x7;
	}

	public int getUnitsFormatArea() {
		return mUnitsPacked2 >> 4 & 0xF;
	}

	public int getUnitsFormatEnergy() {
		return mUnitsPacked1 >>> 4 & 0x3;
	}

	public int getUnitsFormatLength() {
		return mUnitsPacked2 & 0xF;
	}

	public int getUnitsFormatSlope() {
		return mUnitsPacked1 >>> 2 & 0x3;
	}

	public int getUnitsFormatSpeed() {
		return mUnitsPacked3 >>> 5 & 0x7;
	}

	public int getUnitsFormatTemperature() {
		return mUnitsPacked1 & 0x3;
	}

	public int getUnitsFormatWeight() {
		return mUnitsPacked3 >>> 3 & 0x3;
	}

	public float getSpeed() {
		return mExtendedTrackInfo.mSpeed;
	}

	@Override
	public void reset() {
		super.reset();
		trackRecRecording = false;
		trackRecPaused = false;
		trackRecProfileName = "";
		trackRecStats = null;
		mUnitsPacked1 = 0;
		mUnitsPacked2 = 0;
		mUnitsPacked3 = 0;
		mExtendedTrackInfo = new ExtendedTrackInfo();
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		reset();
		super.readObject(version, dr);
		infoAvailable = dr.readBoolean();
		trackRecRecording = dr.readBoolean();
		if (trackRecRecording) {
			trackRecPaused = dr.readBoolean();
			trackRecProfileName = dr.readString();
			byte[] unitsPacked = dr.readBytes(3);
			mUnitsPacked1 = unitsPacked[0];
			mUnitsPacked2 = unitsPacked[1];
			mUnitsPacked3 = unitsPacked[2];
			try {
				mExtendedTrackInfo = (ExtendedTrackInfo) dr.readStorable(ExtendedTrackInfo.class);
			} catch (Exception e) {
				Logger.logE(TAG, "Failed to read extended data", e);
				mExtendedTrackInfo = new ExtendedTrackInfo();
			}
			boolean isStats = dr.readBoolean();
			try {
				trackRecStats = isStats ? ((TrackStats) dr.readStorable(TrackStats.class)) : null;
			} catch (Exception e) {
				Logger.logE(TAG, "Failed to read trackStats", e);
				trackRecStats = null;
			}
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeBoolean(infoAvailable);
		dw.writeBoolean(trackRecRecording);

		if (trackRecRecording) {
			dw.writeBoolean(trackRecPaused);
			dw.writeString(trackRecProfileName);
			dw.write(new byte[] {mUnitsPacked1, mUnitsPacked2, mUnitsPacked3});
			dw.writeStorable(mExtendedTrackInfo);
			dw.writeBoolean(trackRecStats != null);
			if (trackRecStats != null) {
				dw.writeStorable(trackRecStats);
			}
		}
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	public boolean isInfoAvailable() {
		return infoAvailable;
	}

	public boolean isTrackRecRecording() {
		return trackRecRecording;
	}

	public boolean isTrackRecPaused() {
		return trackRecPaused;
	}

	public String getTrackRecProfileName() {
		return trackRecProfileName;
	}

	public TrackStats getTrackRecStats() {
		return trackRecStats;
	}

	public TrackRecordingStateEnum getTrackRecordingState() {
		return trackRecPaused ? TrackRecordingStateEnum.PAUSED :
				trackRecRecording ? TrackRecordingStateEnum.RECORDING :
						TrackRecordingStateEnum.NOT_RECORDING;
	}

	public static class ExtendedTrackInfo extends Storable {
		private static final float INVALID_SPEED = -1f;
		private float mSpeed;

		public ExtendedTrackInfo() {
			super();
		}

		public ExtendedTrackInfo(byte[] data) throws IOException {
			super(data);
		}

		public ExtendedTrackInfo(Float speed) {
			this();
			mSpeed = speed == null ? INVALID_SPEED : speed;
		}

		@Override
		protected int getVersion() {
			return 0;
		}

		@Override
		public void reset() {
			mSpeed = INVALID_SPEED;
		}

		@Override
		protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
			mSpeed = dr.readFloat();
		}

		@Override
		protected void writeObject(DataWriterBigEndian dw) throws IOException {
			dw.writeFloat(mSpeed);
		}
	}
}
