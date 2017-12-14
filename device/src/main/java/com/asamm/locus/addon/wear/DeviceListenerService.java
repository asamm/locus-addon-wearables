package com.asamm.locus.addon.wear;

import android.content.Context;

import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import locus.api.utils.Logger;

/**
 * Listener for incoming messages/data from the watch
 * <p>
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class DeviceListenerService extends WearableListenerService {

	private static final String TAG = DeviceListenerService.class.getSimpleName();
	/**
	 * Timeout in seconds after which communication services are killed
	 * and locus periodic update disabled
	 */
	private static final int INACTVITY_TIMEOUT_SECONDS = 15;
	// timer for termination
	private static Timer mTimerTerminate;

	/**
	 * DataEvent consumer
	 */
	private final DataConsumer<DataEvent> dataEventConsumer = new DataConsumer<DataEvent>() {
		@Override
		public void consume(Context c, DeviceCommService rh, DataEvent newData) {
			rh.onDataChanged(c, newData);
		}

		@Override
		public DataPath getPath(DataEvent newData) {
			DataItem d = newData.getDataItem();
			return DataPath.valueOf(d);
		}
	};

	/**
	 * DataChanged callback
	 *
	 * @param dataEventBuffer
	 */
	@Override
	public void onDataChanged(DataEventBuffer dataEventBuffer) {
		for (DataEvent event : dataEventBuffer) {
			if (event.getType() == DataEvent.TYPE_CHANGED) {
				Logger.logD(TAG, "Received data changed: " + event.getDataItem().getUri().getPath());
				handleDataChange(dataEventConsumer, event);
			} else if (event.getType() == DataEvent.TYPE_DELETED) {
				// DataItem deleted
			}
		}
	}

	/**
	 * Helper method for data consumption using supplied consumer.
	 *
	 * @param dataConsumer
	 * @param newData
	 * @param <T>
	 */
	private <T> void handleDataChange(DataConsumer<T> dataConsumer, T newData) {
		final DataPath p = dataConsumer.getPath(newData);
		if (p == null) {
			return; // Unknown path, ignore received data
		}


		// handle termination request if present, otherwise delegate data handling to DeviceCommService
		if (p == DataPath.PUT_APP_DESTROYED) {
			cancelTerminationTimer();
			DeviceCommService.destroyInstance(this);
		} else {
			switch (p) {
				// for following paths, refresh terminationTimer
				case GET_KEEP_ALIVE:
				case GET_HAND_SHAKE:
				case GET_TRACK_REC_PROFILES:
				case GET_PERIODIC_DATA:
				case GET_PROFILE_ICON:
					cancelTerminationTimer();
					// start "destroyer"
					mTimerTerminate = new Timer();
					mTimerTerminate.schedule(new TimerTask() {

						@Override
						public void run() {
							DeviceCommService.destroyInstance(DeviceListenerService.this);
							mTimerTerminate = null;
						}
					}, TimeUnit.SECONDS.toMillis(INACTVITY_TIMEOUT_SECONDS));
					// then for all paths try to consume data content
				default:
					dataConsumer.consume(this, DeviceCommService.getInstance(this), newData);

			}
		}
	}

	private void cancelTerminationTimer() {
		if (mTimerTerminate != null) {
			mTimerTerminate.cancel();
			mTimerTerminate = null;
		}
	}

	/**
	 * Generic incomming data consumer with ability to detect termination request data before
	 * delegating consumption of the data.
	 */
	private interface DataConsumer<T> {
		/**
		 * Processes new incoming data
		 *
		 * @param c       context
		 * @param rh      comm service which will handle data consumption
		 * @param newData data to consume
		 */
		void consume(Context c, DeviceCommService rh, T newData);

		/**
		 * Checks incoming data for termination request.
		 *
		 * @param newData data to consume
		 * @return
		 */
		DataPath getPath(T newData);
	}
}
