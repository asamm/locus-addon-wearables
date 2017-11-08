package com.asamm.locus.addon.wear;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.assam.locus.addon.wear.common.DataPath;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class DeviceCommunicationService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static String TAG = DeviceCommunicationService.class.getSimpleName();
    private static DeviceCommunicationService mDeviceCommunicationService;

    private GoogleApiClient mGoogleApiClient;

    private DeviceCommunicationService(Context c) {
        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    public static DeviceCommunicationService getInstance() {
        return mDeviceCommunicationService;
    }

    public static DeviceCommunicationService initialize(final Context c) {
        if (mDeviceCommunicationService == null) {
            synchronized (TAG) {
                if (mDeviceCommunicationService == null) {
                    mDeviceCommunicationService = new DeviceCommunicationService(c);

                }
            }
        }
        return mDeviceCommunicationService;
    }
    public void destroy(){
        if (mDeviceCommunicationService != null) {
            mDeviceCommunicationService.mGoogleApiClient.disconnect();
            mDeviceCommunicationService = null;
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Logger.logD(TAG, "googleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.logD(TAG, "connection suspeded");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "connection failed");
    }

    public static boolean isInitialized() {
        return mDeviceCommunicationService != null;
    }
    public boolean isConnected() {
        return mGoogleApiClient.isConnected();
    }

    public void sendDataItem(DataPath path, byte[] data) {
        PutDataRequest request = PutDataRequest.create(path.getPath());
        request.setData(data);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }

}
