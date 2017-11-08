package com.asamm.locus.addon.wear;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.assam.locus.addon.wear.common.Const;
import com.assam.locus.addon.wear.common.DataPath;
import com.assam.locus.addon.wear.common.HandShakeValue;
import com.assam.locus.addon.wear.common.TimeStampStorable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ConcurrentLinkedQueue;

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

public class RequestHandler implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // tag for logger
    private static final String TAG = RequestHandler.class.getSimpleName();

    // instance of handler
    private static RequestHandler mInstance;

    // Google API client
    private GoogleApiClient mGoogleApiClient;

    // Last received update from Locus
    private UpdateContainer mLastUpdate;

    /** List of unsent data consisting of pairs of <PATH, DATA> */
    private ConcurrentLinkedQueue<Pair<DataPath, TimeStampStorable>> mUnsentData;
    /**
     * Default constructor.
     *
     * @param ctx current context
     */
    private RequestHandler(Context ctx) {
        // connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(ctx).
                addApi(Wearable.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                build();
        mGoogleApiClient.connect();
        mUnsentData - new ConcurrentLinkedQueue();
        // enable receiver
        PeriodicUpdatesReceiver.enableReceiver(ctx);
    }

    /**
     * Get instance of request handler.
     *
     * @param ctx current context
     * @return instance of handler
     */
    static RequestHandler getInstance(Context ctx) {
        if (mInstance == null) {
            synchronized (TAG) {
                if (mInstance == null) {
                    mInstance = new RequestHandler(ctx);
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
                Logger.logW(TAG, "onDestroy()");

                // destroy GoogleAPIClient class
                mInstance.mGoogleApiClient.disconnect();

                // disable receiver
                PeriodicUpdatesReceiver.disableReceiver(ctx);
            }
            mInstance = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Logger.logD(TAG, "connected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

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

    public void onMessageReceived(Context c, MessageEvent messageEvent) {
    }

    public void onDataChanged(Context c, DataEvent newData) {
        DataItem item = newData.getDataItem();
        String path = item.getUri().getPath();
        if (DataPath.GET_HAND_SHAKE.getPath().equals(path)) {
            HandShakeValue v = loadHandShake(c);
            sendDataItem(DataPath.PUT_HAND_SHAKE, v.getAsBytes());
        } else {
            Logger.logW(TAG, "Data changed at unknown path: " + path);
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
                HandShakeValue.createEmpty() :
                new HandShakeValue(locusVersion.getVersionCode(),
                                   locusInfo == null ? false : locusInfo.isRunning(),
                                   locusInfo == null ? false : locusInfo.isPeriodicUpdatesEnabled());
       return value;
    }

    private void sendDataItem(DataPath path, byte[] data) {
        PutDataRequest request = PutDataRequest.create(path.getPath());
        request.setData(data);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }
    public boolean isConnected() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

}
