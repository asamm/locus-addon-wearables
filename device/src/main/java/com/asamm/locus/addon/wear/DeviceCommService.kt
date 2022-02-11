package com.asamm.locus.addon.wear

import android.annotation.SuppressLint
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
class DeviceCommService private constructor(ctx: Context) : LocusWearCommService(ctx) {

    @Volatile
    private var aboutToBeDestroyed = false

    // Last received update from Locus
    @Volatile
    private var lastUpdateContainer: UpdateContainer? = null

    @Volatile
    private var lastUpdateContainerReceived = 0L

    // time for periodic data transmission to wear device
    private var periodicDataTimer: PeriodicDataTimer? = null
    private var profileIcons: TrackProfileIconValue.ValueList? = null

    /**
     * Marks last time that data was received from the watch
     */
    @Volatile
    private var lastReceivedTime = System.currentTimeMillis()

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
            lastUpdateContainerReceived = System.currentTimeMillis()
            startRefresher()
        } catch (e: RequiredVersionMissingException) {
            lastUpdateContainer = null
        }
    }

    override fun destroy() {
        Logger.logD(TAG, "destroy()")
        super.destroy()

        // stop receiver
        stopRefresher()
    }

    // DEVICE LISTENER EVENTS

    /**
     * Handle data registered over `onDataChanged` callback.
     */
    fun onDataChanged(c: Context, newData: DataEvent) {
        val item = newData.dataItem
        val path = DataPath.valueOf(item)
        if (path == null) {
            Logger.logD(
                TAG, "onDataChanged($c, $newData), " +
                        "invalid path"
            )
            return
        }

        // handle data
        onDataReceived(c, path, createStorableForPath(path, item))
    }

    /**
     * Handle data registered primary over `onMessageReceived` callback.
     */
    fun onDataReceived(ctx: Context, path: DataPath, params: TimeStampStorable?) {
        Logger.logD(TAG, "onDataReceived($ctx, $path, $params)")

        // check if refresher works and restart if needed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateContainerReceived > REENABLE_PERIODIC_RECEIVER_TIMEOUT_MS) {
            Logger.logE(TAG, "Periodic receiver seems offline, trying to restart.")
            try {
                // update mLastUpdate manually and refresh mLastPeriodicUpdateReceivedMilis
                onNewUpdateContainer(ActionBasics.getUpdateContainer(ctx, lv!!))
                startRefresher()
            } catch (e: RequiredVersionMissingException) {
                Logger.logW(
                    TAG,
                    "ActionTools.getDataUpdateContainer RequiredVersionMissingException"
                )
            }
        }

        // send logs to Locus Map app logger
        if (path != DataPath.PUT_HEART_RATE) {
            sendLocusMapLogD("Wear", "Received $path")
        }

        // handle data
        when (path) {
            DataPath.GET_HAND_SHAKE -> {
                handleGetHandShake(ctx)
            }
            DataPath.GET_TRACK_REC_PROFILES -> {
                handleGetTrackRecProfiles(ctx)
            }
            DataPath.GET_PROFILE_ICON -> {
                handleGetTrackRecProfileIcon(ctx, params)
            }
            DataPath.PUT_TRACK_REC_STATE_CHANGE -> {
                lv = getActiveVersion(ctx)
                val v = params as TrackRecordingStateChangeValue
                handleRecordingStateChanged(ctx, lv, v.recordingState, v.getmProfileName())
            }
            DataPath.GET_ADD_WAYPOINT -> {
                handleAddWpt(ctx, null)
            }
            DataPath.POST_ADD_WAYPOINT -> {
                handleAddWpt(ctx, (params as CommandStringExtra).value)
            }
            DataPath.GET_PERIODIC_DATA -> {
                lv = getActiveVersion(ctx)
                val v = params as PeriodicCommand
                handlePeriodicWearUpdate(ctx, v)
            }
            DataPath.PUT_HEART_RATE -> {
                val tag = "Wear HRM"
                val hrValue = params as CommandFloatExtra
                if (hrValue.isValid) {
                    val intent = Intent()
                    intent.action = "com.asamm.locus.DATA_TASK"
                    val hrmData = "{heart_rate:{data:" + hrValue.value + "}}"
                    intent.putExtra("tasks", hrmData)
                    sendBroadcast(ctx, intent)
                    sendLocusMapLogD(tag, hrmData)
                } else {
                    sendLocusMapLogD(tag, "Ignored, invalid data: " + hrValue.value)
                }
                val lastDevKeepAlive = getLastTransmitTimeFor(DataPath.DEVICE_KEEP_ALIVE)
                val lastUpdate = lastUpdateContainer
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
                // nothing to do
            }
        }
    }

    // HAND SHAKE

    /**
     * Handle request to "Hand shake".
     */
    private fun handleGetHandShake(ctx: Context) {
        sendDataItem(
            DataPath.PUT_HAND_SHAKE,
            loadHandShake(ctx)
        )
    }

    /**
     * Load basic data from current Locus application.
     */
    private fun loadHandShake(ctx: Context): HandShakeValue {
        // check if object exists
        var locusInfo: LocusInfo? = null
        if (lv != null) {
            // handle info
            locusInfo = ActionBasics.getLocusInfo(ctx, lv!!)
        }

        // prepare container with data and send it
        Logger.logD(TAG, "loadHandShake($ctx), lv: $lv, $locusInfo")
        return if (lv == null) HandShakeValue() else HandShakeValue(
            lv!!.versionCode,
            // - 1 to compensate for device suffix
            BuildConfig.VERSION_CODE - 1,
            locusInfo != null && locusInfo.isRunning
        )
    }

    // TRACK RECORDING

    private fun handleGetTrackRecProfiles(ctx: Context) {
        val profiles = loadTrackRecordProfiles(ctx)
        if (profiles.first != null) {
            sendDataItem(DataPath.PUT_TRACK_REC_PROFILE_INFO, profiles.first)
            profileIcons = profiles.second
        }
    }

    private fun handleGetTrackRecProfileIcon(ctx: Context, params: TimeStampStorable?) {
        if (profileIcons == null) {
            val profilesIcons = loadTrackRecordProfiles(ctx)
            profileIcons = profilesIcons.second
        }
        val pigc = params as ProfileIconGetCommand
        if (profileIcons != null) {
            for (icon in profileIcons!!.storables) {
                if (pigc.profileId == icon.id) {
                    sendDataItem(DataPath.PUT_PROFILE_ICON, icon)
                    break
                }
            }
        }
    }

    /**
     * Load recording profiles data from current Locus application.
     *
     * @return A pair of lists. First lists contains track profiles and description.
     * Second list contains track profile icons. Both lists are the same lenght and order.
     */
    private fun loadTrackRecordProfiles(ctx: Context)
            : Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> {
        var trackRecProfiles: List<TrackRecordProfileSimple>? = null
        try {
            // read Locus info
            val lv = getActiveVersion(ctx, lv!!.versionCode)
            if (lv != null) {
                // load also track record profiles
                trackRecProfiles = ActionBasics.getTrackRecordingProfiles(ctx, lv)
            }
        } catch (e: RequiredVersionMissingException) {
            Logger.logE(TAG, "loadTrackRecordProfiles()", e)

            // clear data
            trackRecProfiles = null
        }

        // return result
        val result = Pair(TrackProfileInfoValue.ValueList(), TrackProfileIconValue.ValueList())
        if (trackRecProfiles != null && trackRecProfiles.isNotEmpty()) {
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

    // ADD WAYPOINT

    private fun handleAddWpt(ctx: Context, wpName: String?) {
        lv = getActiveVersion(ctx)
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

        // send data
        sendCommand(DataPath.PUT_ADD_WAYPOINT)
    }

    //*************************************************
    // HELPERS
    //*************************************************

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

    private fun destroyPeriodicDataTimer() {
        synchronized(TAG) {
            periodicDataTimer?.cancel()
            periodicDataTimer = null
        }
    }

    private fun handlePeriodicWearUpdate(ctx: Context, command: PeriodicCommand) {
        val activityId = command.getmPeriodicActivityId()
        val periodMs = command.getmPeriodMs()
        val extra = command.getExtra<TimeStampStorable>()
        val task: TimerTask? = when (activityId) {
            PeriodicCommand.IDX_PERIODIC_TRACK_RECORDING -> object : TimerTask() {

                override fun run() {
                    if (aboutToBeDestroyed) {
                        destroyPeriodicDataTimer()
                        return
                    }
                    val trv = loadTrackRecordingValue(ctx)
                    sendDataItem(DataPath.PUT_TRACK_REC, trv)
                }
            }
            PeriodicCommand.IDX_PERIODIC_MAP -> object : TimerTask() {
                override fun run() {
                    if (aboutToBeDestroyed) {
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
            if (command.isStopRequest || task == null || aboutToBeDestroyed) {
                return
            }
            periodicDataTimer = PeriodicDataTimer().apply {
                schedule(task, 0, periodMs.toLong())
            }
        }
    }

    private fun sendMapPeriodic(ctx: Context, extra: MapPeriodicParams) {
        // just ignore the request, application should recognize that Locus is missing.
        if (lv == null || lv!!.versionCode < VersionCode.UPDATE_14.vcFree) {
            return
        }
        var zoom = extra.zoom
        val data = lastUpdateContainer
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
            mapPreview = getMapPreview(
                ctx, lv!!,
                createMapPreviewParams(
                    offsetCenter,
                    zoom, extra.width, extra.height,
                    correctedOffsetX, correctedOffsetY,
                    extra.densityDpi, if (extra.isAutoRotate) mapRotation else 0
                )
            )
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
        val m = MapContainer(
            mapPreview,
            data,
            locusInfo,
            zoom,
            offsetX,
            offsetY,
            locToSend,
            rotationDeg.toShort()
        )
        sendDataItem(DataPath.PUT_MAP, m)
    }

    private fun createMapPreviewParams(
        location: Location, zoom: Int, width: Int, height: Int,
        offsetX: Int, offsetY: Int, dpi: Int, rotation: Int
    )
            : MapPreviewParams {
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


    private fun handleRecordingStateChanged(
        ctx: Context,
        lv: LocusVersion?,
        newState: TrackRecordingStateEnum?,
        profile: String
    ) {
        var currentRecState: TrackRecordingStateEnum? = null
        if (lastUpdateContainer != null) {
            currentRecState = when {
                lastUpdateContainer!!.isTrackRecPaused -> {
                    TrackRecordingStateEnum.PAUSED
                }
                lastUpdateContainer!!.isTrackRecRecording -> {
                    TrackRecordingStateEnum.RECORDING
                }
                else -> {
                    TrackRecordingStateEnum.NOT_RECORDING
                }
            }
        }
        if (newState != null && currentRecState != newState) {
            try {
                when (newState) {
                    TrackRecordingStateEnum.PAUSED -> ActionBasics.actionTrackRecordPause(ctx, lv!!)
                    TrackRecordingStateEnum.RECORDING -> ActionBasics.actionTrackRecordStart(
                        ctx,
                        lv!!,
                        profile
                    )
                    TrackRecordingStateEnum.NOT_RECORDING -> ActionBasics.actionTrackRecordStop(
                        ctx,
                        lv!!,
                        true
                    )
                }
            } catch (e: RequiredVersionMissingException) {
                Logger.logE(TAG, "Invalid version $lv, cant change track recording state.", e)
            }
        }
        try {
            onNewUpdateContainer(ActionBasics.getUpdateContainer(ctx, lv!!))
        } catch (e: RequiredVersionMissingException) {
            Logger.logW(TAG, "getDataUpdateContainer() - RequiredVersionMissingException")
        }
        val trv = loadTrackRecordingValue(ctx)
        sendDataItem(DataPath.PUT_TRACK_REC, trv)
    }

    private fun loadTrackRecordingValue(ctx: Context): TrackRecordingValue {
        val infoAvailable = lastUpdateContainer != null
        val myLocAvailable = lastUpdateContainer?.locMyLocation != null
        val myLoc = if (myLocAvailable) lastUpdateContainer!!.locMyLocation else null
        val trackRec = infoAvailable && lastUpdateContainer!!.isTrackRecRecording
        val trackRecPause = infoAvailable && lastUpdateContainer!!.isTrackRecPaused
        val profileName = if (infoAvailable) lastUpdateContainer!!.trackRecProfileName else ""
        val speed = if (myLoc?.hasSpeed == true) myLoc.speed else 0.0f
        val hrm = if (myLoc?.hasSensorHeartRate == true) myLoc.sensorHeartRate else 0.toShort()
        val altitude = if (myLoc?.hasAltitude == true) myLoc.altitude else 0.0
        val stats = if (infoAvailable) lastUpdateContainer!!.trackRecStats else null
        val locusInfo = ActionBasics.getLocusInfo(ctx, lv!!)
        return TrackRecordingValue(
            infoAvailable, trackRec, trackRecPause,
            profileName, stats, locusInfo, ExtendedTrackInfo(speed, hrm, altitude.toFloat())
        )
    }

    fun doUpdateReceiveTimestamp() {
        lastReceivedTime = System.currentTimeMillis()
    }

    private class PeriodicDataTimer : Timer()

    //*************************************************
    // UPDATES RECEIVER
    //*************************************************

    var refresher: Thread? = null

    private fun startRefresher() {
        Logger.logD(
            TAG, "startRefresher(), " +
                    "aboutToBeDestroyed: $aboutToBeDestroyed"
        )
        // clear current refresher
        stopRefresher()

        // start new
        if (!aboutToBeDestroyed) {
            refresher = Thread(Refresher())
            refresher?.start()
        }
    }

    private fun stopRefresher() {
        Logger.logD(
            TAG, "stopRefresher(), " +
                    "current: ${refresher?.hashCode()}"
        )
        refresher = null
    }

    /**
     * Update content with fresh updates.
     */
    private fun onNewUpdateContainer(update: UpdateContainer?) {
        Logger.logD(TAG, "onUpdate($update)")
        if (System.currentTimeMillis() - lastReceivedTime > 15000) {
            destroyInstance()
        } else {
            lastUpdateContainer = update
            lastUpdateContainerReceived = System.currentTimeMillis()
        }
    }

    /**
     * Notify about incorrect data.
     */
    fun onIncorrectData() {
        Logger.logD(TAG, "onIncorrectData()")
        lastUpdateContainer = null
    }

    private inner class Refresher : Runnable {

        override fun run() {
            Logger.logD(TAG, "refresher: started: ${Thread.currentThread().hashCode()}")
            while (true) {
                Thread.sleep(1000)
                // stop refresher
                if (Thread.currentThread() != refresher) {
                    break
                }

                // request new data
                val uc = ActionBasics.getUpdateContainer(context, lv!!)
                if (uc != null) {
                    onNewUpdateContainer(uc)
                } else {
                    onIncorrectData()
                }
            }
            Logger.logD(TAG, "refresher $refresher finished")
        }
    }

    companion object {

        @Volatile
        @SuppressLint("StaticFieldLeak")
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
         */
        @JvmStatic
        fun destroyInstance() {
            synchronized(TAG) {
                _instance?.let {
                    it.aboutToBeDestroyed = true
                    it.destroy()

                    // disable watch periodic data timer
                    it.destroyPeriodicDataTimer()

                    // destroy instance
                    _instance = null
                    Logger.logD(TAG, "Destroying device comm instance")
                }
            }
        }
    }
}