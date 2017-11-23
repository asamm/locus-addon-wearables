package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 23.11.2017.
 * Asamm Software, s.r.o.
 */

public class ProfileIconGetCommand extends TimeStampStorable{

	private long mProfileId;

	public ProfileIconGetCommand() {
		super();
	}

	public ProfileIconGetCommand(byte[] data) throws IOException {
		super(data);
	}

	public ProfileIconGetCommand(long profileId) {
		this();
		mProfileId = profileId;
	}

	@Override
	protected int getVersion() {
		return 0;
	}

	@Override
	public void reset() {
		super.reset();
		mProfileId = -1;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mProfileId = dr.readLong();
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeLong(mProfileId);
	}

	public long getProfileId() {
		return mProfileId;
	}
}
