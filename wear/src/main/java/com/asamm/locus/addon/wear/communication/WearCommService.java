package com.asamm.locus.addon.wear.communication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.common.communication.LocusWearCommService;
import com.google.android.gms.common.ConnectionResult;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class WearCommService extends LocusWearCommService {
    private static String TAG = WearCommService.class.getSimpleName();
    private static WearCommService mDeviceCommunicationService;

    private Thread mRefresher;

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

    /**
     * Start thread that will take care about refreshing of content.
     */
    private void startRefresher() {
        // class for periodic checks.
        final Runnable mChecker = new Runnable() {

            @Override
            public void run() {
                try {
                    // repeat actions till system is running
                    int counter = 0;
                    while (isConnected() && mRefresher != null) {
                        Thread.sleep(3333);
                        reconnectIfNeeded();

                    }
                } catch (Exception e) {
                    Logger.logE(TAG, "startRefresher()", e);
                }
            }
        };

        // prepare and start refresher
        mRefresher = new Thread(mChecker);
        mRefresher.setPriority(Thread.MIN_PRIORITY);
        mRefresher.start();
    }

}
