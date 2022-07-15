/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.*
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue
import com.asamm.locus.addon.wear.communication.WearCommService
import com.asamm.locus.addon.wear.features.error.AppFailActivity
import com.asamm.locus.addon.wear.features.error.AppFailType
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordingService
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.LocusWearActivity.WearActivityState
import com.asamm.locus.addon.wear.utils.AppMemoryCache
import com.google.android.gms.wearable.DataItem
import locus.api.utils.Logger
import locus.api.utils.Logger.registerLogger
import java.util.*
import java.util.concurrent.TimeUnit

class MainApplication : Application(), ActivityLifecycleCallbacks {

    @Volatile
    lateinit var cache: AppMemoryCache
        private set


    override fun onCreate() {
        super.onCreate()
        initialize()

        // set logger
        registerLogger(object : Logger.ILogger {

            override fun logI(tag: String, msg: String) {
                Log.i(tag, msg)
            }

            override fun logD(tag: String, msg: String) {
                Log.d(tag, msg)
            }

            override fun logW(tag: String, msg: String) {
                Log.w(tag, msg)
            }

            override fun logE(tag: String, msg: String) {
                Log.e(tag, msg)
            }

            override fun logE(tag: String, msg: String, e: Exception) {
                Log.e(tag, msg, e)
            }
        })
        Logger.logD(TAG, "onCreate()")

        // notify about create of app
        registerActivityLifecycleCallbacks(this)
        setTerminationTimer()
        cache = AppMemoryCache(this)

        // initialize communication service
        WearCommService.initialize(this)
        reconnectIfNeeded()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // do initialize once more so all is correctly set
        initialize()

        // call parent
        super.onConfigurationChanged(newConfig)
    }

    /**
     * Destroy instance of this application.
     */
    fun onDestroy() {
        Logger.logD(TAG, "destroyInstance()")

        // destroy instance of communication class
        WatchDog.getInstance().setAppFailCallback(null)
    }

    /**
     * Initialize basics.
     */
    private fun initialize() {
        app = this
    }

    //*************************************************
    // ACTIVITY LIFECYCLE
    //*************************************************

    @Volatile
    private var _currentActivity: LocusWearActivity? = null

