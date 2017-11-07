package com.asamm.locus.addon.wearables;

import com.assam.locus.addon.wearables.common.Const;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class DeviceListenerService extends WearableListenerService  {

	// timer for termination
	private static Timer mTimerTerminate;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
		Logger.logD("DeviceListenerService", "onMessageReceived(" + messageEvent + "), " +
				"path:" + path);

		// cancel timer if any message has arrived
		if (mTimerTerminate != null) {
			mTimerTerminate.cancel();
			mTimerTerminate = null;
		}

		// handle path parameter
        if (path.equals(Const.PATH_STATE_APP_DESTROYED)) {
            RequestHandler.destroyInstance(this);
        } else {
            RequestHandler.getInstance(this).
                    onMessageReceived(this, messageEvent);

			// start "destroyer"
			mTimerTerminate = new Timer();
			mTimerTerminate.schedule(new TimerTask() {

				@Override
				public void run() {
					RequestHandler.destroyInstance(DeviceListenerService.this);
					mTimerTerminate = null;
				}
			}, TimeUnit.SECONDS.toMillis(15));
        }
    }
}
