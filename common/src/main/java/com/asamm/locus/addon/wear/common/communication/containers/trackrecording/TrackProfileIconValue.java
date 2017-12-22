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
public class TrackProfileIconValue extends TimeStampStorable {

	// tag for logger
	private static final String TAG = TrackProfileIconValue.class.getSimpleName();

	private long mId;
	private byte[] mImg;

	/**
	 * Base constructor mainly for a Storable class.
	 */
	@SuppressWarnings("unused")
	public TrackProfileIconValue() {
		super();
	}

	public TrackProfileIconValue(ActionTools.TrackRecordProfileSimple simpleProfile) {
		this();
		mId = simpleProfile.getId();
		mImg = simpleProfile.getIcon();
	}

	public TrackProfileIconValue(long id, byte[] img) {
		this();
		this.mId = id;
		this.mImg = img;
	}

	/**
	 * Constructor based on raw byte array.
	 *
	 * @param data packed data
	 * @throws IOException
	 */
	public TrackProfileIconValue(byte[] data) throws IOException {
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
		mImg = null;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mId = dr.readLong();
		int imgSize = dr.readInt();
		if (imgSize > 0) {
			mImg = new byte[imgSize];
			dr.readBytes(mImg);
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeLong(mId);
		int imgSize = mImg != null ? mImg.length : 0;
		dw.writeInt(imgSize);
		if (imgSize > 0) {
			dw.write(mImg);
		}
	}

	public long getId() {
		return mId;
	}

	public byte[] getIcon() {
		return mImg;
	}

	public void setIcon(byte[] mImg) {
		this.mImg = mImg;
	}

	public static class ValueList extends ListStorable<TrackProfileIconValue> {
		public ValueList() {
			super();
		}

		public ValueList(byte[] data) throws IOException {
			super(data);
		}

		@Override
		public Class<TrackProfileIconValue> getClazz() {
			return TrackProfileIconValue.class;
		}
	}
}
