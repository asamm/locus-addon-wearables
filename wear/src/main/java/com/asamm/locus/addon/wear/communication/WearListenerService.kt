/**
 * Created by menion on 08/08/15.
 * Asamm Software, s. r. o.
 */
package com.asamm.locus.addon.wear.communication

import com.asamm.locus.addon.wear.AppStorageManager
import com.asamm.locus.addon.wear.BuildConfig
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.locus.addon.wear.WatchDog
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayloadStorable
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue
import com.asamm.locus.addon.wear.features.error.AppFailType
import com.google.android.gms.wearable.*
import locus.api.utils.Logger

/**
 * Service listening to Data API data changes
 */
class WearListenerService : WearableListenerService() {

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
//        Logger.logD(TAG, "onDataChanged($dataEventBuffer)")
        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val app = application
                if (app == null) {
                    Logger.logW(TAG, "Received data but application is null")
                    val p = DataPath.valueOf(event.dataItem)
                    if (p != null) {
                        val value = WearCommService.instance.createStorableForPath<TimeStampStorable>(p, event.dataItem)
                        MainApplication.handleActivityFreeCommRequests(this, p, value)
                    }
                } else {
                    handleDataEvent(
                            app = application as MainApplication,
                            dataItem = event.dataItem
                    )
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
//        Logger.logD(TAG, "onMessageReceived($messageEvent), " +
//                "node: ${messageEvent.sourceNodeId}, " +
//                "path: ${DataPath.fromPath(messageEvent.path)}")
        val p = DataPath.fromPath(messageEvent.path) ?: return
        try {
            handleDataChannelEvent(
                    app = application as MainApplication,
                    data = DataPayloadStorable(
                            p,
                            p.containerClass.getConstructor(ByteArray::class.java)
                                    .newInstance(messageEvent.data)
                    )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //*************************************************
    // CONSUME DATA
    //*************************************************

    private fun handleDataEvent(app: MainApplication, dataItem: DataItem) {
//        Logger.logD(TAG, "handleDataEvent($dataItem), path: ${dataItem.uri.path}")
        val path = DataPath.valueOf(dataItem)
        if (path != null) {
            val value = WearCommService.instance.createStorableForPath<TimeStampStorable>(path, dataItem)
            handleData(app, path, value)
        } else {
            Logger.logW(TAG, "unknown DataItem path " + dataItem.uri.path)
        }
    }

    private fun handleDataChannelEvent(app: MainApplication, data: DataPayloadStorable) {
        if (data.dataPath != null) {
            handleData(app, data.dataPath, data.getData(data.dataPath.containerClass))
        }
    }

    private fun handleData(app: MainApplication, p: DataPath?, value: TimeStampStorable?) {
//        Logger.logD(TAG, "handleData($p, $value)")
        val currentActivity = app.currentActivity
        if (currentActivity != null && p != null) {
            when (p) {
                DataPath.TW_PUT_HAND_SHAKE -> {
                    val handShakeValue = value as HandShakeValue?
                    if (!validateHandShakeOrFail(app, handShakeValue)) {
                        return
                    }
                }
                DataPath.TW_PUT_MAP -> app.cache.lastMapData = value as MapContainer?
                DataPath.PUT_TRACK_REC -> app.cache.setLastTrackRecState(
                        value as TrackRecordingValue?
                )
                DataPath.PUT_TRACK_REC_PROFILE_INFO -> {
                    run {
                        val profiles = value as TrackProfileInfoValue.ValueList?
                        if (profiles != null) {
                            app.cache.profiles = profiles.storables
                        }
                    }
                    run {
                        if (value is TrackProfileIconValue) {
                            AppStorageManager.persistIcon(this, value as TrackProfileIconValue?)
                        }
                        val profiles = app.cache.profiles
                        for (info in profiles) {
                            if (!AppStorageManager.isIconCached(this, info.id)) {
                                WearCommService.instance.sendDataItem(
                                        DataPath.GET_PROFILE_ICON,
                                        ProfileIconGetCommand(info.id)
                                )
                                break
                            }
                        }
                    }
                }
                DataPath.PUT_PROFILE_ICON -> {
                    if (value is TrackProfileIconValue) {
                        AppStorageManager.persistIcon(this, value as TrackProfileIconValue?)
                    }
                    val profiles = app.cache.profiles
                    for (info in profiles) {
                        if (!AppStorageManager.isIconCached(this, info.id)) {
                            WearCommService.instance.sendDataItem(
                                    DataPath.GET_PROFILE_ICON,
                                    ProfileIconGetCommand(info.id)
                            )
                            break
                        }
                    }
                }
                else -> {}
            }

            // forward event to WatchDog
            val wd = WatchDog.getInstance()
            wd?.onNewData(p, value)
            currentActivity.consumeNewData(p, value)
        }

        // special activity/context free requests handling
        if (p != null) {
            MainApplication.handleActivityFreeCommRequests(this, p, value)
        }
    }

    /**
     * Validate received handshake value.
     */
    private fun validateHandShakeOrFail(app: MainApplication, handShakeValue: HandShakeValue?): Boolean {
        // check handshake value itself
        if (handShakeValue == null) {
            Logger.logD(
                    TAG, "validateHandShakeOrFail(null), " +
                    "handshake empty, requesting new one"
            )
            WearCommService.instance.sendCommand(DataPath.TD_GET_HAND_SHAKE)
            return false
        }

        // validate handshake content
        if (handShakeValue.isEmpty
                || handShakeValue.locusVersion < Const.LOCUS_MIN_VERSION_CODE) {
            Logger.logD(
                    TAG, "validateHandShakeOrFail($handShakeValue), " +
                    "empty: ${handShakeValue.isEmpty}, " +
                    "locusVersion: ${handShakeValue.locusVersion}"
            )
            app.doApplicationFail(AppFailType.UNSUPPORTED_LOCUS_VERSION)
            return false
        }

        // TODO cejnar check required version codes before release
        val requiredAddonVersionLowerBound = 1010060 //BuildConfig.VERSION_CODE
        if (handShakeValue.addOnVersion < requiredAddonVersionLowerBound) {
            app.doApplicationFail(AppFailType.CONNECTION_ERROR_DEVICE_APP_OUTDATED)
        } else if (handShakeValue.addOnVersion > BuildConfig.VERSION_CODE) {
            app.doApplicationFail(AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED)
        }
        return true
    }

    companion object {

        // tag for logger
        private val TAG = WearListenerService::class.java.simpleName
    }
}