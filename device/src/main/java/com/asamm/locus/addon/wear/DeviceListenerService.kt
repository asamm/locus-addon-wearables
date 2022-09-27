/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
package com.asamm.locus.addon.wear

import android.content.Context
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayloadStorable
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import locus.api.utils.Logger
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Listener for incoming messages/data from the watch.
 */
class DeviceListenerService : WearableListenerService() {

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
//        Logger.logD(TAG, "onDataChanged($dataEventBuffer)")
        for (event in dataEventBuffer) {
//            Logger.logD(TAG, "  event.type: ${event.type}")
            @Suppress("ControlFlowWithEmptyBody")
            if (event.type == DataEvent.TYPE_CHANGED) {
                handleDataChange(dataEventConsumer, event)
            } else if (event.type == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
//        Logger.logD(
//                TAG, "onMessageReceived($messageEvent), " +
//                "node: ${messageEvent.sourceNodeId}, " +
//                "path: ${DataPath.fromPath(messageEvent.path)}"
//        )
        DeviceCommService.getInstance(this).nodeId = messageEvent.sourceNodeId
        val p = DataPath.fromPath(messageEvent.path)
                ?: return
        try {
            handleDataChange(
                    dataMessageConsumer, DataPayloadStorable(
                    p,
                    p.containerClass
                            .getConstructor(ByteArray::class.java)
                            .newInstance(messageEvent.data)
            )
            )
        } catch (e: Exception) {
            Logger.logE(TAG, "onMessageReceived($messageEvent)", e)
        }
    }

    //*************************************************
    // CONSUME DATA
    //*************************************************

    /**
     * DataEvent consumer
     */
    private val dataEventConsumer: DataConsumer<DataEvent> = object : DataConsumer<DataEvent> {

        override fun consume(c: Context, rh: DeviceCommService, newData: DataEvent) {
            rh.onDataChanged(c, newData)
        }

        override fun getPath(newData: DataEvent): DataPath? {
            return DataPath.valueOf(newData.dataItem)
        }
    }

    /**
     * DataEvent consumer
     */
    private val dataMessageConsumer: DataConsumer<DataPayloadStorable> = object : DataConsumer<DataPayloadStorable> {

        override fun getPath(newData: DataPayloadStorable): DataPath? {
            return newData.dataPath
        }

        override fun consume(c: Context, rh: DeviceCommService, newData: DataPayloadStorable) {
            if (newData.dataPath == DataPath.TD_GET_HAND_SHAKE) {
                Logger.logD(TAG, "handling hand shake")
            }
            if (newData.isValid) {
                rh.onDataReceived(c, newData.dataPath, newData.getData(newData.dataPath.containerClass))
            }
        }
    }

    /**
     * Helper method for data consumption using supplied consumer.
     */
    private fun <T> handleDataChange(dataConsumer: DataConsumer<T>, newData: T) {
        // check valid path. Unknown path = ignore received data
//        Logger.logD(
//                TAG, "handleDataChange($dataConsumer, $newData), " +
//                "path: ${dataConsumer.getPath(newData)}"
//        )
        val p = dataConsumer.getPath(newData)
                ?: return
        when (p) {
            DataPath.TD_KEEP_ALIVE,
            DataPath.TD_GET_HAND_SHAKE,
            DataPath.GET_TRACK_REC_PROFILES,
            DataPath.TD_GET_PERIODIC_DATA,
            DataPath.GET_PROFILE_ICON,
            DataPath.PUT_HEART_RATE -> {
                cancelTerminationTimer()

                // start "destroyer"
                timerTerminate = Timer().apply {
                    schedule(object : TimerTask() {

                        override fun run() {
                            DeviceCommService.destroyInstance()
                            timerTerminate = null
                        }
                    }, TimeUnit.SECONDS.toMillis(INACTIVITY_TIMEOUT_SECONDS.toLong()))
                }
                DeviceCommService.getInstance(this)
                        .doUpdateReceiveTimestamp()
                dataConsumer.consume(this, DeviceCommService.getInstance(this), newData)
            }
            else -> {
                dataConsumer.consume(this, DeviceCommService.getInstance(this), newData)
            }
        }
    }

    private fun cancelTerminationTimer() {
        timerTerminate?.cancel()
        timerTerminate = null
    }

    /**
     * Generic incoming data consumer with ability to detect termination request data before
     * delegating consumption of the data.
     */
    private interface DataConsumer<T> {

        /**
         * Get [DataPath] from incoming data.
         */
        fun getPath(newData: T): DataPath?

        /**
         * Processes new incoming data
         *
         * @param c       context
         * @param rh      comm service which will handle data consumption
         * @param newData data to consume
         */
        fun consume(c: Context, rh: DeviceCommService, newData: T)
    }

    companion object {

        // tag for logger
        private val TAG = DeviceListenerService::class.java.simpleName

        // Timeout in seconds after which communication services are killed
        // and locus periodic update disabled
        private const val INACTIVITY_TIMEOUT_SECONDS = 12

        // timer for termination
        private var timerTerminate: Timer? = null
    }
}