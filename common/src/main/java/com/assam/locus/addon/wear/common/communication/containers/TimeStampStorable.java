package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * This Storable class contains cropped 4B long timestamp which ensures that command are always sent when
 * using DataApi DataItem for transfer. (Sending the same dataItem twice doesn't trigger data changed listener)
 * <p>
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public abstract class TimeStampStorable extends Storable {
    public TimeStampStorable() {
        super();
    }

    /**
     * cropped 4B timestampt
     */
    private int mIntTimeStamp;

    public TimeStampStorable(byte[] byteArray) throws IOException {
        super(byteArray);
    }

    @Override
    public void reset() {
        mIntTimeStamp = (int) System.currentTimeMillis();
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        mIntTimeStamp = dr.readInt();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        dw.writeInt(mIntTimeStamp);
    }

    public int getTimeStamp() {
        return mIntTimeStamp;
    }
}
