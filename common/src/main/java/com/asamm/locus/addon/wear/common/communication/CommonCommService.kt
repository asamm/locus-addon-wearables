/**
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication

import android.content.Context
import android.os.Bundle
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand
import com.asamm.locus.addon.wear.common.utils.Pair
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import locus.api.objects.Storable
import locus.api.utils.Logger
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Ancestor for device and wear communication services.
 * Derived classes are meant to be implemented as singleton.
 */
open class CommonCommService protected constructor(protected var context: Context) :
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private val MAX_DATA_ITEM_SIZE_B = 99 * 1024

    // Google API client
    protected var googleApiClient = GoogleApiClient.Builder(context.applicationContext)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build().apply {
                // connect the GoogleApiClient
                this.connect()
            }

    val isConnected: Boolean
        get() = googleApiClient.isConnected

    val isConnecting: Boolean
        get() = googleApiClient.isConnecting

    @Volatile
    var nodeId: String? = null

    /**
     * List of unsent data consisting of pairs of <PATH></PATH>, DATA>
     */
    private val unsentData = ConcurrentLinkedQueue<Pair<DataPath, TimeStampStorable>>()

    /**
     * This map can be used by the inheriting class to store and read information (depending on context)
     * about last time of receive or send event of selected Data type
     */
    private val mLastDataTransmitTime = HashMap<DataPath, Long>()

    protected open fun destroy() {
        // destroy GoogleAPIClient class
        googleApiClient.disconnect()
    }

    // CONNECTION

    override fun onConnected(bundle: Bundle?) {
        while (!unsentData.isEmpty()) {
            val toSend = unsentData.poll()
            sendDataItem(toSend.first, toSend.second)
        }
    }

    override fun onConnectionSuspended(i: Int) {
        // no handling required
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // no handling required
    }

    fun reconnectIfNeeded() {
//        Logger.logD(
//                "CommonCommService", "reconnectIfNeeded(), " +
//                "connected: " + isConnected + ", " +
//                "connecting: " + isConnecting
//        )
        if (!isConnected && !isConnecting) {
            googleApiClient.connect()
        }
    }

    // COMMUNICATION

    /**
     * Sends empty command to given [path].
     */
    fun sendCommand(path: DataPath) {
        sendDataItem(path, EmptyCommand())
    }

    /**
     * Sends given [data] to given [path].
     */
    fun sendDataItem(path: DataPath, data: TimeStampStorable) {
//        Logger.logD(
//                "CommonCommService",
//                "sendDataItem($path, $data), " +
//                        "connected: ${googleApiClient.isConnected}"
//        )
        if (!googleApiClient.isConnected) {
            unsentData.offer(Pair(path, data))
            reconnectIfNeeded()
        } else {
            sendDataItemWithoutConnectionCheck(path, data)
        }
    }

    /**
     * Sends payload, should be only called from this class and its subclasses
     */
    open fun sendDataItemWithoutConnectionCheck(path: DataPath, data: TimeStampStorable) {
        val request = PutDataRequest.create(path.path)
        val dataToSend = data.asBytes!!

        // check data size whether to send as and asset or plain data item
        if (dataToSend.size >= MAX_DATA_ITEM_SIZE_B) {
            request.putAsset(DataPath.DEFAULT_ASSET_KEY, Asset.createFromBytes(dataToSend))
        } else {
            request.data = dataToSend
        }
        if (path.isUrgent) {
            request.setUrgent()
        }
        Wearable.getDataClient(context).putDataItem(request)
    }

    /**
     * Transforms DataItem object to custom data container which type depends on given path type
     */
    fun <E : TimeStampStorable?> createStorableForPath(path: DataPath, item: DataItem): E? {
        val clazz = path.containerClass
        return if (clazz.simpleName == EmptyCommand::class.java.simpleName) {
            null
        } else try {
            val assets = item.assets
            val asset = assets[DataPath.DEFAULT_ASSET_KEY]
            if (asset == null) {
                val storable: Storable = clazz.newInstance()
                storable.read(item.data!!)
                storable as E
            } else {
                // blocking call - asset receive
                val assetInputStream = Wearable.getDataClient(context).getFdForAsset(
                        asset
                ).result.inputStream
                val baos = ByteArrayOutputStream()
                var nRead: Int
                val buffer = ByteArray(0x1000)
                while (assetInputStream.read(buffer, 0, buffer.size).also { nRead = it } != -1) {
                    baos.write(buffer, 0, nRead)
                }
                val storable: Storable = clazz.newInstance()
                storable.read(baos.toByteArray())
                storable as E
            }
        } catch (e: Exception) {
            Logger.logE("DataPath", "Constructor failed for " + path.name, e)
            null
        }
    }

    /**
     * Sets NOW as last trasmit time for data of type [dataPath]
     */
    fun pushLastTransmitTimeFor(dataPath: DataPath) {
        mLastDataTransmitTime[dataPath] = System.currentTimeMillis()
    }

    /**
     * @return last transmit time for the given data type. Will result in 0L if
     * no transmit time was given yet for this data type.
     */
    fun getLastTransmitTimeFor(dataPath: DataPath): Long {
        val result = mLastDataTransmitTime[dataPath]
        return result ?: 0L
    }

    companion object {

        // tag for logger
        private const val TAG = "CommonCommService"
    }
}