package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Generic command containing single string as parameter
 * Created by Milan Cejnar on 21.11.2017.
 * Asamm Software, s.r.o.
 */

public class StringCommand extends TimeStampStorable {
	private String mParam;

	public StringCommand() {
		super();
	}

	public StringCommand(byte[] data) throws IOException {
		super(data);
	}

	public StringCommand(String param) {
		this();
		if (param != null) {
			mParam = param;
		}
	}


	@Override
	public void reset() {
		super.reset();
		mParam = "";
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		this.mParam = dr.readString();
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeString(mParam);
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	public String getParam() {
		return mParam;
	}
}
