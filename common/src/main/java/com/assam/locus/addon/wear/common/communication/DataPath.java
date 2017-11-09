package com.assam.locus.addon.wear.common.communication;

import com.assam.locus.addon.wear.common.communication.containers.BasicAppInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.EmptyCommand;
import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.assam.locus.addon.wear.common.communication.containers.TimeStampStorable;
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
    PUT_APP_DESTROYED(EmptyCommand.class);

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

    public TimeStampStorable createStorableForPath(DataItem item) {
        try {
            return mContainerClass.getConstructor(byte[].class).newInstance(item.getData());
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
