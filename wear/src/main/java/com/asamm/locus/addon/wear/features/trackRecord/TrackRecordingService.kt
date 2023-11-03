/*
 * Created by milan on 09.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.commands.CommandFloatExtra
import com.asamm.locus.addon.wear.communication.WearCommService
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.BodySensorsManager
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.RecordingSensorStore
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum
import com.asamm.locus.addon.wear.utils.WakeLockManager
import com.asamm.loggerV2.logD
import com.asamm.loggerV2.logE
import com.asamm.loggerV2.logW

/**
 * Foreground service which is started during track recording if the watch is providing
 * sensor data to the phone.
 */
class TrackRecordingService : Service() {

    // number of HR sensor restarts in case it is not measuring.
    private var numHrRestarts = 0

    private var wakeLockManager: WakeLockManager? = null

    private var sensors: BodySensorsManager? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sensors = BodySensorsManager()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START_FOREGROUND_SERVICE -> startInForeground()
            ACTION_STOP_FOREGROUND_SERVICE -> stopForegroundService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) {
            return
        }
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = ""
        notificationManager.createNotificationChannel(channel)
    }

    /* Used to build and start foreground service. */
    private fun startInForeground() {
        logD { "startInForeground()" }
        instance = this

        // create notification default intent.
        val intent = Intent(this, TrackRecordActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        initChannels(this)

        // create notification builder.
        val notificationBuilder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.hrm_rec_service_desc))
            .setSmallIcon(R.mipmap.ic_launcher_no1)
            .setContentIntent(pendingIntent)
            .setTicker(getString(R.string.app_name))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            // Android uses some pre-defined system-wide categories to determine whether to
            // disturb the user with a given notification when the user has enabled Do Not Disturb
            // mode. The Category determines the priority of the Ongoing Activity and new
            // categories were added recently to support Wear
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // create onGoing status
        val ongoingActivityStatus = Status.Builder()
            .addTemplate("Test template")
            .build()
        val ongoingActivity = OngoingActivity.Builder(applicationContext, 3456, notificationBuilder)
                .setAnimatedIcon(R.drawable.ic_launcher_base)
                .setStatus(ongoingActivityStatus)
                .build()
        ongoingActivity.apply(applicationContext)

        // start foreground service.
//        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(3456, notificationBuilder.build())
        startForeground(3456, notificationBuilder.build())
        if (wakeLockManager == null) {
            wakeLockManager = WakeLockManager(TAG).apply {
                acquireWakeLock(this@TrackRecordingService)
            }
        }
        Handler(mainLooper).postDelayed({ afterStart() }, 1000)
    }

    private fun afterStart() {
        val rsm = sensors
        if (rsm == null) {
            logE { "Could not finish TrackRecordingService#afterStart() call. Seems the service was destroyed right after the start." }
            return
        }

        // fake push first device keep alive, real ones start to come in a while
        WearCommService.instance.pushLastTransmitTimeFor(DataPath.TW_KEEP_ALIVE)
        if (!BodySensorsManager.checkAndRequestBodySensorsPermission(this)) {
            logW { "checkAndRequestBodySensorPermission() failed during service start." }
            stopForegroundService()
            return
        }

        val hrmConfig = PreferencesEx.hrmFeatureConfigState
        if (hrmConfig === FeatureConfigEnum.ENABLED) {
            if (rsm.startHrSensor(this, false)) {
                val handler = Handler(mainLooper)
                val sendHrmUpdate: Runnable = object : Runnable {
                    override fun run() {
                        val sensors = sensors ?: return
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - WearCommService.instance.getLastTransmitTimeFor(DataPath.TW_KEEP_ALIVE) >= DEVICE_KEEP_ALIVE_TIMEOUT_MS) {
                            logW { "DEVICE_KEEP_ALIVE has not come in time, terminating HRM service." }
                            stopForegroundService()
                            return
                        }
                        val hrm = RecordingSensorStore.hrm
                        val isValid = hrm.isValid
                        if (!isValid && currentTime - hrm.timestamp > (numHrRestarts + 1) * 90000) {
                            sensors.stopHrSensor(this@TrackRecordingService)
                            Handler(mainLooper).postDelayed({
                                sensors.startHrSensor(this@TrackRecordingService, true)
                                numHrRestarts++
                                logW { "HR sensor restarted. Attempt: $numHrRestarts" }
                            }, 500)
                        }
                        RecordingSensorStore.hrmDebug.sendTimestamp = currentTime
                        WearCommService.instance.sendDataItem(
                            DataPath.PUT_HEART_RATE,
                            CommandFloatExtra(if (hrm.isValid) hrm.value else Float.NaN)
                        )
                        handler.postDelayed(this, 2000)
                    }
                }
                handler.postDelayed(sendHrmUpdate, 2000)
            } else {
                stopForegroundService()
            }
        }
    }

    private fun stopForegroundService() {
        logD { "Stop foreground service." }

        // Stop foreground service and remove the notification.
        stopForeground(STOP_FOREGROUND_REMOVE)

        // Stop the foreground service.
        stopSelf()
        onDestroy()
    }

    override fun onDestroy() {
        instance = null
        sensors?.destroy(this)
        sensors = null
        wakeLockManager?.releaseWakeLock()
        wakeLockManager = null
        super.onDestroy()
    }

    companion object {

        private const val TAG = "TrackRecordingService"

        private const val DEFAULT_CHANNEL_ID = "lm_default_channel"

        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"

        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"

        private const val DEVICE_KEEP_ALIVE_TIMEOUT_MS = (20 * 60000).toLong()

        private var instance: TrackRecordingService? = null

        @JvmStatic
        val isRunning: Boolean
            get() = instance != null
    }
}