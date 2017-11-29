package com.asamm.locus.addon.wear.common.communication.containers.trackrecording;

import com.asamm.locus.addon.wear.common.communication.containers.ListStorable;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.android.ActionTools;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class TrackProfileInfoValue extends TimeStampStorable {

	// tag for logger
	private static final String TAG = TrackProfileInfoValue.class.getSimpleName();

	private long mId;
	private String mName;
	private String mDesc; // Always empty in this version

	/**
	 * Base constructor mainly for a Storable class.
	 */
	@SuppressWarnings("unused")
	public TrackProfileInfoValue() {
		super();
	}

	public TrackProfileInfoValue(long id, String name) {
		this();
		mId = id;
		mName = name;
	}

	public TrackProfileInfoValue(ActionTools.TrackRecordProfileSimple simpleProfile) {
		this();
		mId = simpleProfile.getId();
		mName = simpleProfile.getName();
		//mDesc = simpleProfile.getDesc(); // not used in this version
	}

	/**
	 * Constructor based on raw byte array.
	 *
	 * @param data packed data
	 * @throws IOException
	 */
	public TrackProfileInfoValue(byte[] data) throws IOException {
		super(data);
	}


	/**************************************************/
	// STORABLE PART

	/**************************************************/

	@Override
	protected int getVersion() {
		return 0;
	}

	@Override
	public void reset() {
		super.reset();
		mId = 0L;
		mName = "";
		mDesc = "";
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mId = dr.readLong();
		mName = dr.readString();
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeLong(mId);
		dw.writeString(mName);
	}

	public long getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public String getDesc() {
		return mDesc;
	}

	public static class ValueList extends ListStorable<TrackProfileInfoValue> {
		public ValueList() {
			super();
		}

		public ValueList(byte[] data) throws IOException {
			super(data);
		}

		@Override
		public Class<TrackProfileInfoValue> getClazz() {
			return TrackProfileInfoValue.class;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TrackProfileInfoValue that = (TrackProfileInfoValue) o;

		if (mId != that.mId) return false;
		return mName != null ? mName.equals(that.mName) : that.mName == null;
	}

	@Override
	public int hashCode() {
		return (int) (mId ^ (mId >>> 32));
	}
}
