package com.asamm.locus.addon.wear.communication;

import android.content.Context;

import com.assam.locus.addon.wear.common.communication.LocusWearCommService;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class WearCommService extends LocusWearCommService {
    private static String TAG = WearCommService.class.getSimpleName();
    private static WearCommService mDeviceCommunicationService;

    private WearCommService(Context c) {
        super(c);
    }

    public static WearCommService getInstance() {
        return mDeviceCommunicationService;
    }

    public static WearCommService initialize(final Context c) {
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

}
