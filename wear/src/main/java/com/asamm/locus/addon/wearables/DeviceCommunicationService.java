package com.asamm.locus.addon.wearables;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

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
    }

    public static DeviceCommunicationService getInstance() {
        return mDeviceCommunicationService;
    }

    public static void connect(final Context c) {
        if (mDeviceCommunicationService == null) {
            synchronized (TAG) {
                if (mDeviceCommunicationService == null) {
                    mDeviceCommunicationService = new DeviceCommunicationService(c);
                }
            }
        }

    }
    public static void disconnect(){
        mDeviceCommunicationService = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        //TODO cejnar handle error
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO cejnar handle error
    }

}
