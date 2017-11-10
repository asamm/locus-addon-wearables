package com.asamm.locus.addon.wear;

import android.content.Context;

import com.asamm.locus.addon.wear.common.utils.Pair;
import com.assam.locus.addon.wear.common.communication.Const;
import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.LocusWearCommService;
import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.assam.locus.addon.wear.common.communication.containers.TrackProfileIconValue;
import com.assam.locus.addon.wear.common.communication.containers.TrackProfileInfoValue;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageEvent;

import java.util.ArrayList;
import java.util.List;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.utils.Logger;

/**
 * Singleton class for handling communication between this application and the watch.
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class DeviceCommService extends LocusWearCommService {

    // tag for logger
    private static final String TAG = DeviceCommService.class.getSimpleName();

    private static DeviceCommService mInstance;

    // Last received update from Locus
    private UpdateContainer mLastUpdate;

    /**
     * Default constructor.
     *
     * @param ctx current context
     */
    private DeviceCommService(Context ctx) {
        super(ctx);
        // enable receiver
        PeriodicUpdatesReceiver.enableReceiver(ctx);
    }

    /**
     * Get instance of request handler.
     *
     * @param ctx current context
     * @return instance of handler
     */
    static DeviceCommService getInstance(Context ctx) {
        if (mInstance == null) {
            synchronized (TAG) {
                if (mInstance == null) {
                    mInstance = new DeviceCommService(ctx);
                }
            }
        }
        return mInstance;
    }

    /**
     * Destroy instance of receiver.
     *
     * @param ctx current context
     */
    static void destroyInstance(Context ctx) {
        synchronized (TAG) {
            if (mInstance != null) {
                mInstance.destroy();
                // disable receiver
                PeriodicUpdatesReceiver.disableReceiver(ctx);
                mInstance = null;
            }
        }
    }

    /**
     * Update content with fresh updates.
     *
     * @param update update container
     */
    void onUpdate(UpdateContainer update) {
        Logger.logD(TAG, "onUpdate(" + update + ")");
        mLastUpdate = update;
    }

    /**
     * Notify about incorrect data.
     */
    void onIncorrectData() {
        Logger.logD(TAG, "onIncorrectData()");
        mLastUpdate = null;
    }

    void onMessageReceived(Context c, MessageEvent messageEvent) {
        // TODO cejnar messageApi
    }

    void onDataChanged(Context c, DataEvent newData) {
        DataItem item = newData.getDataItem();
        String path = item.getUri().getPath();
        if (DataPath.GET_HAND_SHAKE.getPath().equals(path)) {
            HandShakeValue v = loadHandShake(c);
            sendDataItem(DataPath.PUT_HAND_SHAKE, v);
        } else {
            Logger.logW(TAG, "Data changed at unknown path: " + path);
        }
    }


    public static boolean isInstance() {
        return mInstance != null;
    }

    ///////////////////////////////////////////////////////////////////////////
    //      Value object create methods - reading from Locus API             //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load basic data from current Locus application.
     */
    private HandShakeValue loadHandShake(Context ctx) {
        LocusUtils.LocusVersion locusVersion;
        LocusInfo locusInfo = null;

        try {
            // read Locus info
            locusVersion = LocusUtils.getActiveVersion(ctx, Const.LOCUS_VERSION_CODE);

            // check if object exists
            if (locusVersion != null) {
                // handle info
                locusInfo = ActionTools.getLocusInfo(ctx, locusVersion);
            }
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "loadHandShake", e);

            // clear data
            locusVersion = null;
            locusInfo = null;
        }

        // prepare container with data and send it
        HandShakeValue value = locusVersion == null ?
                HandShakeValue.createEmpty() :
                new HandShakeValue(locusVersion.getVersionCode(),
                        locusInfo != null && locusInfo.isRunning(),
                        locusInfo != null && locusInfo.isPeriodicUpdatesEnabled());
        return value;
    }

    /**
     * Load recording profiles data from current Locus application.
     * @param nodeId ID of requester
     */
    /**
     * @param ctx
     * @param nodeId
     * @return A pair of lists. First lists contains track profiles and description.
     * Second list contains track profile icons. Both lists are the same lenght and order.
     */
    private Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList>
    loadTrackRecordProfiles(Context ctx, String nodeId) {
        List<ActionTools.TrackRecordProfileSimple> trackRecProfiles = null;

        try {
            // read Locus info
            LocusUtils.LocusVersion lv = LocusUtils.getActiveVersion(
                    ctx, Const.LOCUS_VERSION_CODE);

            // check if object exists
            if (lv != null) {
                // load also track record profiles
                trackRecProfiles = ActionTools.getTrackRecordingProfiles(
                        ctx, lv);
            }
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "loadTrackRecordProfiles(" + nodeId + ")", e);

            // clear data
            trackRecProfiles = null;
        }

        Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> result =
                new Pair<>(new TrackProfileInfoValue.ValueList(), new TrackProfileIconValue.ValueList());

        if (trackRecProfiles != null && !trackRecProfiles.isEmpty()) {
            ArrayList<TrackProfileInfoValue> profiles = new ArrayList<>(trackRecProfiles.size());
            ArrayList<TrackProfileIconValue> icons = new ArrayList<>(trackRecProfiles.size());
            for (ActionTools.TrackRecordProfileSimple profile : trackRecProfiles) {
                profiles.add(new TrackProfileInfoValue(profile));
                icons.add(new TrackProfileIconValue(profile));
            }
            result.first.setStorables(profiles);
            result.second.setStorables(icons);
        }
        return result;
    }


}