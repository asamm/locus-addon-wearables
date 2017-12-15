package com.asamm.locus.addon.wear.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * Activity with map preview
 * <p>
 * Created by Milan Cejnar
 * Asamm Software, s.r.o.
 */
public class MapActivity extends LocusWearActivity {

	private static final String TAG = "MapActivity";

	private static final int MAP_REFRESH_PERIOD_MS = 5000;
	private static final int WATCHDOG_TIMEOUT_MS = MAP_REFRESH_PERIOD_MS * 3;
	private static final int SCALE_ANIMATION_DURATION_MS = 200;

	private static final boolean INVERT_MAP_IN_AMBIENT = false;
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

	private ImageView mBtnZoomIn;
	private ImageView mBtnZoomOut;
	private ImageView mIvAmbient;
	private ViewGroup mLayoutNavigation;

	private volatile MapContainer mLastContainer;

	// ambient mode variables
	private ColorMatrix mInvertColorMatrix;
	private Bitmap mMapAmbientBitmap;
	private TextView mTvNavDistVal;
	private TextView mTvNavDistUnits;


	/**
	 * simple mutex for temporary locking zooming function while animating
	 */
	private volatile boolean mZoomLock;
	private volatile boolean mIsScaled = false;

	private volatile int mDeviceZoom;
	private volatile int mRequestedZoom = Const.ZOOM_UNKOWN;

	@Override
	protected DataPayload<PeriodicCommand> getInitialCommandType() {
		ApplicationMemoryCache appState = ((MainApplication) getApplication()).getCache();

		final MapPeriodicParams params =
				new MapPeriodicParams(mRequestedZoom,
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
		mBtnZoomIn = findViewById(R.id.btn_zoom_in);
		mBtnZoomOut = findViewById(R.id.btn_zoom_out);
		mLayoutNavigation = findViewById(R.id.linear_layout_panel_navigation);
		mTvNavDistUnits = findViewById(R.id.text_view_dist_units);
		mTvNavDistVal = findViewById(R.id.text_view_dist_value);
		mIvAmbient = findViewById(R.id.imageview_ambient);
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
		if (mDeviceZoom != data.getZoomDevice() && data.getZoomDevice() != Const.ZOOM_UNKOWN) {
			changeZoom(data.getZoomDevice());
		}
		mDeviceZoom = data.getZoomDevice();
	}

	/**
	 * Refreshes map image view
	 */
	private void refreshMapView(MapContainer data) {
		if (data != null && data.getLoadedMap() != null && data.getLoadedMap().getImage() != null) {
			Bitmap map = data.getLoadedMap().getImage();
			if (INVERT_MAP_IN_AMBIENT && isAmbient()) {
				Bitmap bm = getMapAmbientBitmap(map.getWidth(), map.getHeight());
				Canvas c = new Canvas(bm);
				Paint paintInvertImage = new Paint();
				paintInvertImage.setColorFilter(new ColorMatrixColorFilter(getInvertColorMatrix()));
				c.drawBitmap(map, 0, 0, paintInvertImage);
				mMapView.setImageBitmap(bm);
			} else {
				mMapView.setImageBitmap(map);
			}
			if (data.getZoomWear() == mRequestedZoom && mIsScaled) {
				mMapView.animate().cancel();
				mMapView.setScaleX(1f);
				mMapView.setScaleY(1f);
				mZoomLock = false;
				mIsScaled = false;
			}
		} else {
			Logger.logE(TAG, (data == null ? "data" : data.getLoadedMap() == null ?
					 "data.loadedMap" : "data.loadedMap.image") + " is null.");
		}
	}

	private Bitmap getMapAmbientBitmap(int width, int height) {
		if (mMapAmbientBitmap == null ||
				mMapAmbientBitmap.getWidth() != width ||
				mMapAmbientBitmap.getHeight() != height) {
			mMapAmbientBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		}
		return mMapAmbientBitmap;
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
		setNavImageForAction(mIvNavPanelMiddle,
				data.isNavValid() ? data.getNavPointAction1Id() : PointRteAction.UNDEFINED.getId());

		if (data.isNavValid()) {
			if (mIvNavPanelTop.getVisibility() != View.VISIBLE) {
				mIvNavPanelTop.setVisibility(View.VISIBLE);
			}
			// action for next point
			setNavImageForAction(mIvNavPanelTop, data.getNavPointAction2Id());
			mTvNavPanelDistValue.setText(UtilsFormat.formatDistance(
					data.getUnitsFormatLength(), data.getNavPoint1Dist(), true));
			mTvNavPanelDistUnits.setText(UtilsFormat.formatDistanceUnits(
					data.getUnitsFormatLength(), data.getNavPoint1Dist()));
		} else {
			mIvNavPanelTop.setVisibility(View.INVISIBLE);
			mTvNavPanelDistValue.setText("");
			mTvNavPanelDistUnits.setText("");
		}
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
				if (mLastContainer.getLoadedMap() == null || mLastContainer.getLoadedMap().getImage() == null
						|| mLastContainer.getLoadedMap().getNumOfNotYetLoadedTiles() > 0) {
					getMainApplication().sendDataWithWatchDog(getInitialCommandType(), getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS);
				} else {
					getMainApplication().addWatchDog(getInitialCommandType(), getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS);
				}
				break;
		}
	}

	public void onZoomClicked(View v) {
		if (mDeviceZoom == Const.ZOOM_UNKOWN || mZoomLock) {
			return;
		}
		if (mRequestedZoom == Const.ZOOM_UNKOWN) {
			mRequestedZoom = mDeviceZoom;
		}
		final int zoomDiff;
		int viewId = v.getId();
		if (viewId == R.id.btn_zoom_in || viewId == R.id.area_zoom_in) {
			zoomDiff = 1;
		} else if (viewId == R.id.btn_zoom_out || viewId == R.id.area_zoom_out) {
			zoomDiff = -1;
		} else {
			return;
		}
		mZoomLock = true;
		if (changeZoom(mRequestedZoom + zoomDiff)) {
			float scale = zoomDiff < 0 ? 0.5f : 2f;
			mIsScaled = true;
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

	public ColorMatrix getInvertColorMatrix() {
		if (mInvertColorMatrix == null) {
			float mx[] = {
					-1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
					0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
					0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
					0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
			mInvertColorMatrix = new ColorMatrix(mx);
		}
		return mInvertColorMatrix;
	}

	@Override
	public void onEnterAmbient(Bundle ambientDetails) {
		super.onEnterAmbient(ambientDetails);
		mBtnZoomIn.setVisibility(View.GONE);
		mBtnZoomOut.setVisibility(View.GONE);
		mIvAmbient.setVisibility(View.VISIBLE);
		mLayoutNavigation.setBackgroundColor(getColor(R.color.base_dark_primary));
		mTvNavDistVal.setTextColor(Color.WHITE);
		mTvNavDistUnits.setTextColor(Color.WHITE);
		refreshMapView(mLastContainer);
	}

	@Override
	public void onExitAmbient() {
		super.onExitAmbient();
		mBtnZoomIn.setVisibility(View.VISIBLE);
		mBtnZoomOut.setVisibility(View.VISIBLE);
		mIvAmbient.setVisibility(View.GONE);
		mLayoutNavigation.setBackgroundColor(getColor(R.color.panel_map_side));
		mTvNavDistVal.setTextColor(getColor(R.color.base_dark_primary));
		mTvNavDistUnits.setTextColor(getColor(R.color.base_dark_primary));
		refreshMapView(mLastContainer);
	}
}
