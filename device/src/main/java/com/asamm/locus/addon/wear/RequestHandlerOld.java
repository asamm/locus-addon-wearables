package com.asamm.locus.addon.wear;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.assam.locus.addon.wear.common.communication.Const;
import com.assam.locus.addon.wear.common.communication.containers.DataContainer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.Utils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.utils.Logger;

/**
 * Created by menion on 17/08/15.
 * Asamm Software, s. r. o.
 */
class RequestHandlerOld implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

    // tag for logger
    private static final String TAG = "RequestHandlerOld";

    // instance of handler
    private static RequestHandlerOld mInstance;

    /**************************************************/
    // PRIVATE
    /**************************************************/

    // Google API client
    private GoogleApiClient mGoogleApiClient;

    // Last received update from Locus
    private UpdateContainer mLastUpdate;

    // target for data
    private String mToSendNodeId;
    // message to send after connect
    private String mToSendMessage;
    // container to send after connect
    private byte[] mToSendData;

    /**
     * Default constructor.
     * @param ctx current context
     */
    private RequestHandlerOld(Context ctx) {
        Logger.logE(TAG, "onCreate()", new Exception());

        // connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(ctx).
                addApi(Wearable.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                build();
        mGoogleApiClient.connect();

        // enable receiver
        PeriodicUpdatesReceiver.enableReceiver(ctx);
    }

    /**
     * Handle received message from watch part.
     * @param ctx current context
     * @param messageEvent received message
     */
	void onMessageReceived(Context ctx, MessageEvent messageEvent) {
        Logger.logD(TAG, "onMessageReceived(" + messageEvent.getPath() + ")");

        // handle received messages
        try {
            // get required parameters
            String path = messageEvent.getPath();
            LocusUtils.LocusVersion lv = LocusUtils.getActiveVersion(ctx);
            // handle message
            if (path.equals(Const.PATH_GET_BASE_DATA)) {
                loadBaseData(ctx, messageEvent.getSourceNodeId());
            } else if (path.equals(Const.PATH_GET_TRACK_RECORD_PROFILES)) {
                loadTrackRecordProfiles(ctx, messageEvent.getSourceNodeId());
            } else if (path.equals(Const.PATH_GET_PERIODIC_UPDATE)) {
                loadUpdateContainer(messageEvent.getSourceNodeId());
            } else if (path.startsWith(Const.PATH_GET_MAP_PREVIEW)) {
                loadMapPreview(ctx, messageEvent.getSourceNodeId(), path, lv);
            } else if (path.startsWith(Const.PATH_TRACK_REC_START)) {
                if (path.length() == Const.PATH_TRACK_REC_START.length()) {
                    ActionTools.actionTrackRecordStart(ctx, lv);
                } else {
                    String profile = path.substring(Const.PATH_TRACK_REC_START.length());
                    ActionTools.actionTrackRecordStart(ctx, lv, profile);
                }
            } else if (path.equals(Const.PATH_TRACK_REC_STOP)) {
                ActionTools.actionTrackRecordStop(ctx, lv, true);
            } else if (path.equals(Const.PATH_TRACK_REC_PAUSE)) {
                ActionTools.actionTrackRecordPause(ctx, lv);
            } else if (path.equals(Const.PATH_TRACK_REC_ADD_WPT)) {
                ActionTools.actionTrackRecordAddWpt(ctx, lv, true);
            }
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "onMessageReceived(" + messageEvent + ")", e);
        }
    }

    // GOOGLE API CLIENT - CONNECTION CALLBACK

    @Override
    public void onConnected(Bundle bundle) {
        Logger.logD(TAG, "onConnected(" + bundle + ")");
        afterApiConnected();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.logD(TAG, "onConnectionSuspended(" + i + ")");
    }

    // GOOGLE API CLIENT - ON CONNECTION FAILED LISTENER

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.logD(TAG, "onConnectionFailed(" + connectionResult + ")");
    }

    // PERIODIC UPDATES

    /**
     * Update content with fresh updates.
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

    /**************************************************/
    // BASIC FUNCTIONS
    /**************************************************/

    /**
     * Load basic data from current Locus application.
     * @param nodeId ID of requester
     */
    private void loadBaseData(Context ctx, String nodeId) {
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
            Logger.logE(TAG, "loadBaseData(" + nodeId + ")", e);

            // clear data
            locusVersion = null;
            locusInfo = null;
        }

        // prepare container with data and send it
        DataContainer container = new DataContainer(
                locusVersion, locusInfo, null);
        sendMessage(nodeId,
                Const.PATH_LOADED_DATA_CONTAINER,
                container.getAsBytes());
    }

    /**
     * Allow to load map preview from Locus.
     * @param nodeId ID of requested
     * @param path path with parameters
     * @param locusVersion current active Locus version
     */
    private void loadMapPreview(Context ctx, String nodeId, String path, LocusUtils.LocusVersion locusVersion) {
        // get parameters
        path = path.substring(Const.PATH_GET_MAP_PREVIEW.length());
        String[] parts = path.split("/");
        double lon = Utils.parseDouble(parts[0]);
        double lat = Utils.parseDouble(parts[1]);
        int zoom = Utils.parseInt(parts[2]);
        int sizeX = Utils.parseInt(parts[3]);
        int sizeY = Utils.parseInt(parts[4]);

        // request map
        ActionTools.BitmapLoadResult loadedMap = null;
        try {
            loadedMap = ActionTools.getMapPreview(ctx,
                    locusVersion, new Location("", lat, lon),
                    zoom, sizeX, sizeY, false);
        } catch (RequiredVersionMissingException e) {
            Logger.logE(TAG, "loadMapPreview(" + nodeId + ", " + path + ", " + locusVersion + ")");
        }

        // prepare container with data and send it
        DataContainer container = new DataContainer();
        container.setMapPreview(loadedMap);
        sendMessage(nodeId,
                Const.PATH_LOADED_DATA_CONTAINER,
                container.getAsBytes());
    }

    /**
     * Load recording profiles data from current Locus application.
     * @param nodeId ID of requester
     */
    private void loadTrackRecordProfiles(Context ctx, String nodeId) {
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

        // prepare container with data and send it
        DataContainer container = new DataContainer(
                null, null, trackRecProfiles);
        sendMessage(nodeId,
                Const.PATH_LOADED_DATA_CONTAINER,
                container.getAsBytes());
    }

    /**
     * Handle currently loaded update container and send it to the wear device.
     * @param nodeId ID of requester
     */
    private void loadUpdateContainer(String nodeId) {
        if (mLastUpdate != null) {
            sendMessage(nodeId, Const.PATH_LOADED_PERIODIC_UPDATE, mLastUpdate.getAsBytes());
        } else {
            sendMessage(nodeId, Const.PATH_LOADED_PERIODIC_UPDATE, null);
        }
    }

    // TOOLS

    /**
     * Perform action after API connects.
     */
    private void afterApiConnected() {
        if (mToSendNodeId != null && mToSendNodeId.length() > 0) {
            sendMessage(mToSendNodeId, mToSendMessage, mToSendData);
        }
    }

    /**
     * Check if current device is correct connected.
     * @return {@code true} if connected
     */
	private boolean isConnected() {
        return mGoogleApiClient != null &&
                mGoogleApiClient.isConnected();
    }

    /**
     * Send message to connected node (wearable device).
     * @param nodeId ID of node
     * @param msg message key
     * @param data data to send
     */
    private void sendMessage(String nodeId, String msg, byte[] data) {
        Logger.logD(TAG, "sendMessage(" + nodeId + ", " + msg + ", " + data + "), " +
                "connected:" + isConnected() + ", data:" + (data == null ? 0L : data.length));

        // check connected state
        if (!isConnected()) {
            mToSendNodeId = nodeId;
            mToSendMessage = msg;
            mToSendData = data;
            return;
        }

        // prepare callback for result
        ResultCallback<MessageApi.SendMessageResult> callback =
                new ResultCallback<MessageApi.SendMessageResult>() {

                    @Override
                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Logger.logE(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                };

        // finally send message and clear references
        Wearable.MessageApi.
                sendMessage(mGoogleApiClient, nodeId, msg, data).
                setResultCallback(callback);
        mToSendNodeId = null;
        mToSendMessage = null;
        mToSendMessage = null;
    }
}
