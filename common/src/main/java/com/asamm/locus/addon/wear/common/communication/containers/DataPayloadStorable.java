package com.asamm.locus.addon.wear.common.communication.containers;

import com.asamm.locus.addon.wear.common.communication.DataPath;

import java.io.IOException;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Simple Path + Data wrapper to replace DataItem when replacing Data API with Message API calls.
 * Created by Milan Cejnar on 08.03.2018.
 * Asamm Software, s.r.o.
 */

public class DataPayloadStorable extends Storable {
	private DataPath mDataPath;
	private Storable mStorable;

	public DataPayloadStorable(DataPath path, Storable data) {
		super();
		this.mDataPath = path;
		this.mStorable = data;
	}
	public DataPayloadStorable(byte[] data) throws IOException{
		super(data);
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	@Override
	public void reset() {
		mDataPath = null;
		mStorable = null;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		byte mDataPathId = dr.readBytes(1)[0];
		mDataPath = DataPath.valueOf(mDataPathId);
		if (mDataPath != null)
			try {
				mStorable = dr.readStorable(mDataPath.getContainerClass());
			} catch (Exception e) {
				reset();
				e.printStackTrace();
			}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		if (isValid()) {
			dw.write(mDataPath.getId());
			dw.writeStorable(mStorable);
		} else {
			dw.write(Byte.MIN_VALUE);
		}
	}

	public boolean isValid() {
		return mStorable != null && mDataPath != null;
	}

	public DataPath getDataPath() {
		return mDataPath;
	}

	public <E extends Storable> E getData(Class<E> clazz) {
		return (E) mStorable;
	}

}
