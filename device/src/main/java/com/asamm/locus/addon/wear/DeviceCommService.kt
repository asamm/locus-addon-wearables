package com.asamm.locus.addon.wear

import android.content.Context
import android.content.Intent
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.LocusWearCommService
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.*
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.*
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue.ExtendedTrackInfo
import com.asamm.locus.addon.wear.common.utils.Pair
import com.google.android.gms.wearable.DataEvent
import locus.api.android.ActionBasics
import locus.api.android.ActionMapTools.getMapPreview
import locus.api.android.MapPreviewParams
import locus.api.android.MapPreviewResult
import locus.api.android.features.periodicUpdates.UpdateContainer
import locus.api.android.objects.LocusInfo
import locus.api.android.objects.LocusVersion
import locus.api.android.objects.TrackRecordProfileSimple
import locus.api.android.objects.VersionCode
import locus.api.android.utils.LocusUtils.getActiveVersion
import locus.api.android.utils.LocusUtils.sendBroadcast
import locus.api.android.utils.exceptions.RequiredVersionMissingException
import locus.api.objects.extra.Location
import locus.api.utils.Logger
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Singleton class for handling communication between this application and the watch.
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */
class DeviceCommService private constructor(ctx: Context) 
    : LocusWearCommService(ctx) {
    
    @Volatile
    private var mAboutToBeDestroyed = false

    // Last received update from Locus
    @Volatile
    private var mLastUpdate: UpdateContainer? = null

    @Volatile
    private var mLastPeriodicUpdateReceivedMilis = 0L

    // time for periodic data transmission to wear device
    private var mPeriodicDataTimer: PeriodicDataTimer? = null
    private var mProfileIcons: TrackProfileIconValue.ValueList? = null

    /**
     * Marks last time that data was received from the watch
     */
    @Volatile
    private var mLastReceivedTime = System.currentTimeMillis()

    /**
     * is updated as side effect of some selected wear requests during handling
     */
    @Volatile
    private var lv: LocusVersion? = null

    /**
     * Default init.
     */
    init {
        Logger.logD(TAG, "Device comm service started.")
        try {
            lv = getActiveVersion(ctx)

            // start receiver
            startReceiver()
        } catch (e: RequiredVersionMissingException) {
            mLastUpdate = null
        }
    }

    override fun destroy() {
        super.destroy()

        // stop receiver
        stopReceiver()
    }

    /**
     * Update content with fresh updates.
     *
     * @param update update container
     */
    fun onUpdate(update: UpdateContainer?) {
        if (System.currentTimeMillis() - mLastReceivedTime > 15000) {
            destroyInstance()
        } else {
//            Logger.logD(TAG, "onUpdate(" + update + ")");
            mLastUpdate = update
            mLastPeriodicUpdateReceivedMilis = System.currentTimeMillis()
        }
    }

    /**
     * Notify about incorrect data.
     */
    fun onIncorrectData() {
        Logger.logD(TAG, "onIncorrectData()")
        mLastUpdate = null
    }

    fun onDataReceived(c: Context, path: DataPath, params: TimeStampStorable) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - mLastPeriodicUpdateReceivedMilis > REENABLE_PERIODIC_RECEIVER_TIMEOUT_MS) {
            Logger.logE(TAG, "Periodic receiver seems offline, trying to restart.")
            try {
                // update mLastUpdate manually and refresh mLastPeriodicUpdateReceivedMilis
                onUpdate(ActionBasics.getUpdateContainer(c, lv!!))
                startReceiver()
            } catch (e: RequiredVersionMissingException) {
                Logger.logW(TAG, "ActionTools.getDataUpdateContainer RequiredVersionMissingException")
            }
        }
        if (path != DataPath.PUT_HEART_RATE) {
            sendLocusMapLogD("Wear", "Received $path")
        }
        when (path) {
            DataPath.GET_HAND_SHAKE -> {
                val hndshk = loadHandShake(c)
                sendDataItem(DataPath.PUT_HAND_SHAKE, hndshk)
            }
            DataPath.GET_TRACK_REC_PROFILES -> {
                val profiles = loadTrackRecordProfiles(c)
                if (profiles.first != null) {
                    sendDataItem(DataPath.PUT_TRACK_REC_PROFILE_INFO, profiles.first)
                    mProfileIcons = profiles.second
                }
            }
            DataPath.GET_PROFILE_ICON -> {
                if (mProfileIcons == null) {
                    val profilesIcons = loadTrackRecordProfiles(c)
                    mProfileIcons = profilesIcons.second
                }
                val pigc = params as ProfileIconGetCommand
                if (mProfileIcons != null) {
                    for (icon in mProfileIcons!!.storables) {
                        if (pigc.profileId == icon.id) {
                            sendDataItem(DataPath.PUT_PROFILE_ICON, icon)
                            break
                        }
                    }
                }
            }
            DataPath.PUT_TRACK_REC_STATE_CHANGE -> {
                lv = getActiveVersion(c)
                val v = params as TrackRecordingStateChangeValue
                handleRecordingStateChanged(c, lv, v.recordingState, v.getmProfileName())
            }
            DataPath.GET_ADD_WAYPOINT -> {
                lv = getActiveVersion(c)
                handleAddWpt(c, lv, null)
                sendCommand(DataPath.PUT_ADD_WAYPOINT)
            }
            DataPath.GET_PERIODIC_DATA -> {
                lv = getActiveVersion(c)
                val v = params as PeriodicCommand
                handlePeriodicWearUpdate(c, v)
            }
            DataPath.POST_ADD_WAYPOINT -> {
                val wptName = (params as CommandStringExtra).value
                lv = getActiveVersion(c)
                handleAddWpt(c, lv, wptName)
                sendCommand(DataPath.PUT_ADD_WAYPOINT)
            }
            DataPath.PUT_HEART_RATE -> {
                val tag = "Wear HRM"
                val hrValue = params as CommandFloatExtra
                if (hrValue.isValid) {
                    val intent = Intent()
                    intent.action = "com.asamm.locus.DATA_TASK"
                    val hrmData = "{heart_rate:{data:" + hrValue.value + "}}"
                    intent.putExtra("tasks", hrmData)
                    sendBroadcast(c, intent)
                    sendLocusMapLogD(tag, hrmData)
                } else {
                    sendLocusMapLogD(tag, "Ignored, invalid data: " + hrValue.value)
                }
                val lastDevKeepAlive = getLastTransmitTimeFor(DataPath.DEVICE_KEEP_ALIVE)
                val lastUpdate = mLastUpdate
                if (lastUpdate != null && !lastUpdate.isTrackRecRecording) {
                    Logger.logD(TAG, "sending STOP_WATCH_TRACK_REC_SERVICE")
                    sendCommand(DataPath.STOP_WATCH_TRACK_REC_SERVICE)
                    pushLastTransmitTimeFor(DataPath.DEVICE_KEEP_ALIVE)
                } else if (currentTime - lastDevKeepAlive > DEVICE_KEEP_ALIVE_SEND_PERIOD_MS) {
                    sendCommand(DataPath.DEVICE_KEEP_ALIVE)
                    pushLastTransmitTimeFor(DataPath.DEVICE_KEEP_ALIVE)
                }
            }
            else -> {
            }
        }
    }

    private fun sendBroadcast(c: Context, i: Intent) {
        val tmpLv = getLocusVersion(c)
        if (tmpLv != null) {
            sendBroadcast(c, i, tmpLv)
        } else {
            Logger.logE(TAG, "Cannot send broadcast, LocusVersion is null.")
        }
    }

    private fun getLocusVersion(c: Context): LocusVersion? {
        if (lv == null) {
            lv = getActiveVersion(c)
        }
        return lv
    }

    private fun sendLocusMapLogD(tag: String, text: String) {
        // commented out for production release

        //        Intent intent = new Intent();
        //        intent.setAction("com.asamm.locus.DATA_TASK");
        //        intent.putExtra("tasks"," {log:{" +
        //                "type:\"d\"," +
        //                "tag: \"" + tag + "\"," +
        //                "value: \"" + text + "\"}" +
        //                "}");
        //        sendBroadcast(context, intent);
    }

    fun onDataChanged(c: Context, newData: DataEvent) {
        val item = newData.dataItem
        val path = DataPath.valueOf(item)
        val params = createStorableForPath<TimeStampStorable>(path, item)
        onDataReceived(c, path, params)
    }

    private fun destroyPeriodicDataTimer() {
        synchronized(TAG) {
            if (mPeriodicDataTimer != null) {
                mPeriodicDataTimer!!.cancel()
                mPeriodicDataTimer = null
            }
        }
    }

    private fun handlePeriodicWearUpdate(ctx: Context, command: PeriodicCommand) {
        var command: PeriodicCommand? = command
        if (command == null) {
            command = PeriodicCommand.createStopPeriodicUpdatesCommand()
        }
        val activityId = command!!.getmPeriodicActivityId()
        val periodMs = command.getmPeriodMs()
        val extra = command.getExtra<TimeStampStorable>()
        val task: TimerTask? = when (activityId) {
            PeriodicCommand.IDX_PERIODIC_TRACK_RECORDING -> object : TimerTask() {
                override fun run() {
                    if (mAboutToBeDestroyed) {
                        destroyPeriodicDataTimer()
                        return
                    }
                    val trv = loadTrackRecordingValue(ctx)
                    sendDataItem(DataPath.PUT_TRACK_REC, trv)
                }
            }
            PeriodicCommand.IDX_PERIODIC_MAP -> object : TimerTask() {
                override fun run() {
                    if (mAboutToBeDestroyed) {
                        destroyPeriodicDataTimer()
                        return
                    }
                    sendMapPeriodic(ctx, extra as MapPeriodicParams)
                }
            }
            else -> null
        }
        synchronized(TAG) {
            destroyPeriodicDataTimer()
            if (command.isStopRequest || task == null || mAboutToBeDestroyed) {
                return
            }
            mPeriodicDataTimer = PeriodicDataTimer(activityId, periodMs)
            mPeriodicDataTimer!!.schedule(task, 0, periodMs.toLong())
        }
    }

    private fun sendMapPeriodic(ctx: Context, extra: MapPeriodicParams) {
        // just ignore the request, application should recognize that Locus is missing.
        if (lv == null || lv!!.versionCode < VersionCode.UPDATE_14.vcFree) {
            return
        }
        var zoom = extra.zoom
        val data = mLastUpdate
        val offsetX = extra.offsetX
        val offsetY = extra.offsetY
        if (zoom == Const.ZOOM_UNKOWN.toInt()) {
            zoom = data?.mapZoomLevel ?: Const.ZOOM_DEFAULT
        }

        // request map
        var mapPreview: MapPreviewResult? = null
        var offsetCenter = ZERO_LOCATION
        var rotationDeg = 0
        val isZeroOffset = offsetX == 0 && offsetY == 0
        try {
            // if zero offset, ignore location from watch, use last available known location instead to render map
            if (offsetX != 0 || offsetY != 0) {
                offsetCenter = Location(extra.lastLatitude, extra.lastLongitude)
            }
            if (extra.isAutoRotate && extra.bearing != MapPeriodicParams.APPLY_DEVICE_BEARING) {
                rotationDeg = extra.bearing.toInt()
            } else if (data != null) {
                rotationDeg = if (abs(data.orientHeading) < 0.001f) {
                    (data.orientCourse + 0.5f).toInt()
                } else {
                    (data.orientHeading + 0.5f).toInt()
                }
            }
            val mapRotation = (360 - rotationDeg) % 360
            var correctedOffsetX = offsetX
            var correctedOffsetY = offsetY
            if ((offsetX != 0 || offsetY != 0) && extra.isAutoRotate && mapRotation != 0) {
                val sin = sin(rotationDeg / 180f * Math.PI).toFloat()
                val cos = cos(rotationDeg / 180f * Math.PI).toFloat()
                correctedOffsetX = (cos * offsetX - sin * offsetY + 0.5f).toInt()
                correctedOffsetY = (sin * offsetX + cos * offsetY + 0.5f).toInt()
            }
            mapPreview = getMapPreview(ctx, lv!!,
                    createMapPreviewParams(offsetCenter,
                            zoom, extra.width, extra.height,
                            correctedOffsetX, correctedOffsetY,
                            extra.densityDpi, if (extra.isAutoRotate) mapRotation else 0, extra.diagonal))
        } catch (e: RequiredVersionMissingException) {
            Logger.logE(TAG, "loadMapPreview($lv)")
        }
        val locusInfo = ActionBasics.getLocusInfo(ctx, lv!!)
        // if there is no offset applied, then return last know location
        val locToSend: Location = if (isZeroOffset) {
            val tmp = data?.locMyLocation
            tmp ?: ZERO_LOCATION
        } else {
            // if offset (panning) is currently applied, then just return last used offset center from the watch
            offsetCenter
        }
        val m = MapContainer(mapPreview, data, locusInfo, zoom, offsetX, offsetY, locToSend, rotationDeg.toShort())
        sendDataItem(DataPath.PUT_MAP, m)
    }

    private fun createMapPreviewParams(location: Location, zoom: Int, width: Int, height: Int,
                                       offsetX: Int, offsetY: Int, dpi: Int, rotation: Int,
                                       diagonal: Int): MapPreviewParams {
        val mpp = MapPreviewParams()
        mpp.zoom = zoom
        mpp.locCenter = location
        mpp.widthPx = width
        mpp.heightPx = height
        mpp.offsetX = offsetX
        mpp.offsetY = offsetY
        mpp.densityDpi = dpi
        mpp.rotation = rotation
        //mpp.setRadius(diagonal);
        return mpp
    }

    ///////////////////////////////////////////////////////////////////////////
    //      Value object create methods - reading from Locus API             //
    ///////////////////////////////////////////////////////////////////////////
    private fun handleAddWpt(ctx: Context, lv: LocusVersion?, wpName: String?) {
        try {
            if (wpName == null || wpName.trim { it <= ' ' }.isEmpty()) {
                ActionBasics.actionTrackRecordAddWpt(ctx, lv!!, "", true)
            } else {
                ActionBasics.actionTrackRecordAddWpt(ctx, lv!!, wpName, true)
            }
        } catch (e: RequiredVersionMissingException) {
            Logger.logE(TAG, "Invalid version $lv, can't add WPT", e)
            throw IllegalStateException(e)
        }
    }

    private fun handleRecordingStateChanged(ctx: Context, lv: LocusVersion?, newState: TrackRecordingStateEnum?, profile: String) {
        var currentRecState: TrackRecordingStateEnum? = null
        if (mLastUpdate != null) {
            currentRecState = if (mLastUpdate!!.isTrackRecPaused) TrackRecordingStateEnum.PAUSED else if (mLastUpdate!!.isTrackRecRecording) TrackRecordingStateEnum.RECORDING else TrackRecordingStateEnum.NOT_RECORDING
        }
        if (newState != null && currentRecState != newState) {
            try {
                when (newState) {
                    TrackRecordingStateEnum.PAUSED -> ActionBasics.actionTrackRecordPause(ctx, lv!!)
                    TrackRecordingStateEnum.RECORDING -> ActionBasics.actionTrackRecordStart(ctx, lv!!, profile)
                    TrackRecordingStateEnum.NOT_RECORDING -> ActionBasics.actionTrackRecordStop(ctx, lv!!, true)
                }
            } catch (e: RequiredVersionMissingException) {
                Logger.logE(TAG, "Invalid version $lv, cant change track recording state.", e)
            }
        }
        try {
            onUpdate(ActionBasics.getUpdateContainer(ctx, lv!!))
        } catch (e: RequiredVersionMissingException) {
            Logger.logW(TAG, "getDataUpdateContainer() - RequiredVersionMissingException")
        }
        val trv = loadTrackRecordingValue(ctx)
        sendDataItem(DataPath.PUT_TRACK_REC, trv)
    }

    /**
     * Load basic data from current Locus application.
     */
    private fun loadHandShake(ctx: Context): HandShakeValue {
        var locusInfo: LocusInfo? = null

        // check if object exists
        if (lv != null) {
            // handle info
            locusInfo = ActionBasics.getLocusInfo(ctx, lv!!)
        }

        // prepare container with data and send it
        return if (lv == null) HandShakeValue() else HandShakeValue(lv!!.versionCode,
                BuildConfig.VERSION_CODE - 1,  // - 1 to compensate for device suffix
                locusInfo != null && locusInfo.isRunning,
                locusInfo != null && locusInfo.isPeriodicUpdatesEnabled)
    }
    /**
     * Load recording profiles data from current Locus application.
     * @param nodeId ID of requester
     */
    /**
     * @param ctx
     * @return A pair of lists. First lists contains track profiles and description.
     * Second list contains track profile icons. Both lists are the same lenght and order.
     */
    private fun loadTrackRecordProfiles(ctx: Context): Pair<TrackProfileInfoValue.ValueList?, TrackProfileIconValue.ValueList> {
        var trackRecProfiles: List<TrackRecordProfileSimple?>? = null
        try {
            // read Locus info
            val lv = getActiveVersion(
                    ctx, lv!!.versionCode)

            // check if object exists
            if (lv != null) {
                // load also track record profiles
                trackRecProfiles = ActionBasics.getTrackRecordingProfiles(
                        ctx, lv)
            }
        } catch (e: RequiredVersionMissingException) {
            Logger.logE(TAG, "loadTrackRecordProfiles()", e)

            // clear data
            trackRecProfiles = null
        }
        val result = Pair(TrackProfileInfoValue.ValueList(), TrackProfileIconValue.ValueList())
        if (trackRecProfiles != null && !trackRecProfiles.isEmpty()) {
            val profiles = ArrayList<TrackProfileInfoValue>(trackRecProfiles.size)
            val icons = ArrayList<TrackProfileIconValue>(trackRecProfiles.size)
            for (profile in trackRecProfiles) {
                profiles.add(TrackProfileInfoValue(profile))
                icons.add(TrackProfileIconValue(profile))
            }
            result.first!!.storables = profiles
            result.second.storables = icons
        }
        return result
    }

    private fun loadTrackRecordingValue(ctx: Context): TrackRecordingValue {
        val infoAvailable = mLastUpdate != null
        val myLocAvailable = infoAvailable && mLastUpdate!!.locMyLocation != null
        val myLoc = if (myLocAvailable) mLastUpdate!!.locMyLocation else null
        val trackRec = infoAvailable && mLastUpdate!!.isTrackRecRecording
        val trackRecPause = infoAvailable && mLastUpdate!!.isTrackRecPaused
        val profileName = if (infoAvailable) mLastUpdate!!.trackRecProfileName else ""
        val speed = if (myLocAvailable) myLoc!!.speed else null
        val hrm = if (myLocAvailable) myLoc!!.sensorHeartRate else 0
        val altitude = if (myLocAvailable && myLoc!!.hasAltitude) myLoc.altitude.toFloat() else Float.NaN
        val stats = if (infoAvailable) mLastUpdate!!.trackRecStats else null
        var locusInfo: LocusInfo? = null
        locusInfo = ActionBasics.getLocusInfo(ctx, lv!!)
        return TrackRecordingValue(infoAvailable, trackRec, trackRecPause,
                profileName, stats, locusInfo, ExtendedTrackInfo(speed, hrm, altitude))
    }

    fun doUpdateReceiveTimestamp() {
        mLastReceivedTime = System.currentTimeMillis()
    }

    private class PeriodicDataTimer(private val activityId: Byte, private val periodMs: Int) : Timer()

    //*************************************************
    // UPDATES RECEIVER
    //*************************************************

    var refresher: Thread? = null

    private fun startReceiver() {
        // clear current refresher
        stopReceiver()

        // start new
        if (!mAboutToBeDestroyed) {
            refresher = Thread(Refresher()).apply {
                start()
            }
        }
    }

    private fun stopReceiver() {
        refresher = null
    }

    private inner class Refresher : Runnable {

        override fun run() {
            while (true) {
                // stop refresher
                if (Thread.currentThread() != refresher)  {
                    break
                }

                // request new data
                val uc = ActionBasics.getUpdateContainer(context, lv!!)
                if (uc != null) {
                    onUpdate(uc)
                } else {
                    onIncorrectData()
                }
            }
            Logger.logD(TAG, "refresher $refresher finished")
        }
    }

    companion object {

        @Volatile
        private var _instance: DeviceCommService? = null

        private const val REENABLE_PERIODIC_RECEIVER_TIMEOUT_MS = 5000L
        private const val DEVICE_KEEP_ALIVE_SEND_PERIOD_MS = 90000L

        // tag for logger
        private val TAG = DeviceCommService::class.java.simpleName
        private val ZERO_LOCATION = Location(0.0, 0.0)

        /**
         * Get instance of request handler.
         *
         * @param ctx current context
         * @return instance of handler
         */
        @JvmStatic
        fun getInstance(ctx: Context): DeviceCommService {
            if (_instance == null) {
                synchronized(TAG) {
                    if (_instance == null) {
                        // enable receiver
                        _instance = DeviceCommService(ctx)
                    }
                }
            }
            return _instance!!
        }

        /**
         * Destroy instance of receiver.
         *
         * @param ctx current context
         */
        @JvmStatic
        fun destroyInstance() {
            synchronized(TAG) {
                _instance?.let {
                    it.mAboutToBeDestroyed = true
                    it.destroy()

                    // disable watch periodic data timer
                    it.destroyPeriodicDataTimer()

                    // destroy instance
                    _instance = null
                    Logger.logD(TAG, "Destroying device comm instance")
                }
            }
        }

        val isInstance: Boolean
            get() = _instance != null
    }
}