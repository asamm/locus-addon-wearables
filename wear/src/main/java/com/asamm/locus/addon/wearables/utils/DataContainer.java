package com.asamm.locus.addon.wearables.utils;

import android.util.Log;

import java.io.IOException;
import java.util.List;

import locus.api.android.ActionTools;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class DataContainer extends Storable {

    // tag for logger
    private static final String TAG = "DataContainer";

    // version of active locus
    private LocusUtils.LocusVersion mLocusVersion;
    // info from Locus
    private LocusInfo mLocusInfo;
    // track recording profiles
    private List<ActionTools.TrackRecordProfileSimple> mTrackRecProfiles;
    // result from loaded image
    private ActionTools.BitmapLoadResult mLoadedMap;

    /**
     * Base constructor mainly for a Storable class.
     */
    @SuppressWarnings("unused")
    public DataContainer() {
        super();
    }

    /**
     * Constructor based on raw byte array.
     * @param data packed data
     * @throws IOException
     */
    public DataContainer(byte[] data) throws IOException {
        super(data);
    }

    /**
     * Construct container.
     * @param locusVersion version of active locus
     * @param locusInfo info from Locus
     * @param trackRecProfiles track recording profiles
     */
    public DataContainer(LocusUtils.LocusVersion locusVersion, LocusInfo locusInfo,
                         List<ActionTools.TrackRecordProfileSimple> trackRecProfiles) {
        super();
        this.mLocusVersion = locusVersion;
        this.mLocusInfo = locusInfo;
        this.mTrackRecProfiles = trackRecProfiles;
    }

    /**
     * Get current Locus version.
     * @return Locus version object
     */
    public LocusUtils.LocusVersion getLocusVersion() {
        return mLocusVersion;
    }

    /**
     * Get current Locus info.
     * @return current Locus info object
     */
    public LocusInfo getLocusInfo() {
        return mLocusInfo;
    }

    /**
     * Get current track recording profiles.
     * @return current profiles
     */
    public List<ActionTools.TrackRecordProfileSimple> getTrackRecProfiles() {
        return mTrackRecProfiles;
    }

    // LOADED IMAGE

    /**
     * Get current attached map preview.
     * @return attached map preview
     */
    public ActionTools.BitmapLoadResult getMapPreview() {
        return mLoadedMap;
    }

    /**
     * Set new map preview to current container.
     * @param preview new map preview
     */
    public void setMapPreview(ActionTools.BitmapLoadResult preview) {
        this.mLoadedMap = preview;
    }

    /**
     * Merge fresh container to current existing container.
     * @param container container we wants to merge
     */
    public void mergeContainer(DataContainer container) {
        // check container
        if (container == null) {
            Logger.logD(TAG, "mergeContainer(" + container + "), " +
                    "attempt to merge empty container");
            return;
        }

        // merge version
        if (container.getLocusVersion() != null) {
            mLocusVersion = container.getLocusVersion();
        }else
            Log.d(TAG, "mergeContainer: getLocusVersion() is null");

        // merge info
        if (container.getLocusInfo() != null) {
            mLocusInfo = container.getLocusInfo();
        }

        // merge profiles
        if (container.getTrackRecProfiles() != null) {
            mTrackRecProfiles = container.getTrackRecProfiles();
        }

        // map preview
        if (container.getMapPreview() != null) {
            mLoadedMap = container.getMapPreview();
        }
    }

    /**************************************************/
    // STORABLE PART
    /**************************************************/

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        mLocusVersion = null;
        mLocusInfo = null;
        mTrackRecProfiles = null;
        mLoadedMap = null;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        // read LocusVersion
        try {
            // read LocusVersion
            if (dr.readBoolean()) {
                mLocusVersion = (LocusUtils.LocusVersion)
                        dr.readStorable(LocusUtils.LocusVersion.class);
            }

            // read info container
            if (dr.readBoolean()) {
                mLocusInfo = (LocusInfo)
                        dr.readStorable(LocusInfo.class);
            }

            // read track record profiles
            if (dr.readBoolean()) {
                //noinspection unchecked
                mTrackRecProfiles = (List<ActionTools.TrackRecordProfileSimple>)
                        dr.readListStorable(ActionTools.TrackRecordProfileSimple.class);
            }

            // map preview
            if (dr.readBoolean()) {
                mLoadedMap = (ActionTools.BitmapLoadResult)
                        dr.readStorable(ActionTools.BitmapLoadResult.class);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        // write LocusVersion
        if (mLocusVersion != null) {
            dw.writeBoolean(true);
            dw.writeStorable(mLocusVersion);
        } else {
            dw.writeBoolean(false);
        }

        // write info container
        if (mLocusInfo != null) {
            dw.writeBoolean(true);
            dw.writeStorable(mLocusInfo);
        } else {
            dw.writeBoolean(false);
        }

        // write track record profiles
        if (mTrackRecProfiles != null) {
            dw.writeBoolean(true);
            dw.writeListStorable(mTrackRecProfiles);
        } else {
            dw.writeBoolean(false);
        }

        // write map preview
        if (mLoadedMap != null) {
            dw.writeBoolean(true);
            dw.writeStorable(mLoadedMap);
        } else {
            dw.writeBoolean(false);
        }
    }
}
