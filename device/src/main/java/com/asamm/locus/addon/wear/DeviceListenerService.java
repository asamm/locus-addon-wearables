package com.asamm.locus.addon.wear;

import android.content.Context;

import com.assam.locus.addon.wear.common.communication.Const;
import com.assam.locus.addon.wear.common.communication.DataPath;
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
    /** Timeout in seconds after which communication services are killed
        and locus periodic update disabled */
    private static final int INACTVITY_TIMEOUT_SECONDS = 15;
    // timer for termination
    private static Timer mTimerTerminate;

    /** Messages Consumer */
    private final DataConsumer<MessageEvent> messageConsumer = new DataConsumer<MessageEvent>() {
        @Override
        public void consume(Context c, DeviceCommService rh, MessageEvent newData) {
            rh.onMessageReceived(c, newData);
        }
        @Override
        public boolean isTerminateRequest(MessageEvent newData) {
            String path = newData.getPath();
            return Const.PATH_STATE_APP_DESTROYED.equals(path);
        }
    };

    /** DataEvent consumer */
    private final DataConsumer<DataEvent> dataEventConsumer = new DataConsumer<DataEvent>() {
        @Override
        public void consume(Context c, DeviceCommService rh, DataEvent newData) {
            rh.onDataChanged(c, newData);
        }

        @Override
        public boolean isTerminateRequest(DataEvent newData) {
            DataItem d = newData.getDataItem();
            return DataPath.PUT_APP_DESTROYED.getPath().equals(d.getUri().getPath());
        }
    };

    /**
     * Message receive callback
     *
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        Logger.logD("DeviceListenerService", "onMessageReceived(" + messageEvent + "), " +
                "path:" + path);
        handleDataChange(messageConsumer, messageEvent);
    }

    /**
     * DataChanged callback
     *
     * @param dataEventBuffer
     */
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

    /**
     * Helper method for data consumption using supplied consumer.
     * @param dataConsumer
     * @param newData
     * @param <T>
     */
    private <T> void handleDataChange(DataConsumer<T> dataConsumer, T newData) {
        // cancel timer if any message has arrived
        if (mTimerTerminate != null) {
            mTimerTerminate.cancel();
            mTimerTerminate = null;
        }

        // handle termination request if present, otherwise delegate data handling to DeviceCommService
        if (dataConsumer.isTerminateRequest(newData)) {
            DeviceCommService.destroyInstance(this);
        } else {
            if (DeviceCommService.getInstance(this).isConnected()) {
                dataConsumer.consume(this, DeviceCommService.getInstance(this), newData);
            } else {
                Logger.logE(TAG, "GAPI client not connected");
            }

            // start "destroyer"
            mTimerTerminate = new Timer();
            mTimerTerminate.schedule(new TimerTask() {

                @Override
                public void run() {
                    DeviceCommService.destroyInstance(DeviceListenerService.this);
                    mTimerTerminate = null;
                }
            }, TimeUnit.SECONDS.toMillis(INACTVITY_TIMEOUT_SECONDS));
        }
    }


    /** Generic incomming data consumer with ability to detect termination request data before
     * delegating consumption of the data. */
    private interface DataConsumer<T> {
        /**
         * Processes new incoming data
         *
         * @param c context
         * @param rh comm service which will handle data consumption
         * @param newData data to consume
         */
        void consume(Context c, DeviceCommService rh, T newData);

        /**
         * Checks incoming data for termination request.
         *
         * @param newData data to consume
         * @return
         */
        boolean isTerminateRequest(T newData);
    }
}
