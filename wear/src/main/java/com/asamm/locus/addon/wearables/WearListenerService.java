package com.asamm.locus.addon.wearables;

import android.util.Log;

import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by menion on 08/08/15.
 * Asamm Software, s. r. o.
 */
public class WearListenerService extends WearableListenerService {

    // tag for logger
    private static final String TAG = WearListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
//        Logger.logW(TAG, "onMessageReceived(" + messageEvent.getPath() + "), " +
//                "comm:" + DeviceCommunication.getInstance());

        // let device handle it
        DeviceCommunication comm = DeviceCommunication.getInstance();
        if (comm != null ) {
            comm.handleNewData(messageEvent);
        }
    }

}
