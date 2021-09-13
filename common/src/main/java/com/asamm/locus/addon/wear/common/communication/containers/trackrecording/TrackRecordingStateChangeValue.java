package com.asamm.locus.addon.wear.common.communication.containers.trackrecording;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 13.11.2017.
 * Asamm Software, s.r.o.
 */

public class TrackRecordingStateChangeValue extends TimeStampStorable {
    private static final String TAG = TrackRecordingStateChangeValue.class.getSimpleName();

    private TrackRecordingStateEnum mRecordingState = TrackRecordingStateEnum.NOT_RECORDING;

    private String mProfileName = null;

    public TrackRecordingStateChangeValue() {
        super();
    }

    public TrackRecordingStateChangeValue(byte[] arr) throws IOException {
        super(arr);
    }

    public TrackRecordingStateChangeValue(TrackRecordingStateEnum state,
                                          String profileName) {
        super();
        if (state != null) {
            this.mRecordingState = state;
            this.mProfileName = profileName;
        }
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        String s = dr.readString();
        mRecordingState = TrackRecordingStateEnum.valueOf(s);
        boolean isProfile = dr.readBoolean();
        mProfileName = isProfile ? dr.readString() : null;
    }

    public static <E extends Storable> E readStorable(Class<E> claz, DataReaderBigEndian dr)
            throws InstantiationException, IllegalAccessException, IOException {
        return claz.cast(Storable.Companion.read(claz, dr));
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeString(mRecordingState.name());
        boolean isProfileName = mProfileName != null;
        dw.writeBoolean(isProfileName);
        if (isProfileName) {
            dw.writeString(mProfileName);
        }
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    public TrackRecordingStateEnum getRecordingState() {
        return mRecordingState;
    }

    public String getmProfileName() {
        return mProfileName;
    }
}
