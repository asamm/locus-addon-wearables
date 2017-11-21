package com.asamm.locus.addon.wear;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

/**
 * Container class serves as a memory cache to keep some received data while the app is running.
 *
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public class ApplicationState {
	private HandShakeValue mHandShakeValue = null;
	private Point mScreenDimension = new Point();
	private MapContainer mMapContainer;


	ApplicationState(Context c) {
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getSize(mScreenDimension);
	}

	public HandShakeValue getmHandShakeValue() {
		return mHandShakeValue;
	}

	void setmHandShakeValue(HandShakeValue mHandShakeValue) {
		this.mHandShakeValue = mHandShakeValue;
	}

	public boolean isHandShake() {
		return mHandShakeValue != null;
	}

	public int getScreenWidth() {
		return mScreenDimension.x;
	}

	public int getScreenHeight() {
		return mScreenDimension.y;
	}

	public void setLastTrackRecState(Context ctx, TrackRecordingValue value) {
	AppPreferencesManager.persistLastRecState(ctx, value);
	}

	public TrackRecordingStateEnum getLastTrackRecState(Context ctx) {
		return AppPreferencesManager.getLastTrackRecProfileState(ctx);
	}

	public String getLastTrackRecProfileName(Context ctx) {
		return AppPreferencesManager.getLastTrackRecProfileName(ctx);
	}

	public void setLastMapData(MapContainer mapContainer) {
		if (mapContainer != null) {
			mMapContainer = mapContainer;
		}
	}

	public MapContainer getLastMapData() {
		return mMapContainer;
	}
}
