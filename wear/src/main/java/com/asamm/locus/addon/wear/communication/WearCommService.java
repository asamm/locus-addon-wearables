package com.asamm.locus.addon.wear.communication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.asamm.locus.addon.wear.MainApplication;
import com.assam.locus.addon.wear.common.communication.LocusWearCommService;
import com.google.android.gms.common.ConnectionResult;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class WearCommService extends LocusWearCommService {
    private static String TAG = WearCommService.class.getSimpleName();
    private static WearCommService mDeviceCommunicationService;

    private final MainApplication mApp;
    private WearCommService(MainApplication c) {
        super(c);
        this.mApp = c;
    }

    public static WearCommService getInstance() {
        return mDeviceCommunicationService;
    }

    public static WearCommService initialize(final MainApplication c) {
        if (mDeviceCommunicationService == null) {
            synchronized (TAG) {
                if (mDeviceCommunicationService == null) {
                    mDeviceCommunicationService = new WearCommService(c);

                }
            }
        }
        return mDeviceCommunicationService;
    }

    public void destroy() {
        if (mDeviceCommunicationService != null) {
            super.destroy();
            mDeviceCommunicationService = null;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);
        final MainApplication app = this.mApp;
        if (app != null) {
            app.onConnected();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        super.onConnectionFailed(connectionResult);
        if (mApp != null) {
            mApp.onConnectionSuspened();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onConnectionSuspended(i);
        if (mApp != null) {
            mApp.onConnectionSuspened();
        }
    }
}
