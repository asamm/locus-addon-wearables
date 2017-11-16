package com.assam.locus.addon.wear.common.communication;

import com.assam.locus.addon.wear.common.communication.containers.BasicAppInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.assam.locus.addon.wear.common.communication.containers.MapContainer;
import com.assam.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.google.android.gms.wearable.DataItem;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public enum DataPath {
    GET_HAND_SHAKE(EmptyCommand.class),
    PUT_HAND_SHAKE(HandShakeValue.class),
    GET_BASIC_INFO(EmptyCommand.class),
    PUT_BASIC_INFO(BasicAppInfoValue.class),
    GET_TRACK_REC_PROFILES(EmptyCommand.class),
    PUT_TRACK_REC_PROFILE_INFO(TrackProfileInfoValue.ValueList.class),
    PUT_TRACK_REC_ICON_INFO(TrackProfileIconValue.ValueList.class),
    PUT_APP_DESTROYED(EmptyCommand.class),

    GET_TRACK_REC(EmptyCommand.class),
    PUT_TRACK_REC(TrackRecordingValue.class),
    PUT_TRACK_REC_STATE_CHANGE(TrackRecordingStateChangeValue.class),
    GET_ADD_WAYPOINT(EmptyCommand.class),

    GET_PERIODIC_DATA(PeriodicCommand.class),

    GET_KEEP_ALIVE(EmptyCommand.class),
    PUT_MAP(MapContainer.class),
    /** Fake communication data path, used for signalling activity about ON_CONNECTED event inside
        strictly the application. Should not be used over network. */
    PUT_ON_CONNECTED_EVENT(EmptyCommand.class);

    public static final String BASE_PATH = "/locus/wear";

    private static final DataPath[] values = values();

    private String mKey;
    private String mPath;
    private Class<? extends TimeStampStorable> mContainerClass;

    DataPath(Class<? extends TimeStampStorable> container) {
        mKey = this.name().toLowerCase();
        mPath = BASE_PATH + '/' + mKey;
        this.mContainerClass = container;
    }

    public String getKey() {
        return mKey;
    }

    public String getPath() {
        return mPath;
    }

    public <E extends TimeStampStorable> E createStorableForPath(DataItem item) {
        if (mContainerClass.getSimpleName().equals(EmptyCommand.class.getSimpleName())) {
            return null;
        }
        try {
            return (E) mContainerClass.getConstructor(byte[].class).newInstance(item.getData());
        } catch (Exception e) {
            Logger.logE("DataPath", "Constructor failed for " + name(), e);
            return null;
        }
    }

    public static DataPath valueOf(DataItem item) {
        for (DataPath p : values) {
            if (p.getPath().equals(item.getUri().getPath())) {
                return p;
            }
        }
        return null;
    }
}