    /**
     * Set reference to current activity.
     */
    var currentActivity: LocusWearActivity?
        get() = _currentActivity
        private set(activity) {
            if (_currentActivity != null
                    && activity != null
                    && !activity.isChildLocusWearActivity) {
                _currentActivity!!.finish()
            }
            Logger.logD(TAG, "setCurrentActivity($activity)")

            // if new activity is registered, end timer
            if (activity != null) {
                cancelTerminationTimer()
            }
            // register activity
            if (_currentActivity == null && activity != null) {
                Logger.logD(TAG, " - application restored")
            } else if (_currentActivity != null && activity == null) {
                Logger.logD(TAG, " - application terminated")
                setTerminationTimer()
            }
            val previous = _currentActivity
            _currentActivity = activity
            val wd = WatchDog.getInstance()
            wd?.onCurrentActivityChanged(
                    previous?.javaClass,
                    if (_currentActivity == null) null else _currentActivity!!.javaClass
            )
        }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        Logger.logD(TAG, "onActivityCreated($activity, $bundle)")
        if (activity !is LocusWearActivity) {
            return
        }
        reconnectIfNeeded()
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is LocusWearActivity) {
            return
        }
        WatchDog.getInstance().setAppFailCallback { reason: AppFailType ->
            doApplicationFail(reason)
        }
        reconnectIfNeeded()
    }

    override fun onActivityResumed(activity: Activity) {
        // handle special "Fail" activity
        if (activity is AppFailActivity) {
            cancelTerminationTimer()
            _currentActivity?.finish()
            _currentActivity = null
            onDestroy()
            return
        }

        // skip other activity types
        if (activity !is LocusWearActivity) {
            return
        }

        // set current activity
        val oldAct = _currentActivity
        if (oldAct == null) {
            currentActivity = activity
        } else {
            // check state of old custom activity
            when (oldAct.state) {
                WearActivityState.ON_START,
                WearActivityState.ON_PAUSE,
                WearActivityState.ON_STOP -> currentActivity = activity
            }
        }

        // register as "last used"
        PreferencesEx.lastActivity = activity.javaClass
    }

    override fun onActivityPaused(activity: Activity) {
        // nothing to do
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity !is LocusWearActivity) {
            return
        }
        // activity is not visible
        if (_currentActivity === activity) {
            currentActivity = null
        }
        // no activity is visible any more or currently shown activity does not consume periodic data
        if (_currentActivity?.isUsePeriodicData != true) {
            WearCommService.instance.sendDataItem(
                    DataPath.GET_PERIODIC_DATA,
                    PeriodicCommand.createStopPeriodicUpdatesCommand()
            )
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        // nothing to do
    }

    override fun onActivityDestroyed(activity: Activity) {
        // nothing to do
    }

    //*************************************************
    // COMMUNICATION
    //*************************************************

    /**
     * Active connected client, which has the app installed.
     */
    fun onConnected() {
        _currentActivity?.consumeNewData(DataPath.PUT_ON_CONNECTED_EVENT, null)
    }

    fun onConnectionSuspended() {
        reconnectIfNeeded()
    }

    private fun reconnectIfNeeded() {
        WearCommService.instance.reconnectIfNeeded()
    }

    //*************************************************
    // CONSUME DATA
    //*************************************************

    fun handleDataEvent(dataItem: DataItem) {
        val p = DataPath.valueOf(dataItem)
        if (p != null) {
            val value = WearCommService.instance.createStorableForPath<TimeStampStorable>(p, dataItem)
            handleData(p, value)
        } else {
            Logger.logW(TAG, "unknown DataItem path " + dataItem.uri.path)
        }
    }

    fun handleDataChannelEvent(data: DataPayloadStorable) {
        if (data.dataPath != null) {
            handleData(data.dataPath, data.getData(data.dataPath.containerClass))
        }
    }

    private fun handleData(p: DataPath?, value: TimeStampStorable?) {
        //Logger.logD(TAG, "handleData($p, $value)")
        val currentActivity = _currentActivity
        if (currentActivity != null && p != null) {
            when (p) {
                DataPath.PUT_HAND_SHAKE -> {
                    val handShakeValue = value as HandShakeValue?
                    if (!validateHandShakeOrFail(handShakeValue)) {
                        return
                    }
                }
                DataPath.PUT_MAP -> cache.lastMapData = value as MapContainer?
                DataPath.PUT_TRACK_REC -> cache.setLastTrackRecState(
                        value as TrackRecordingValue?
                )
                DataPath.PUT_TRACK_REC_PROFILE_INFO -> {
                    run {
                        val profiles = value as TrackProfileInfoValue.ValueList?
                        if (profiles != null) {
                            cache.profiles = profiles.storables
                        }
                    }
                    run {
                        if (value is TrackProfileIconValue) {
                            AppStorageManager.persistIcon(this, value as TrackProfileIconValue?)
                        }
                        val profiles = cache.profiles
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
                    val profiles = cache.profiles
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
            val wd = WatchDog.getInstance()
            wd?.onNewData(p, value)
            currentActivity.consumeNewData(p, value)
        }
        // special activity/context free requests handling
        if (p != null) {
            handleActivityFreeCommRequests(this, p, value)
        }
    }

    /**
     * Validate received handshake value.
     */
    private fun validateHandShakeOrFail(handShakeValue: HandShakeValue?): Boolean {
        // check handshake value itself
        if (handShakeValue == null) {
            Logger.logD(
                    TAG, "validateHandShakeOrFail($handShakeValue), " +
                    "handshake empty, requesting new one"
            )
            WearCommService.instance.sendCommand(DataPath.GET_HAND_SHAKE)
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
            doApplicationFail(AppFailType.UNSUPPORTED_LOCUS_VERSION)
            return false
        }

        // TODO cejnar check required version codes before release
        val requiredAddonVersionLowerBound = 1010060 //BuildConfig.VERSION_CODE
        if (handShakeValue.addOnVersion < requiredAddonVersionLowerBound) {
            doApplicationFail(AppFailType.CONNECTION_ERROR_DEVICE_APP_OUTDATED)
        } else if (handShakeValue.addOnVersion > BuildConfig.VERSION_CODE) {
            doApplicationFail(AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED)
        }
        return true
    }

    //*************************************************
    // WATCH DOG HELPERS
    //*************************************************

    fun sendDataWithWatchDog(
            request: DataPayload<out TimeStampStorable>,
            expectedResponse: DataPath?, timeoutToFailMs: Long) {
        addWatchDog(request, expectedResponse, timeoutToFailMs)
        WearCommService.instance.sendDataItem(request.path, request.storable)
    }

    fun sendDataWithWatchDog(
            path: DataPath, data: TimeStampStorable,
            expectedResponse: DataPath?, timeoutToFailMs: Long) {
        addWatchDog(
                DataPayload(path, data),
                expectedResponse,
                timeoutToFailMs
        )
        WearCommService.instance.sendDataItem(path, data)
    }

    fun sendDataWithWatchDogConditionable(
            request: DataPayload<TimeStampStorable>,
            expectedResponse: DataPath?, timeoutToFailMs: Long,
            responsePredicate: WatchDogPredicate<out TimeStampStorable>?) {
        val act = _currentActivity
        val wd = WatchDog.getInstance()
        if (wd != null && act != null) {
            wd.startWatchingWithCondition(
                    act.javaClass, request,
                    expectedResponse, timeoutToFailMs, responsePredicate
            )
        }
        WearCommService.instance.sendDataItem(request.path, request.storable)
    }

    fun addWatchDog(
            request: DataPayload<out TimeStampStorable>,
            expectedResponse: DataPath?, timeoutToFailMs: Long) {
        val act = _currentActivity
        val wd = WatchDog.getInstance()
        if (wd != null && act != null) {
            wd.startWatching(act.javaClass, request, expectedResponse, timeoutToFailMs)
        }
    }

    //*************************************************
    // TOOLS
    //*************************************************

    private fun setTerminationTimer() {
        // start timer
        val terminateTask: TimerTask = object : TimerTask() {
            override fun run() {
                onDestroy()
            }
        }

        // execute timer
        timerTerminate = Timer().apply {
            schedule(
                    terminateTask,
                    TimeUnit.SECONDS.toMillis(10)
            )
        }
    }

    /**
     * Cancel running termination timer.
     */
    private fun cancelTerminationTimer() {
        timerTerminate?.cancel()
        timerTerminate = null
    }

    /**
     * Show "Application failed" screen with explanation.
     */
    fun doApplicationFail(reason: AppFailType) {
        startActivity(Intent(this, AppFailActivity::class.java).apply {
            putExtra(AppFailActivity.ARG_ERROR_TYPE, reason.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    companion object {

        // tag for logger
        private val TAG = MainApplication::class.java.simpleName

        // timer for termination
        private var timerTerminate: Timer? = null

        @Volatile
        @SuppressLint("StaticFieldLeak")
        lateinit var app: MainApplication

        /**
         * This method contains some logic to handle requests that are not dependent
         * on specific activity context.
         * The method is especially used to handle new communication problems and lifecycle handling
         * that arise as a result of standalone foreground HRM/track rec service which is quite
         * independent from the rest of the application.
         */
        @JvmStatic
        fun handleActivityFreeCommRequests(ctx: Context, p: DataPath, value: TimeStampStorable?) {
            if (p === DataPath.DEVICE_KEEP_ALIVE) {
                WearCommService.instance.pushLastTransmitTimeFor(p)
            } else if (p === DataPath.STOP_WATCH_TRACK_REC_SERVICE) {
                val intent = Intent(ctx, TrackRecordingService::class.java)
                intent.action = TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE
                ctx.startService(intent)
            }
        }
    }
}