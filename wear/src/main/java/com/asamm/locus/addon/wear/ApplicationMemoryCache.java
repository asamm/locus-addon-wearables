package com.asamm.locus.addon.wear;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

import java.util.Collections;
import java.util.List;

/**
 * Container class serves as a memory cache to keep some received data while the app is running.
 *
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public class ApplicationMemoryCache {
	private Point mScreenDimension = new Point();
	private MapContainer mMapContainer;
	private List<TrackProfileInfoValue> mProfiles;


	ApplicationMemoryCache(Context c) {
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getSize(mScreenDimension);
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

	public void setLastMapData(MapContainer mapContainer) {
		if (mapContainer != null) {
			mMapContainer = mapContainer;
		}
	}

	public MapContainer getLastMapData() {
		return mMapContainer;
	}

	public List<TrackProfileInfoValue> getProfiles() {
		return mProfiles == null ? Collections.emptyList() : mProfiles;
	}

	public void setProfiles(List<TrackProfileInfoValue> profiles) {
		this.mProfiles = profiles;
	}
}
