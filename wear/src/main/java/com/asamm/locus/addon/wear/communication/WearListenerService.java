package com.asamm.locus.addon.wear.communication;

import com.asamm.locus.addon.wear.MainApplication;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;

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
				((MainApplication) getApplication()).handleDataEvent(event.getDataItem());
			} else if (event.getType() == DataEvent.TYPE_DELETED) {
				// DataItem deleted
			}
		}
	}

	@Override
	public void onChannelOpened(Channel channel) {
		super.onChannelOpened(channel);
	}

}
