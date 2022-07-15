package com.asamm.locus.addon.wear.communication

import android.annotation.SuppressLint
import android.os.Bundle
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.locus.addon.wear.WatchDog
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.LocusWearCommService
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.utils.TriStateLogicEnum
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.wearable.CapabilityApi
import com.google.android.gms.wearable.CapabilityApi.CapabilityListener
import com.google.android.gms.wearable.CapabilityApi.GetCapabilityResult
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeApi.GetConnectedNodesResult
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

/**
 * Singleton instance handling wear-device communication
 *
 *
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
class WearCommService private constructor(val app: MainApplication)
    : LocusWearCommService(app), CapabilityListener {

    @Volatile
    private var mLastSentDataTimestamp = 0L

    /**
     * @return UNKNOWN if not known yet, TRUE if cabable client present, FALSE if capable device not found
     */
    var isAppInstalledOnDevice = TriStateLogicEnum.UNKNOWN
        private set

    override fun destroy() {
        if (googleApiClient != null && googleApiClient.isConnected) {
            Wearable.CapabilityApi.removeCapabilityListener(
                    googleApiClient,
                    this,
                    CAPABILITY_PHONE_APP
            )
        }
        super.destroy()
    }

    override fun onConnected(bundle: Bundle?) {
        super.onConnected(bundle)
        // Set up listeners for capability changes (install/uninstall of remote app).
        Wearable.CapabilityApi.addCapabilityListener(
                googleApiClient,
                this,
                CAPABILITY_PHONE_APP
        )
        checkIfPhoneHasApp()
        app.onConnected()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        super.onConnectionFailed(connectionResult)
        app.onConnectionSuspended()
    }

    override fun onConnectionSuspended(i: Int) {
        super.onConnectionSuspended(i)
        app.onConnectionSuspended()
    }

    fun getConnectedNodes(resultCallback: ResultCallback<GetConnectedNodesResult>) {
        Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .setResultCallback(resultCallback, 1000, TimeUnit.MILLISECONDS)
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        val capableNodes = capabilityInfo.nodes
        isAppInstalledOnDevice =
                if (capableNodes.size > 0) TriStateLogicEnum.TRUE else TriStateLogicEnum.FALSE
        if (isAppInstalledOnDevice == TriStateLogicEnum.TRUE) {
            val nodeIt: Iterator<Node> = capableNodes.iterator()
            mNodeId = null
            while (mNodeId == null && nodeIt.hasNext()) {
                val n = nodeIt.next()
                mNodeId = if (n.isNearby) n.id else null
            }
        }
        app.onConnected()
    }

    override fun sendDataItemWithoutConnectionCheck(path: DataPath, data: TimeStampStorable) {
        val currentTime = System.currentTimeMillis()
        // if keep alive command but some other command was sent recently then ignore this
        // command to save bandwidth
        if (path === DataPath.GET_KEEP_ALIVE &&
                currentTime - mLastSentDataTimestamp <= WatchDog.WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS) {
            return
        }
        // if sending other data than keep alive command, write current time to
        // postpone keep alive thread
        if (path !== DataPath.GET_KEEP_ALIVE) {
            mLastSentDataTimestamp = currentTime
        }
        super.sendDataItemWithoutConnectionCheck(path, data)
    }

    private fun checkIfPhoneHasApp() {
        val pendingResult = Wearable.CapabilityApi.getCapability(
                googleApiClient,
                CAPABILITY_PHONE_APP,
                CapabilityApi.FILTER_ALL
        )
        pendingResult.setResultCallback { getCapabilityResult: GetCapabilityResult ->
            if (getCapabilityResult.status.isSuccess) {
                onCapabilityChanged(getCapabilityResult.capability)
            }
        }
    }

    companion object {

        private const val CAPABILITY_PHONE_APP = "verify_remote_wear_for_locus_map_phone_app"

        /**
         * Instance of the wear connection service.
         */
        @JvmStatic
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: WearCommService
            private set


        fun initialize(c: MainApplication) {
            if (!::instance.isInitialized) {
                instance = WearCommService(c)
            }
        }

        @JvmStatic
        fun destroyInstance() {
            instance.destroy()
        }
    }
}