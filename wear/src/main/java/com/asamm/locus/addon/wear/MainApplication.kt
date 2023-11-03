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
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand
import com.asamm.locus.addon.wear.communication.WearCommService
import com.asamm.locus.addon.wear.features.error.AppFailActivity
import com.asamm.locus.addon.wear.features.error.AppFailType
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordingService
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.LocusWearActivity.WearActivityState
import com.asamm.locus.addon.wear.utils.AppMemoryCache
import com.asamm.logger.Logger
import com.asamm.loggerV2.logD
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Core application instance for the Wear module.
 */
class MainApplication : Application(), ActivityLifecycleCallbacks {

    @Volatile
    lateinit var cache: AppMemoryCache
        private set

    override fun onCreate() {
        super.onCreate()
        initialize()

        // set logger
        Logger.registerLogger(object : Logger.ILogger {

            override fun logD(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.d(tag, msg)
            }

            override fun logI(tag: String, msg: String, vararg args: Any) {
                Log.i(tag, msg)
            }

            override fun logV(tag: String, msg: String, vararg args: Any) {
                Log.i(tag, msg)
            }

            override fun logW(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.w(tag, msg)
            }

            override fun logE(ex: Throwable?, tag: String, msg: String, vararg args: Any) {
                Log.e(tag, msg, ex)
            }
        })
        logD { "onCreate()" }

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
        Logger.d(TAG, "onDestroy()")

        // destroy instance of communication class
        WatchDog.getInstance().destroy()
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
            Logger.d(TAG, "setCurrentActivity($activity)")

            // if new activity is registered, end timer
            if (activity != null) {
                cancelTerminationTimer()
            }
            // register activity
            if (_currentActivity == null && activity != null) {
                Logger.d(TAG, " - application restored")
            } else if (_currentActivity != null && activity == null) {
                Logger.d(TAG, " - application terminated")
                setTerminationTimer()
            }
            val previous = _currentActivity
            _currentActivity = activity
            WatchDog.getInstance()?.onCurrentActivityChanged(
                    previous?.javaClass,
                    if (_currentActivity == null) null else _currentActivity!!.javaClass
            )
        }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        Logger.d(TAG, "onActivityCreated($activity, $bundle), test: ${activity is LocusWearActivity}")
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
                else -> { /* nothing to do */
                }
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
                    DataPath.TD_GET_PERIODIC_DATA,
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

    private fun reconnectIfNeeded() {
        WearCommService.instance.reconnectIfNeeded()
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
            if (p === DataPath.TW_KEEP_ALIVE) {
                WearCommService.instance.pushLastTransmitTimeFor(p)
            } else if (p === DataPath.STOP_WATCH_TRACK_REC_SERVICE) {
                val intent = Intent(ctx, TrackRecordingService::class.java)
                intent.action = TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE
                ctx.startService(intent)
            }
        }
    }
}