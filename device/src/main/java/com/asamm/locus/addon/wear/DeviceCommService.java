package com.asamm.locus.addon.wear;

import android.content.Context;

import com.asamm.locus.addon.wear.common.utils.Pair;
import com.assam.locus.addon.wear.common.communication.Const;
import com.assam.locus.addon.wear.common.communication.DataPath;
import com.assam.locus.addon.wear.common.communication.LocusWearCommService;
import com.assam.locus.addon.wear.common.communication.containers.BasicAppInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.assam.locus.addon.wear.common.communication.containers.MapContainer;
import com.assam.locus.addon.wear.common.communication.containers.PeriodicCommand;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.TrackStats;
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

    private PeriodicDataTimer mPeriodicDataTimer;


    /**
     * Default constructor.
     *
     * @param ctx current context
     */
    private DeviceCommService(Context ctx) {
        super(ctx);

        try {
            mLastUpdate = ActionTools.getDataUpdateContainer(ctx, LocusUtils.getActiveVersion(ctx));
        } catch (RequiredVersionMissingException e) {
            // TODO cejnar nepodporovat nizsi verze locus API - pouziva okamzite napr GET_TRACK_REC
        }
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
            DeviceCommService s = mInstance;
            if (s != null) {
                s.destroy();
                if (s.mPeriodicDataTimer != null) {
                    s.mPeriodicDataTimer.cancel();
                    s.mPeriodicDataTimer = null;
                }
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
        DataPath path = DataPath.valueOf(item);
        switch (path) {
            case GET_HAND_SHAKE:
                HandShakeValue hndshk = loadHandShake(c);
                sendDataItem(DataPath.PUT_HAND_SHAKE, hndshk);
                break;
            case GET_BASIC_INFO:
                BasicAppInfoValue appInfo = loadBasicInfo(c);
                if (appInfo != null) {
                    sendDataItem(DataPath.PUT_BASIC_INFO, appInfo);
                }
                break;
            case GET_TRACK_REC_PROFILES:
                Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> profiles =
                        loadTrackRecordProfiles(c);
                if (profiles.first != null) {
                    sendDataItem(DataPath.PUT_TRACK_REC_PROFILE_INFO, profiles.first);
                    sendDataItem(DataPath.PUT_TRACK_REC_ICON_INFO, profiles.second);
                }
                break;
            case GET_TRACK_REC:
                TrackRecordingValue trv = loadTrackRecordingValue();
                sendDataItem(DataPath.PUT_TRACK_REC, trv);
                break;
            case PUT_TRACK_REC_STATE_CHANGE: {
                LocusUtils.LocusVersion lv = LocusUtils.getActiveVersion(c);
                TrackRecordingStateChangeValue v = path.createStorableForPath(item);
                handleRecordingStateChanged(c, lv, v.getRecordingState(), v.getmProfileName());
            }
                break;
            case GET_ADD_WAYPOINT: {
                LocusUtils.LocusVersion lv = LocusUtils.getActiveVersion(c);
                handleAddWpt(c, lv);
            }
                break;
            case GET_PERIODIC_DATA: {
                PeriodicCommand v = path.createStorableForPath(item);
                handlePeriodicWearUpdate(v);
            }
                break;
            default:
                Logger.logE(TAG, "Unknown request " + path);
        }
    }

    private void handlePeriodicWearUpdate(PeriodicCommand command) {
        if ((command == null || command.isStopRequest())) {
            if (mPeriodicDataTimer != null) {
                mPeriodicDataTimer.cancel();
            }
            mPeriodicDataTimer = null;
            return;
        }
        final byte activityId = command.getmPeriodicActivityId();
        final int periodMs = command.getmPeriodMs();
        if (mPeriodicDataTimer != null) {
            if (mPeriodicDataTimer.periodMs == periodMs && mPeriodicDataTimer.activityId == activityId) {
                return;
            } else {
                mPeriodicDataTimer.cancel();
                mPeriodicDataTimer = null;
            }
        }

        final TimerTask task;
        switch ( activityId) {
            case PeriodicCommand.IDX_PERIODIC_ACITIVITY_TRACK_RECORDING:
                task = new TimerTask() {
                    @Override
                    public void run() {
                        TrackRecordingValue trv = loadTrackRecordingValue();
                        sendDataItem(DataPath.PUT_TRACK_REC, trv);
                    }
                };
                break;
            case PeriodicCommand.IDX_PERIODIC_ACITIVITY_MAP:
                task = new TimerTask() {
                    @Override
                    public void run() {
                        sendMapPeriodic();
                    }
                };
                break;
            default:
                task = null;
                break;
        }
        if (task == null) { // unknown activity id, don't start the timer
            return;
        }
        mPeriodicDataTimer = new PeriodicDataTimer(activityId, periodMs);
        mPeriodicDataTimer.schedule(task, 0, periodMs);
    }

    private void sendMapPeriodic() {
        MapContainer m = new MapContainer(); // TODO cejnar keep map in memory and rewrite bitmap to save on GC?
        sendDataItem(DataPath.PUT_MAP, m);
        // TODO cejnar
    }

    public static boolean isInstance() {
        return mInstance != null;
    }

    ///////////////////////////////////////////////////////////////////////////
    //      Value object create methods - reading from Locus API             //
    ///////////////////////////////////////////////////////////////////////////

    private void handleAddWpt(Context ctx, LocusUtils.LocusVersion lv) {
        try {
            ActionTools.actionTrackRecordAddWpt(ctx, lv, true);
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "Invalid version " + lv + ", can't add WPT", e);
        }
    }

    private void handleRecordingStateChanged(Context ctx, LocusUtils.LocusVersion lv, TrackRecordingStateEnum newState, String profile) {
        try {
            switch (newState) {
                case PAUSED:
                    ActionTools.actionTrackRecordPause(ctx, lv);
                    break;
                case RUNNING:
                    ActionTools.actionTrackRecordStart(ctx, lv, profile);
                    break;
                case NOT_RECORDING:
                    ActionTools.actionTrackRecordStop(ctx, lv, true);
                    break;
            }
            if (newState == TrackRecordingStateEnum.RUNNING) {

            }
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "Invalid version " + lv + ", cant change track recording state.", e);
        }
    }

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
                new HandShakeValue() :
                new HandShakeValue(locusVersion.getVersionCode(),
                        locusInfo != null && locusInfo.isRunning(),
                        locusInfo != null && locusInfo.isPeriodicUpdatesEnabled());
        return value;
    }

    /**
     * Load basic data from current Locus application.
     */
    private BasicAppInfoValue loadBasicInfo(Context ctx) {
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
            locusInfo = null;
        }

        // prepare container with data and send it
        BasicAppInfoValue value = locusInfo == null ?
                null :
                new BasicAppInfoValue(locusInfo);
        return value;
    }

    /**
     * Load recording profiles data from current Locus application.
     * @param nodeId ID of requester
     */
    /**
     * @param ctx
     * @return A pair of lists. First lists contains track profiles and description.
     * Second list contains track profile icons. Both lists are the same lenght and order.
     */
    private Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList>
    loadTrackRecordProfiles(Context ctx) {
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
            Logger.logE(TAG, "loadTrackRecordProfiles()", e);

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

    private TrackRecordingValue loadTrackRecordingValue() {
        boolean infoAvailable = mLastUpdate != null;
        boolean trackRec = infoAvailable && mLastUpdate.isTrackRecRecording();
        boolean trackRecPause = infoAvailable && mLastUpdate.isTrackRecPaused();
        String profileName = infoAvailable ? mLastUpdate.getTrackRecProfileName() : "";
        TrackStats stats = infoAvailable ? mLastUpdate.getTrackRecStats() : null;
        TrackRecordingValue trv = new TrackRecordingValue(infoAvailable, trackRec, trackRecPause,
                                                          profileName, stats);
        return trv;
    }

    private static class PeriodicDataTimer extends Timer {
        private byte activityId;
        private int periodMs;
        private PeriodicDataTimer(byte activityId, int periodMs) {
            super();
            this.activityId = activityId;
            this.periodMs = periodMs;
        }
    }
}
