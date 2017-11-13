package com.assam.locus.addon.wear.common.communication.containers.trackrecording;

import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.objects.Storable;
import locus.api.objects.extra.TrackStats;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 13.11.2017.
 * Asamm Software, s.r.o.
 */

public class TrackRecordingStateChangeValue extends TimeStampStorable {
    private static final String TAG = TrackRecordingStateChangeValue.class.getSimpleName();

    private TrackRecordingStateEnum mRecordingState;

    public TrackRecordingStateChangeValue() {
        super();
    }

    public TrackRecordingStateChangeValue(byte[] arr) throws IOException {
        super(arr);
    }

    public TrackRecordingStateChangeValue(TrackRecordingStateEnum state) {
        super();
        if (state == null) {
            reset();
        } else {
            this.mRecordingState = state;
        }
    }

    @Override
    public void reset() {
        super.reset();
        mRecordingState = TrackRecordingStateEnum.NOT_RECORDING;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        String s = dr.readString();
        mRecordingState = TrackRecordingStateEnum.valueOf(s);
    }

    public static <E extends Storable> E readStorable(Class<E> claz, DataReaderBigEndian dr)
            throws InstantiationException, IllegalAccessException, IOException {
        return claz.cast(Storable.read(claz, dr));
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeString(mRecordingState.name());
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    public TrackRecordingStateEnum getRecordingState() {
        return mRecordingState;
    }
}
