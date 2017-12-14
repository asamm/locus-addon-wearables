package com.asamm.locus.addon.wear;

import android.content.Context;

import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.LocusWearCommService;
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.MapPeriodicParams;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.TrackStats;
import locus.api.utils.Logger;

/**
 * Singleton class for handling communication between this application and the watch.
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class DeviceCommService extends LocusWearCommService {

	private static volatile DeviceCommService mInstance;

	private static final Long REENABLE_PERIODIC_RECEIVER_TIMEOUT_MS = 5000L;

	// tag for logger
	private static final String TAG = DeviceCommService.class.getSimpleName();

	private volatile boolean mAboutToBeDestroyed = false;
	// Last received update from Locus
	private volatile UpdateContainer mLastUpdate;

	private volatile long mLastPeriodicUpdateReceivedMilis = 0L;

	// time for periodic data transmission to wear device
	private PeriodicDataTimer mPeriodicDataTimer;

	private TrackProfileIconValue.ValueList mProfileIcons;

	private static final Location ZERO_LOCATION = new Location(0,0);
	/**
	 * is updated as side effect of some selected wear requests during handling
	 */
	private volatile LocusUtils.LocusVersion lv;

	/**
	 * Default constructor.
	 *
	 * @param ctx current context
	 */
	private DeviceCommService(Context ctx) {
		super(ctx);
		Logger.logD(TAG, "Device comm service started.");
		try {
			lv = LocusUtils.getActiveVersion(ctx);
			onUpdate(ActionTools.getDataUpdateContainer(ctx, lv));
		} catch (RequiredVersionMissingException e) {
			mLastUpdate = null;
		}
	}

	/**
	 * Get instance of request handler.
	 *
	 * @param ctx current context
	 * @return instance of handler
	 */
	static DeviceCommService getInstance(Context ctx) {
		if (mInstance == null) {
			synchronized (TAG) {
				if (mInstance == null) {
					// enable receiver
					PeriodicUpdatesReceiver.enableReceiver(ctx);
					mInstance = new DeviceCommService(ctx);
				}
			}
		}
		return mInstance;
	}

	/**
	 * Destroy instance of receiver.
	 *
	 * @param ctx current context
	 */
	static void destroyInstance(Context ctx) {
		synchronized (TAG) {
			final DeviceCommService s = mInstance;
			if (s != null) {
				s.mAboutToBeDestroyed = true;
				s.destroy();
				// disable watch periodic data timer
				s.destroyPeriodicDataTimer();
				// disable receiver
				Logger.logD(TAG, "Destroying device comm instance");
				PeriodicUpdatesReceiver.disableReceiver(ctx);
				mInstance = null;
			}
		}
	}

	private void enablePeriodicReceiver(Context ctx) {
		synchronized (TAG) {
			if (!mAboutToBeDestroyed) PeriodicUpdatesReceiver.enableReceiver(ctx);
		}
	}

	/**
	 * Update content with fresh updates.
	 *
	 * @param update update container
	 */
	void onUpdate(UpdateContainer update) {
		Logger.logD(TAG, "onUpdate(" + update + ")");
		mLastUpdate = update;
		mLastPeriodicUpdateReceivedMilis = System.currentTimeMillis();
	}

	/**
	 * Notify about incorrect data.
	 */
	void onIncorrectData() {
		Logger.logD(TAG, "onIncorrectData()");
		mLastUpdate = null;
	}

	void onDataChanged(Context c, DataEvent newData) {
		Logger.logD(TAG, "received " + newData.getDataItem().getUri().getPath());
		Long currentTime = System.currentTimeMillis();
		if (currentTime - mLastPeriodicUpdateReceivedMilis > REENABLE_PERIODIC_RECEIVER_TIMEOUT_MS) {
			Logger.logE(TAG, "Periodic receiver seems offline, trying to restart.");
			try {
				// update mLastUpdate manually and refresh mLastPeriodicUpdateReceivedMilis
				onUpdate(ActionTools.getDataUpdateContainer(c, lv));
				enablePeriodicReceiver(c);
			} catch (RequiredVersionMissingException e) {
				Logger.logW(TAG, "ActionTools.getDataUpdateContainer RequiredVersionMissingException");
			}
		}
		DataItem item = newData.getDataItem();
		DataPath path = DataPath.valueOf(item);
		switch (path) {
			case GET_HAND_SHAKE:
				HandShakeValue hndshk = loadHandShake(c);
				sendDataItem(DataPath.PUT_HAND_SHAKE, hndshk);
				break;
			case GET_TRACK_REC_PROFILES:
				Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> profiles =
						loadTrackRecordProfiles(c);
				if (profiles.first != null) {
					sendDataItem(DataPath.PUT_TRACK_REC_PROFILE_INFO, profiles.first);
					mProfileIcons = profiles.second;
				}
				break;
			case GET_PROFILE_ICON:
				if (mProfileIcons == null) {
					Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> profilesIcons =
							loadTrackRecordProfiles(c);
					mProfileIcons = profilesIcons.second;
				}
				ProfileIconGetCommand pigc = createStorableForPath(path, item);
				if (mProfileIcons != null) {
					for (TrackProfileIconValue icon : mProfileIcons.getStorables()) {
						if (pigc.getProfileId() == icon.getId()) {
							sendDataItem(DataPath.PUT_PROFILE_ICON, icon);
							break;
						}
					}
				}
				break;

			case PUT_TRACK_REC_STATE_CHANGE: {
				lv = LocusUtils.getActiveVersion(c);
				TrackRecordingStateChangeValue v = createStorableForPath(path, item);
				handleRecordingStateChanged(c, lv, v.getRecordingState(), v.getmProfileName());
			}
			break;
			case GET_ADD_WAYPOINT: {
				lv = LocusUtils.getActiveVersion(c);
				handleAddWpt(c, lv);
				sendCommand(DataPath.PUT_ADD_WAYPOINT);
			}
			case GET_PERIODIC_DATA: {
				lv = LocusUtils.getActiveVersion(c);
				PeriodicCommand v = createStorableForPath(path, item);
				handlePeriodicWearUpdate(c, v);
			}
			break;
			default:
				// ignore
				break;
		}
	}

	private void destroyPeriodicDataTimer() {
		synchronized (TAG) {
			if (mPeriodicDataTimer != null) {
				mPeriodicDataTimer.cancel();
				mPeriodicDataTimer = null;
			}
		}
	}
	private void handlePeriodicWearUpdate(final Context ctx, PeriodicCommand command) {
		if (command == null) {
			command = PeriodicCommand.createStopPeriodicUpdatesCommand();
		}
		final byte activityId = command.getmPeriodicActivityId();
		final int periodMs = command.getmPeriodMs();
		final TimeStampStorable extra = command.getExtra();

		final TimerTask task;
		switch (activityId)

		{
			case PeriodicCommand.IDX_PERIODIC_TRACK_RECORDING:
				task = new TimerTask() {
					@Override
					public void run() {
						if (DeviceCommService.this.mAboutToBeDestroyed) {
							destroyPeriodicDataTimer();
							return;
						}
						TrackRecordingValue trv = loadTrackRecordingValue(ctx);
						sendDataItem(DataPath.PUT_TRACK_REC, trv);
					}
				};
				break;
			case PeriodicCommand.IDX_PERIODIC_MAP:
				task = new TimerTask() {
					@Override
					public void run() {
						if (DeviceCommService.this.mAboutToBeDestroyed) {
							destroyPeriodicDataTimer();
							return;
						}
						sendMapPeriodic(ctx, ((MapPeriodicParams) extra));
					}
				};
				break;
			default:
				task = null;
				break;
		}

		synchronized (TAG) {
			destroyPeriodicDataTimer();

			if ((command == null || command.isStopRequest()) || task == null
					|| mAboutToBeDestroyed) {
				return;
			}
			mPeriodicDataTimer = new PeriodicDataTimer(activityId, periodMs);
			mPeriodicDataTimer.schedule(task, 0, periodMs);
		}
	}

	private void sendMapPeriodic(Context ctx, MapPeriodicParams extra) {
		int zoom = extra.getZoom();
		int width = extra.getWidth();
		int height = extra.getHeight();
		final UpdateContainer data = mLastUpdate;

		if (zoom == Const.ZOOM_UNKOWN) {
			zoom = data != null ? data.getMapZoomLevel() : Const.ZOOM_DEFAULT;
		}

		// request map
		ActionTools.BitmapLoadResult loadedMap = null;

		try {
			Location loc = data != null
					&& data.isEnabledMyLocation()
					&& data.getLocMyLocation() != null ?
					data.getLocMyLocation() : ZERO_LOCATION;
			loadedMap = ActionTools.getMapPreview(ctx,
					lv, loc, zoom, width, height, true);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "loadMapPreview(" + lv + ")");
		}

		LocusInfo locusInfo = null;
		try {
			locusInfo = ActionTools.getLocusInfo(ctx, lv);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "Missing required version, current version " + lv, e);
		}

		MapContainer m = new MapContainer(loadedMap, data, locusInfo, zoom);
		sendDataItem(DataPath.PUT_MAP, m);
	}

