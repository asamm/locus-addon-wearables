package com.asamm.locus.addon.wear;

import android.content.Context;

import com.assam.locus.addon.wear.common.Const;
import com.assam.locus.addon.wear.common.DataPath;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Listener for incoming messages/data from the watch
 * <p>
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class DeviceListenerService extends WearableListenerService {

    private static final String TAG = DeviceListenerService.class.getSimpleName();
    // timer for termination
    private static Timer mTimerTerminate;

    private interface DataConsumer<T> {
        void consume(Context c, RequestHandler rh, T newData);

        boolean isTerminateRequest(T newData);
    }

    private final DataConsumer<MessageEvent> messageConsumer = new DataConsumer<MessageEvent>() {
        @Override
        public void consume(Context c, RequestHandler rh, MessageEvent newData) {
            rh.onMessageReceived(c, newData);
        }

        @Override
        public boolean isTerminateRequest(MessageEvent newData) {
            String path = newData.getPath();
            return Const.PATH_STATE_APP_DESTROYED.equals(path);
        }
    };

    private final DataConsumer<DataEvent> dataEventConsumer = new DataConsumer<DataEvent>() {
        @Override
        public void consume(Context c, RequestHandler rh, DataEvent newData) {
            rh.onDataChanged(c, newData);
        }

        @Override
        public boolean isTerminateRequest(DataEvent newData) {
            DataItem d = newData.getDataItem();
            return DataPath.PUT_APP_DESTROYED.getPath().equals(d.getUri().getPath());
        }
    };

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Logger.logD("DeviceListenerService", "onMessageReceived(" + messageEvent + "), " +
                "path:" + path);
        handleDataChange(messageConsumer, messageEvent);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                Logger.logD(TAG, "Received data changed: " + event.getDataItem().getUri().getPath());
                handleDataChange(dataEventConsumer, event);
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private <T> void handleDataChange(DataConsumer<T> dataConsumer, T newData) {
        // cancel timer if any message has arrived
        if (mTimerTerminate != null) {
            mTimerTerminate.cancel();
            mTimerTerminate = null;
        }

        // handle termination request if present, otherwise delegate data handling to RequestHandler
        if (dataConsumer.isTerminateRequest(newData)) {
            RequestHandler.destroyInstance(this);
        } else {
            if (RequestHandler.getInstance(this).isConnected()) {
                dataConsumer.consume(this, RequestHandler.getInstance(this), newData);
            } else {
                Logger.logE(TAG, "GAPI client not connected");
            }

            // start "destroyer"
            mTimerTerminate = new Timer();
            mTimerTerminate.schedule(new TimerTask() {

                @Override
                public void run() {
                    RequestHandler.destroyInstance(DeviceListenerService.this);
                    mTimerTerminate = null;
                }
            }, TimeUnit.SECONDS.toMillis(15));
        }
    }
}
