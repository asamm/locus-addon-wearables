package com.assam.locus.addon.wear.common;

/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public enum DataPath {
    GET_HAND_SHAKE,
    PUT_HAND_SHAKE,
    PUT_APP_DESTROYED;

    public static final String BASE_PATH = "/locus/wear";

    private String mKey;
    private String mPath;

    DataPath() {
        mKey = this.name().toLowerCase();
        mPath = BASE_PATH + '/' + mKey;
    }

    public String getKey() {
        return mKey;
    }

    public String getPath() {
        return mPath;
    }
}
