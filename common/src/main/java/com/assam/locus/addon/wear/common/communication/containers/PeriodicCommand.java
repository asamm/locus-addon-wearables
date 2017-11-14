package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class PeriodicCommand extends TimeStampStorable {

    public static final int PERIOD_STOP = 0;

    /** Activities supporting periodic messages */
    public static final byte IDX_PERIODIC_ACITIVY_STOP_ALL = 0;
    public static final byte IDX_PERIODIC_ACITIVITY_TRACK_RECORDING = 1;
    public static final byte IDX_PERIODIC_ACITIVITY_MAP = 2;

    private byte mPeriodicActivityId;
    private int mPeriodMs;

    public PeriodicCommand() {
        super();
    }
    public PeriodicCommand(byte[] arr)throws IOException {
        super(arr);
    }
    public PeriodicCommand (byte periodicActivityId, int periodMs) {
        this.mPeriodicActivityId = periodicActivityId;
        this.mPeriodMs = periodMs;
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

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        mPeriodicActivityId = dr.readBytes(1)[0];
        mPeriodMs = dr.readInt();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.write(mPeriodicActivityId);
        dw.writeInt(mPeriodMs);
    }

    public static PeriodicCommand createStopPeriodicUpdatesCommand(){
        return new PeriodicCommand(IDX_PERIODIC_ACITIVY_STOP_ALL, PERIOD_STOP);
    }

}
