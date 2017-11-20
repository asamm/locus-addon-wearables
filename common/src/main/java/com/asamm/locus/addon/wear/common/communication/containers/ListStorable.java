package com.asamm.locus.addon.wear.common.communication.containers;

import java.io.IOException;
import java.util.List;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Simple wrapper for representing a list of storables as simple storable
 * <p>
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class ListStorable<T extends Storable> extends TimeStampStorable {
	private List<T> mStorables;

	public ListStorable() {
		super();
	}

	public ListStorable(byte[] data) throws IOException {
		super(data);
	}

	public abstract Class<T> getClazz();

	@Override
	public void reset() {
		super.reset();
		mStorables = null;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		// read track record profiles
		if (dr.readBoolean()) {
			//noinspection unchecked
			mStorables = (List<T>)
					dr.readListStorable(getClazz());
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		// write track record profiles
		if (mStorables != null) {
			dw.writeBoolean(true);
			dw.writeListStorable(mStorables);
		} else {
			dw.writeBoolean(false);
		}
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	public List<T> getStorables() {
		return mStorables;
	}

	public void setStorables(List<T> storables) {
		this.mStorables = storables;
	}


	public int getSize() {
		return (mStorables == null) ? 0 : mStorables.size();
	}
}
