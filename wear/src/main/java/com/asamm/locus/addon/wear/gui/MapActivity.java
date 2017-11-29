package com.asamm.locus.addon.wear.gui;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wear.AppPreferencesManager;
import com.asamm.locus.addon.wear.ApplicationMemoryCache;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.MapPeriodicParams;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.custom.NavHelper;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.enums.PointRteAction;
import locus.api.utils.Logger;

public class MapActivity extends LocusWearActivity {

	private static final int MAP_REFRESH_PERIOD_MS = 5000;
	private static final int SCALE_ANIMATION_DURATION_MS = 200;
	// reference to map view
	private ImageView mMapView;

	// main container
	private LinearLayout mLlNavPanel;
	// top navigation command (next)
	private ImageView mIvNavPanelTop;
	// main navigation command (current)
	private ImageView mIvNavPanelMiddle;
	// distance to next command (value)
	private TextView mTvNavPanelDistValue;
	// distance to next command (units)
	private TextView mTvNavPanelDistUnits;

	private volatile MapContainer mLastContainer;

	/**
	 * simple mutex for temporary locking zooming function while animating
	 */
	private volatile boolean mZoomLock;

	private volatile int mDeviceZoom;
	private volatile int mRequestedZoom = Const.ZOOM_UNKONWN;

	@Override
	protected DataPayload<PeriodicCommand> getInitialCommandType() {
		ApplicationMemoryCache appState = ((MainApplication) getApplication()).getCache();

		final MapPeriodicParams params =
				new MapPeriodicParams(0d, 0d, mRequestedZoom,
						appState.getScreenWidth(),
						appState.getScreenHeight());
		return new DataPayload<>(DataPath.GET_PERIODIC_DATA,
				new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_MAP,
						MAP_REFRESH_PERIOD_MS, params));
	}

	@Override
	protected DataPath getInitialCommandResponseType() {
		return DataPath.PUT_MAP;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = findViewById(R.id.image_view_map);
		mLlNavPanel = findViewById(R.id.linear_layout_panel_navigation);
		mIvNavPanelTop = findViewById(R.id.image_view_next);
		mIvNavPanelMiddle = findViewById(R.id.image_view_main);
		mTvNavPanelDistValue = findViewById(R.id.text_view_dist_value);
		mTvNavPanelDistUnits = findViewById(R.id.text_view_dist_units);

		// Enables Always-on
		setAmbientEnabled();
		initView();
	}

	@Override
	protected void onStart() {
		Pair<Integer, Integer> zooms = AppPreferencesManager.getZoomValues(this);
		mDeviceZoom = zooms.first;
		mRequestedZoom = zooms.second;
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		AppPreferencesManager.persistZoomValues(this, mDeviceZoom, mRequestedZoom);
	}

	/**
	 * Initialize view before first data arrives
	 */
	private void initView() {
		ApplicationMemoryCache cache = ((MainApplication) getApplication()).getCache();
		MapContainer savedContainer = cache.getLastMapData();
		if (savedContainer != null && savedContainer.isMapPresent()) {
			refreshMapView(savedContainer);
		} else {
			mMapView.setBackground(getDrawable(R.drawable.var_map_loading_tile_256));
		}
		refreshPanelNavigation(null); // hide navigation panel
	}

	public void refreshLayout(final MapContainer data) {
		// run in UI thread
		runOnUiThread(() -> {
			refreshMapView(data);
			refreshZoomModel(data);
			refreshPanelNavigation(data);
		});
	}
	private void refreshZoomModel(MapContainer data) {
		// zoom on device changed right now or from last time app was opened - reset zoom to device zoom value
		if (mDeviceZoom != data.getZoomDevice() && data.getZoomDevice() != Const.ZOOM_UNKONWN) {
			changeZoom(data.getZoomDevice());
		}
		Logger.logD("","ZOOM WEAR: "+data.getZoomWear());
		mDeviceZoom = data.getZoomDevice();
	}

	/**
	 * Refreshes map image view
	 */
	private void refreshMapView(MapContainer data) {
		if (data != null && data.getLoadedMap() != null) {
			mMapView.setBackground(null);
			mMapView.setImageBitmap(data.getLoadedMap().getImage());
			if (data.getZoomWear() == mRequestedZoom) {
				mMapView.animate().cancel();
				mMapView.setScaleX(1f);
				mMapView.setScaleY(1f);
				mZoomLock = false;
			}
		}
	}

	/**
	 * Refresh panel with navigation.
	 */
	private void refreshPanelNavigation(MapContainer data) {
		if (data == null || data.getmGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION) {
			mLlNavPanel.setVisibility(View.GONE);
			return;
		}
		mLlNavPanel.setVisibility(View.VISIBLE);

		// action for current point
		setNavImageForAction(mIvNavPanelMiddle, data.getNavPointAction1Id());

		// action for next point
		setNavImageForAction(mIvNavPanelTop, data.getNavPointAction2Id());

		// set time to target
		mTvNavPanelDistValue.setText(UtilsFormat.formatDistance(
				data.getUnitsFormatLength(), data.getNavPoint1Dist(), true));
		mTvNavPanelDistUnits.setText(UtilsFormat.formatDistanceUnits(
				data.getUnitsFormatLength(), data.getNavPoint1Dist()));
	}

	private void setNavImageForAction(ImageView view, int pointRteActionId) {
		PointRteAction action = PointRteAction.getActionById(pointRteActionId);
		int img = NavHelper.getNavPointImageRes(action);
		if (!Integer.valueOf(img).equals(view.getTag())) {
			view.setImageResource(img);
			view.setTag(img);
		}
	}

	@Override
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		super.consumeNewData(path, data);
		switch (path) {
			case PUT_MAP:
				mLastContainer = (MapContainer) data;
				refreshLayout(mLastContainer);
				break;
		}
	}

	public void onZoomClicked(View v) {
		if (mDeviceZoom == Const.ZOOM_UNKONWN || mZoomLock) {
			return;
		}
		if (mRequestedZoom == Const.ZOOM_UNKONWN) {
			mRequestedZoom = mDeviceZoom;
		}
		final int zoomDiff;
		if (v.getId() == R.id.btn_zoom_in) {
			zoomDiff = 1;
		} else if (v.getId() == R.id.btn_zoom_out) {
			zoomDiff = -1;
		} else {
			return;
		}
		mZoomLock = true;
		if (changeZoom(mRequestedZoom + zoomDiff)) {
			float scale = zoomDiff < 0 ? 0.5f : 2f;
			mMapView.animate()
					.scaleX(scale)
					.scaleY(scale)
					.setDuration(SCALE_ANIMATION_DURATION_MS)
					.setInterpolator(new DecelerateInterpolator())
					.withEndAction(() -> mZoomLock = false)
					.start();
		} else {
			mZoomLock = false;
		}
	}

	private boolean changeZoom(int newZoom) {
		newZoom = Math.min(Math.max(newZoom, Const.ZOOM_MIN), Const.ZOOM_MAX);
		if (newZoom == mRequestedZoom) {
			return false;
		}
		mRequestedZoom = newZoom;
		DataPayload<PeriodicCommand> refreshCmd = getInitialCommandType();
		WearCommService.getInstance().sendDataItem(refreshCmd.getPath(), refreshCmd.getStorable());
		return true;
	}

	@Override
	public boolean isUsePeriodicData() {
		return true;
	}
}
