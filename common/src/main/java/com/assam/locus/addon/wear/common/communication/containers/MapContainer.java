package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.objects.enums.PointRteAction;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */

public class MapContainer extends TimeStampStorable {

    private static final String TAG = "MapContainer";

    // result from loaded image
    private ActionTools.BitmapLoadResult mLoadedMap;

    // information about type of active guidance
    private int guideType;
    private int navPointAction1Id;
    private int navPointAction2Id;
    private double navPoint1Dist;
    private int unitsFormatLength;

    public MapContainer() {
        super();
    }

    public MapContainer(byte[] byteArray) throws IOException {
        super(byteArray);
    }

    public MapContainer(ActionTools.BitmapLoadResult loadedMap, UpdateContainer mLastUpdate, LocusInfo li) {
        this();
        mLoadedMap = loadedMap;
        if (mLastUpdate != null) {
            guideType = mLastUpdate.getGuideType();
            UpdateContainer.GuideTypeTrack guide = mLastUpdate.getGuideTypeTrack();
            if (guide != null) {
                navPointAction1Id = guide.getNavPoint1Action().getId();
                navPointAction2Id = guide.getNavPoint2Action().getId();
                navPoint1Dist = guide.getNavPoint1Dist();
                if (li != null) {
                    unitsFormatLength = li.getUnitsFormatLength();
                }
            }
        }
    }

    public void reset() {
        super.reset();
        mLoadedMap = null;
        guideType = UpdateContainer.GUIDE_TYPE_DISABLED;
        navPointAction1Id = PointRteAction.UNDEFINED.getId();
        navPointAction2Id = PointRteAction.UNDEFINED.getId();
        navPoint1Dist = 0;
        unitsFormatLength = 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        guideType = dr.readInt();
        navPointAction1Id = dr.readInt();
        navPointAction2Id = dr.readInt();
        navPoint1Dist = dr.readDouble();
        unitsFormatLength = dr.readInt();
        boolean isMap = dr.readBoolean();
        try {
            mLoadedMap = isMap ? (ActionTools.BitmapLoadResult) dr.readStorable(ActionTools.BitmapLoadResult.class) : null;
        } catch (Exception e) {
            mLoadedMap =  null;
            Logger.logE(TAG, "Could not read map image.", e);
        }
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeInt(guideType);
        dw.writeInt(navPointAction1Id);
        dw.writeInt(navPointAction2Id);
        dw.writeDouble(navPoint1Dist);
        dw.writeInt(unitsFormatLength);
        boolean isMap = mLoadedMap != null;
        dw.writeBoolean(isMap);
        if (isMap) {
            dw.writeStorable(mLoadedMap);
        }
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    public int getGuideType() {
        return guideType;
    }

    public int getNavPointAction1Id() {
        return navPointAction1Id;
    }

    public void setNavPointAction1Id(int navPointAction1Id) {
        this.navPointAction1Id = navPointAction1Id;
    }

    public int getNavPointAction2Id() {
        return navPointAction2Id;
    }

    public void setNavPointAction2Id(int navPointAction2Id) {
        this.navPointAction2Id = navPointAction2Id;
    }

    public double getNavPoint1Dist() {
        return navPoint1Dist;
    }

    public int getUnitsFormatLength() {
        return unitsFormatLength;
    }

    public ActionTools.BitmapLoadResult getLoadedMap() {
        return mLoadedMap;
    }
}
