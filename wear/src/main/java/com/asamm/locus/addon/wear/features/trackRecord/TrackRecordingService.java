/*
 * Created by milan on 09.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.trackRecord;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.commands.CommandFloatExtra;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.HrmValue;
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.RecordingSensorManager;
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.RecordingSensorStore;
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum;
import com.asamm.locus.addon.wear.features.settings.PreferencesEx;
import com.asamm.locus.addon.wear.utils.WakeLockManager;

import locus.api.utils.Logger;

import static com.asamm.locus.addon.wear.common.communication.DataPath.DEVICE_KEEP_ALIVE;
import static com.asamm.locus.addon.wear.common.communication.DataPath.PUT_HEART_RATE;

/**
 * Foreground service which is started during track recording if the watch is providing
 * sensor data to the phone.
 */
public class TrackRecordingService extends Service {

    private static final String TAG = "TrackRecordingService";
    private static final String DEFAULT_CHANNEL_ID = "lm_default_channel";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final long DEVICE_KEEP_ALIVE_TIMEOUT_MS = 20 * 60_000;

    // number of HR sensor restarts in case it is not measuring.
    private int numHrRestarts = 0;

    private static TrackRecordingService instance = null;

    private WakeLockManager wakeLockManager;

    private RecordingSensorManager mSensors;


    public static boolean isRunning() {
        return instance != null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mSensors = new RecordingSensorManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startInForeground();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("");
        notificationManager.createNotificationChannel(channel);
    }

    /* Used to build and start foreground service. */
    private void startInForeground() {
        Logger.INSTANCE.logD(TAG, "Start foreground service.");

        instance = this;

        // Create notification default intent.
        Intent intent = new Intent(this, TrackRecordActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        initChannels(this);
        // Create notification builder.
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID);

        b.setContentTitle(getString(R.string.app_name));
        b.setContentText(getString(R.string.hrm_rec_service_desc));
        b.setSmallIcon(R.mipmap.ic_launcher_no1);
        b.setContentIntent(pendingIntent);
        b.setTicker(getString(R.string.app_name));

        // Build the notification.
        Notification notification = b.build();


        // Start foreground service.
        startForeground(3456, notification);

        if (wakeLockManager == null) {
            wakeLockManager = new WakeLockManager(TAG);
            // acquire wakelock
            wakeLockManager.acquireWakeLock(this);
        }

        new Handler(getMainLooper()).postDelayed(this::afterStart, 1000);
    }

    private void afterStart() {
        final RecordingSensorManager rsm = mSensors;
        if (rsm == null) {
            Logger.INSTANCE.logE(TAG, "Could not finish TrackRecordingService#afterStart() call. Seems the service was destroyed right after the start.");
            return;
        }
        // fake push first device keep alive, real ones start to come in a while
        WearCommService.getInstance().pushLastTransmitTimeFor(DEVICE_KEEP_ALIVE);

        if (!RecordingSensorManager.checkAndRequestBodySensorPermission(this)) {
            Logger.INSTANCE.logW(TAG, "checkAndRequestBodySensorPermission() failed during service start.");
            stopForegroundService();
            return;
        }
        FeatureConfigEnum hrmConfig = PreferencesEx.getHrmFeatureConfig();
        if (hrmConfig == FeatureConfigEnum.ENABLED) {
            if (rsm.startHrSensor(this, false)) {
                final Handler handler = new Handler(getMainLooper());
                Runnable sendHrmUpdate = new Runnable() {
                    @Override
                    public void run() {
                        RecordingSensorManager sensors = mSensors;
                        if (sensors == null)
                            return;

                        final long currentTime = System.currentTimeMillis();
                        if (currentTime - WearCommService.getInstance().getLastTransmitTimeFor(DEVICE_KEEP_ALIVE) >= DEVICE_KEEP_ALIVE_TIMEOUT_MS) {
                            Logger.INSTANCE.logW(TAG, "DEVICE_KEEP_ALIVE has not come in time, terminating HRM service.");
                            stopForegroundService();
                            return;
                        }
                        HrmValue hrm = RecordingSensorStore.hrm;
                        boolean isValid = hrm.isValid();
                        if (!isValid && currentTime - hrm.getTimestamp() > (numHrRestarts + 1) * 90_000) {
                            sensors.stopHrSensor(TrackRecordingService.this);
                            new Handler(getMainLooper()).postDelayed(() -> {
                                sensors.startHrSensor(TrackRecordingService.this, true);
                                numHrRestarts++;
                                Logger.INSTANCE.logW(TAG, "HR sensor restarted. Attempt: " + numHrRestarts);
                            }, 500);
                        }

                        RecordingSensorStore.hrmDebug.setSendTimestamp(currentTime);
                        WearCommService.getInstance().sendDataItem(PUT_HEART_RATE,
                                new CommandFloatExtra(hrm.isValid() ? hrm.getValue() : Float.NaN));

//                        Logger.logD(TAG, "Sending HRM, value " + hrm.getValue() + " timestamp " + new SimpleDateFormat("hh:mm:ss").format(hrm.getTimestamp()));
                        handler.postDelayed(this, 2000);
                    }
                };
                handler.postDelayed(sendHrmUpdate, 2000);
            } else {
                stopForegroundService();
            }
        }
    }

    private void stopForegroundService() {
        Logger.INSTANCE.logD(TAG, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
        onDestroy();
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (mSensors != null) {
            mSensors.destroy(this);
            mSensors = null;
        }
        if (wakeLockManager != null) {
            wakeLockManager.releaseWakeLock();
            wakeLockManager = null;
        }

        super.onDestroy();
    }
}
