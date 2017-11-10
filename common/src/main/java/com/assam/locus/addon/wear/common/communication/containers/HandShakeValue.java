package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class HandShakeValue extends TimeStampStorable {
    int mLocusVersion;
    byte mStatusFlag;

    private static final byte STATUS_LOCUS_RUNNING = 0b1;
    private static final byte STATUS_PERIODIC_UPDATES = 0b10;

    public HandShakeValue() {
        super();
    }

    public HandShakeValue(byte[] data) throws IOException {
        super(data);
    }

    public HandShakeValue(int locusVersion, boolean locusRunning, boolean periodicUpdatesEnabled) {
        this();
        mLocusVersion = locusVersion;
        setLocusRunning(locusRunning);
        setPeriodicUpdates(periodicUpdatesEnabled);
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        super.reset();
        mLocusVersion = -1;
        mStatusFlag = 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        mLocusVersion = dr.readInt();
        mStatusFlag = dr.readBytes(1)[0];
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeInt(mLocusVersion);
        dw.write(mStatusFlag);
        int i = 0;
    }

    public int getmLocusVersion() {
        return mLocusVersion;
    }

    public boolean isLocusRunning() {
        return (mStatusFlag & STATUS_LOCUS_RUNNING) != 0;
    }

    public void setLocusRunning(boolean b) {
        setFlag(b, STATUS_LOCUS_RUNNING);
    }

    public boolean isPeriodicUpdates() {
        return (mStatusFlag & STATUS_PERIODIC_UPDATES) != 0;
    }

    public void setPeriodicUpdates(boolean b) {
        setFlag(b, STATUS_PERIODIC_UPDATES);
    }

    private void setFlag(boolean b, byte flag) {
        if (b) {
            mStatusFlag |= flag;
        } else {
            mStatusFlag &= ~flag;
        }
    }

    public boolean isEmpty() {
        return mLocusVersion < 0;
    }

}
