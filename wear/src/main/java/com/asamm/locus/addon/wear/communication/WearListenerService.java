package com.asamm.locus.addon.wear.communication;

import android.app.Application;
import android.content.Intent;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.application.TrackRecordingService;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayloadStorable;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import locus.api.utils.Logger;

/**
 * Service listening to Data API data changes
 * <p>
 * Created by menion on 08/08/15.
 * Asamm Software, s. r. o.
 */
public class WearListenerService extends WearableListenerService {

	// tag for logger
	private static final String TAG = WearListenerService.class.getSimpleName();

	@Override
	public void onDataChanged(DataEventBuffer dataEventBuffer) {

		for (DataEvent event : dataEventBuffer) {
			if (event.getType() == DataEvent.TYPE_CHANGED) {
				Application a = getApplication();
				if (a == null) {
					Logger.logW(TAG, "Received data but application is null");
					DataPath p = DataPath.valueOf(event.getDataItem());
					if (p == DataPath.STOP_WATCH_TRACK_REC_SERVICE) {
						Intent intent = new Intent(this, TrackRecordingService.class);
						intent.setAction(TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE);
						startService(intent);
					}
				} else {
					((MainApplication) getApplication()).handleDataEvent(event.getDataItem());
				}
			} else if (event.getType() == DataEvent.TYPE_DELETED) {
				// DataItem deleted
			}
		}
	}
	@Override
	public void onMessageReceived(MessageEvent messageEvent) {

		DataPath p = DataPath.fromPath(messageEvent.getPath());
		Logger.logD(TAG, "Received message " + p);
		if (p == null)
			return;
		try {
			((MainApplication) getApplication()).handleDataChannelEvent(
					new DataPayloadStorable(p, p.getContainerClass().getConstructor(byte[].class).newInstance(messageEvent.getData())));
		} catch (Exception e) {
			e.printStackTrace();

		}

	}
}