//	private void compressionTest(ActionTools.BitmapLoadResult loadedMap) {
//		// TODO cejnar debug only, delete this method
//		Bitmap b = loadedMap.getImage();
//		ByteArrayOutputStream baosPng = new ByteArrayOutputStream();
//		b.compress(Bitmap.CompressFormat.PNG, 0, baosPng);
//		ByteArrayOutputStream baosJpeg = new ByteArrayOutputStream();
//		b.compress(Bitmap.CompressFormat.JPEG, 80, baosJpeg);
//		ByteArrayOutputStream baosWebp = new ByteArrayOutputStream();
//		b.compress(Bitmap.CompressFormat.WEBP, 90, baosWebp);
//		Logger.logD(TAG, "Original: " + loadedMap.getImage().getByteCount());
//		Logger.logD(TAG, "PNG: " + baosPng.toByteArray().length);
//		Logger.logD(TAG, "JPEG: " + baosJpeg.toByteArray().length);
//		Logger.logD(TAG, "WEBP: " + baosWebp.toByteArray().length);
//		Logger.logD(TAG, "finished");
//	}

	public static boolean isInstance() {
		return mInstance != null;
	}

	///////////////////////////////////////////////////////////////////////////
	//      Value object create methods - reading from Locus API             //
	///////////////////////////////////////////////////////////////////////////

	private void handleAddWpt(Context ctx, LocusUtils.LocusVersion lv) {
		try {
			ActionTools.actionTrackRecordAddWpt(ctx, lv, true);
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "Invalid version " + lv + ", can't add WPT", e);
			throw new IllegalStateException(e);
		}
	}

	private void handleRecordingStateChanged(Context ctx, LocusUtils.LocusVersion lv, TrackRecordingStateEnum newState, String profile) {
		TrackRecordingStateEnum currentRecState = null;
		if (mLastUpdate != null) {
			currentRecState = mLastUpdate.isTrackRecPaused() ?
					TrackRecordingStateEnum.PAUSED :
					mLastUpdate.isTrackRecRecording() ? TrackRecordingStateEnum.RECORDING :
							TrackRecordingStateEnum.NOT_RECORDING;
		}
		if (newState != null && currentRecState != newState) {
			try {
				switch (newState) {
					case PAUSED:
						ActionTools.actionTrackRecordPause(ctx, lv);
						break;
					case RECORDING:
						ActionTools.actionTrackRecordStart(ctx, lv, profile);
						break;
					case NOT_RECORDING:
						ActionTools.actionTrackRecordStop(ctx, lv, true);
						break;
				}
			} catch (RequiredVersionMissingException e) {
				Logger.logE(TAG, "Invalid version " + lv + ", cant change track recording state.", e);
			}
		}
		try {
			onUpdate(ActionTools.getDataUpdateContainer(ctx, lv));
		} catch (RequiredVersionMissingException e) {
			Logger.logW(TAG, "getDataUpdateContainer() - RequiredVersionMissingException");
		}
		TrackRecordingValue trv = loadTrackRecordingValue(ctx);
		sendDataItem(DataPath.PUT_TRACK_REC, trv);
	}

	/**
	 * Load basic data from current Locus application.
	 */
	private HandShakeValue loadHandShake(Context ctx) {
		LocusInfo locusInfo = null;

		try {
			// check if object exists
			if (lv != null) {
				// handle info
				locusInfo = ActionTools.getLocusInfo(ctx, lv);
			}
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "loadHandShake", e);
			// clear data
			locusInfo = null;
		}

		// prepare container with data and send it
		HandShakeValue value = lv == null ?
				new HandShakeValue() :
				new HandShakeValue(lv.getVersionCode(),
						locusInfo != null && locusInfo.isRunning(),
						locusInfo != null && locusInfo.isPeriodicUpdatesEnabled());
		return value;
	}

	/**
	 * Load recording profiles data from current Locus application.
	 * @param nodeId ID of requester
	 */
	/**
	 * @param ctx
	 * @return A pair of lists. First lists contains track profiles and description.
	 * Second list contains track profile icons. Both lists are the same lenght and order.
	 */
	private Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList>
	loadTrackRecordProfiles(Context ctx) {
		List<ActionTools.TrackRecordProfileSimple> trackRecProfiles = null;

		try {
			// read Locus info
			LocusUtils.LocusVersion lv = LocusUtils.getActiveVersion(
					ctx, DeviceCommService.this.lv.getVersionCode());

			// check if object exists
			if (lv != null) {
				// load also track record profiles
				trackRecProfiles = ActionTools.getTrackRecordingProfiles(
						ctx, lv);
			}
		} catch (RequiredVersionMissingException e) {
			Logger.logE(TAG, "loadTrackRecordProfiles()", e);

			// clear data
			trackRecProfiles = null;
		}

		Pair<TrackProfileInfoValue.ValueList, TrackProfileIconValue.ValueList> result =
				new Pair<>(new TrackProfileInfoValue.ValueList(), new TrackProfileIconValue.ValueList());

		if (trackRecProfiles != null && !trackRecProfiles.isEmpty()) {
			ArrayList<TrackProfileInfoValue> profiles = new ArrayList<>(trackRecProfiles.size());
			ArrayList<TrackProfileIconValue> icons = new ArrayList<>(trackRecProfiles.size());
			for (ActionTools.TrackRecordProfileSimple profile : trackRecProfiles) {
				profiles.add(new TrackProfileInfoValue(profile));
				icons.add(new TrackProfileIconValue(profile));
			}
			result.first.setStorables(profiles);
			result.second.setStorables(icons);
		}
		return result;
	}

	private TrackRecordingValue loadTrackRecordingValue(Context ctx) {
		boolean infoAvailable = mLastUpdate != null;
		boolean myLocAvailable = infoAvailable && mLastUpdate.getLocMyLocation() != null;
		boolean trackRec = infoAvailable && mLastUpdate.isTrackRecRecording();
		boolean trackRecPause = infoAvailable && mLastUpdate.isTrackRecPaused();
		String profileName = infoAvailable ? mLastUpdate.getTrackRecProfileName() : "";
		Float speed = myLocAvailable ? mLastUpdate.getLocMyLocation().getSpeed() : null;

		TrackStats stats = infoAvailable ? mLastUpdate.getTrackRecStats() : null;

		LocusInfo locusInfo = null;
		try {
			locusInfo = ActionTools.getLocusInfo(ctx, lv);
		} catch (RequiredVersionMissingException e) {
			Logger.logW(TAG, "Missing required version, current version " + lv);
		}

		TrackRecordingValue trv = new TrackRecordingValue(infoAvailable, trackRec, trackRecPause,
				profileName, stats, locusInfo, new TrackRecordingValue.ExtendedTrackInfo(speed));
		return trv;
	}

	private static class PeriodicDataTimer extends Timer {
		private byte activityId;
		private int periodMs;

		private PeriodicDataTimer(byte activityId, int periodMs) {
			super();
			this.activityId = activityId;
			this.periodMs = periodMs;
		}
	}

}
