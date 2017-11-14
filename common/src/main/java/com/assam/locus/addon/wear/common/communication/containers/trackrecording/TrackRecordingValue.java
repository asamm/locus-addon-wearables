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

public class TrackRecordingValue extends TimeStampStorable {
    private static final String TAG = TrackRecordingValue.class.getSimpleName();

    private boolean infoAvailable;
    // is track record enabled
    private boolean trackRecRecording;
    // if track record is enabled, is running or paused
    private boolean trackRecPaused;
    // name of active track recording profile
    private String trackRecProfileName;
    // complete track statistics
    private TrackStats trackRecStats;

    public TrackRecordingValue() {
        super();
    }

    public TrackRecordingValue(byte[] arr) throws IOException {
        super(arr);
    }

    public TrackRecordingValue(boolean infoAvailable, boolean isTrackRecording, boolean isPaused,
                               String trackRecProfileName, TrackStats trackStats) {
        super();
        this.infoAvailable = infoAvailable;
        this.trackRecRecording = isTrackRecording;
        this.trackRecPaused = isPaused;
        this.trackRecProfileName = trackRecProfileName;
        this.trackRecStats = trackStats;
    }

    @Override
    public void reset() {
        super.reset();
        trackRecRecording = false;
        trackRecPaused = false;
        trackRecProfileName = "";
        trackRecStats = null;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        reset();
        super.readObject(version, dr);
        infoAvailable = dr.readBoolean();
        trackRecRecording = dr.readBoolean();
        if (trackRecRecording) {
            trackRecPaused = dr.readBoolean();
            trackRecProfileName = dr.readString();
            boolean isStats = dr.readBoolean();
            try {
                trackRecStats = isStats ? readStorable(TrackStats.class, dr) : null;
            } catch (Exception e) {
                Logger.logE(TAG, "Failed to read trackStats", e);
                trackRecStats = null;
            }
        }
    }

    public static <E extends Storable> E readStorable(Class<E> claz, DataReaderBigEndian dr)
            throws InstantiationException, IllegalAccessException, IOException {
        return claz.cast(Storable.read(claz, dr));
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeBoolean(infoAvailable);
        dw.writeBoolean(trackRecRecording);
        if (trackRecRecording) {
            dw.writeBoolean(trackRecPaused);
            dw.writeString(trackRecProfileName);
            dw.writeBoolean(trackRecStats != null);
            if (trackRecStats != null) {
                dw.writeStorable(trackRecStats);
            }
        }
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    public boolean isInfoAvailable() {
        return infoAvailable;
    }

    public boolean isTrackRecRecording() {
        return trackRecRecording;
    }

    public boolean isTrackRecPaused() {
        return trackRecPaused;
    }

    public String getTrackRecProfileName() {
        return trackRecProfileName;
    }

    public TrackStats getTrackRecStats() {
        return trackRecStats;
    }

    public TrackRecordingStateEnum getTrackRecordingState() {
        return trackRecPaused ? TrackRecordingStateEnum.PAUSED :
                trackRecRecording ? TrackRecordingStateEnum.RECORDING :
                        TrackRecordingStateEnum.NOT_RECORDING;
    }
}
