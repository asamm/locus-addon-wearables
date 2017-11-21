package com.asamm.locus.addon.wear.gui;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wear.ApplicationCache;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.MapPeriodicParams;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.StringCommand;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.gui.custom.NavHelper;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.enums.PointRteAction;

public class MapActivity extends LocusWearActivity {

	private static final int MAP_REFRESH_PERIOD_MS = 5000;
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

	private MapContainer mLastContainer;

	private int mRequestedZoom = Const.ZOOM_UNKONWN;

	@Override
	protected DataPayload<PeriodicCommand> getInitialCommandType() {
		ApplicationCache appState = ((MainApplication) getApplication()).getCache();

		final MapPeriodicParams params =
				new MapPeriodicParams(0d, 0d, mRequestedZoom,
						appState.getScreenWidth(),
						appState.getScreenHeight());
		return new DataPayload<>(DataPath.GET_PERIODIC_DATA,
				new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_ACITIVITY_MAP,
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

	/**
	 * Initialize view before first data arrives
	 */
	private void initView() {
		ApplicationCache cache = ((MainApplication) getApplication()).getCache();
		MapContainer savedContainer = cache.getLastMapData();
		if (savedContainer != null && savedContainer.isMapPresent()) {
			refreshMapView(savedContainer);
		} else {
			mMapView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.map_placeholder));
		}
		refreshPanelNavigation(null); // hide navigation panel
	}

	public void refreshLayout(final MapContainer data) {
		// run in UI thread
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// refresh layout
				refreshMapView(data);
				refreshPanelNavigation(data);
			}
		});
	}

	/**
	 * Refreshes map image view
	 */
	private void refreshMapView(MapContainer data) {
		if (data != null && data.getLoadedMap() != null) {
			mMapView.setImageBitmap(data.getLoadedMap().getImage());
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

	private int getCurrentZoom() {
		if (mLastContainer == null) {
			return Const.ZOOM_UNKONWN;
		}
		return mLastContainer.getZoom();
	}

	public void onZoomClicked(View v) {
		// don't know current zoom or there is valid requested zoom which is already different from
		// last known map zoom
		// TODO cejnar fix logic, both zoom persisted, wear zoom independent
		int currentZoom = getCurrentZoom();
		if (currentZoom == Const.ZOOM_UNKONWN) {
			return;
		}
		if (mRequestedZoom == Const.ZOOM_UNKONWN) {
			mRequestedZoom = currentZoom;
		}
		// TODO cejnar min/max zoom check
		if (v.getId() == R.id.btn_zoom_in) {
			mRequestedZoom++;
		} else if (v.getId() == R.id.btn_zoom_out) {
			mRequestedZoom--;
		}
		DataPayload<PeriodicCommand> refreshCmd = getInitialCommandType();
		WearCommService.getInstance().sendDataItem(refreshCmd.getPath(), refreshCmd.getStorable());
	}

	@Override
	public boolean isUsePeriodicData() {
		return true;
	}
}