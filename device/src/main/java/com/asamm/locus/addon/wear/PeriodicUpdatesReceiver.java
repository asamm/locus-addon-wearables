package com.asamm.locus.addon.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.PeriodicUpdatesHandler;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.utils.Logger;

/**
 * Handles logic for locus periodic updates
 * Created by menion on 17/08/15.
 * Asamm Software, s. r. o.
 */
public class PeriodicUpdatesReceiver extends BroadcastReceiver {

	// tag for logger
	private static final String TAG = "PeriodicUpdatesHandler";

	@Override
	public void onReceive(final Context context, Intent intent) {
		PeriodicUpdatesHandler.getInstance().onReceive(context, intent,
				new PeriodicUpdatesHandler.OnUpdate() {

					@Override
					public void onUpdate(LocusUtils.LocusVersion locusVersion, UpdateContainer update) {
						// check instance
						if (!DeviceCommService.isInstance()) {
							Logger.logI(TAG, "onUpdate(), " +
									"instance not exists");
							disableReceiver(context);
							return;
						}
						Logger.logI(TAG, "periodic update received");
						// handle data
						DeviceCommService.getInstance(context).onUpdate(update);
					}

					@Override
					public void onIncorrectData() {
						Logger.logI(TAG, "Periodic udpate - Incorrect data");
						// check instance
						if (!DeviceCommService.isInstance()) {
							Logger.logI(TAG, "onIncorrectData(), " +
									"instance not exists");
							disableReceiver(context);
							return;
						}
						// handle data
						DeviceCommService.getInstance(context).onIncorrectData();
					}
				});
	}

	/**
	 * Enable updates receiver.
	 *
	 * @param ctx current context
	 */
	protected static void enableReceiver(Context ctx) {
		try {
			ActionTools.enablePeriodicUpdatesReceiver(ctx,
					LocusUtils.getActiveVersion(ctx),
					PeriodicUpdatesReceiver.class);
			Logger.logD(TAG, "periodic receiver enabled");
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "enableReceiver(" + ctx + ")", e);
		}
	}

	/**
	 * Disable updates receiver.
	 *
	 * @param ctx current context
	 */
	protected static void disableReceiver(Context ctx) {
		try {
			ActionTools.disablePeriodicUpdatesReceiver(ctx,
					LocusUtils.getActiveVersion(ctx),
					PeriodicUpdatesReceiver.class);
			Logger.logD(TAG, "periodic receiver disabled");
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "disableReceiver(" + ctx + ")", e);
		}
	}
}
