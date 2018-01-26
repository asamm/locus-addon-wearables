package com.asamm.locus.addon.wear.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.asamm.locus.addon.wear.gui.custom.CustomWearableDrawerLayout;
import com.asamm.locus.addon.wear.gui.custom.NavHelper;
import com.asamm.locus.addon.wear.gui.custom.WearMapActionMoveFling;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAction;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.enums.PointRteAction;
import locus.api.objects.extra.Location;
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

	private GestureDetector mDetector;

	/**
	 * simple mutex for temporary locking zooming function while animating
	 */
	private volatile boolean mZoomLock;
	private volatile boolean mIsScaled = false;

	private volatile int mDeviceZoom;
	private volatile int mRequestedZoom = Const.ZOOM_UNKOWN;
	private volatile int mOffsetX = 0;
	private volatile int mOffsetY = 0;
	private volatile boolean mAutoRotate = false;
	private int mDensityDpi = 0;
	private int mDiagonal = 0;
	private ApplicationMemoryCache appCache;
	/**
	 * Last rendered location and offset
	 */
	private volatile Location mLastMapLocation = new Location(0, 0);
	private volatile int mLastRenderedOffsetX = 0;
	private volatile int mLastRenderedOffsetY = 0;

	// map panning handler to postpone new map request if map is currently scrolling
	private final Handler mPanHandler = new Handler();
	private final Runnable mPanRunnable = () -> {
//		DataPayload<TimeStampStorable> refreshCmd = getInitialCommandType();
//		WearCommService.getInstance().sendDataItem(refreshCmd.getPath(), refreshCmd.getStorable());
	};
	private static final int PAN_DELAY = 300;

	// fling handling
	private WearMapActionMoveFling.OffsetUpdatable flingUpdatable = (x, y, isLast) -> {
		mOffsetX += x;
		mOffsetY += y;
		refreshMapOffset(mOffsetX, mOffsetY, mLastRenderedOffsetX, mLastRenderedOffsetY);
		if (isLast) {
			cancelFling();
			mPanHandler.removeCallbacksAndMessages(null);
			mPanHandler.postDelayed(mPanRunnable, 0);
		}
	};
	private WearMapActionMoveFling mFlingAnimator = new WearMapActionMoveFling(0, 0, flingUpdatable);

	// variable used to signal !scrolling
	private boolean mScrollLock = true;

	// ********** METHODS ********** //

	@Override
	protected DataPayload<TimeStampStorable> getInitialCommandType() {
		final MapPeriodicParams params =
				new MapPeriodicParams(mRequestedZoom,
						appCache.getScreenWidth(),
						appCache.getScreenHeight(),
						mOffsetX, mOffsetY, mDensityDpi, mAutoRotate, mDiagonal,
						mLastMapLocation.latitude, mLastMapLocation.longitude);

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
		appCache = ((MainApplication) getApplication()).getCache();
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

		mDensityDpi = getResources().getDisplayMetrics().densityDpi;

		int w = appCache.getScreenHeight();
		int h = appCache.getScreenHeight();
		boolean isRound = getResources().getConfiguration().isScreenRound();
		if (isRound) {
			mDiagonal = (Math.max(w, h) + 1) / 2;
		} else {
			mDiagonal = (int) (Math.sqrt(h * h + w * w) + 1) / 2;
		}

		mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				mOffsetX += distanceX;
				mOffsetY += distanceY;
				refreshMapOffset(mOffsetX, mOffsetY, mLastRenderedOffsetX, mLastRenderedOffsetY);
				mPanHandler.removeCallbacksAndMessages(null);
				mPanHandler.postDelayed(mPanRunnable, PAN_DELAY);
				return true;
			}

			@Override
			public boolean onDown(MotionEvent event) {
				cancelFling();
				Log.d(TAG, "onDown is scrolling: " + mScrollLock);
				return true;
			}

			@Override
			public boolean onFling(MotionEvent event1, MotionEvent event2,
								   float velocityX, float velocityY) {
				cancelFling();
				mFlingAnimator = new WearMapActionMoveFling(velocityX, velocityY, flingUpdatable);
				mFlingAnimator.start(mMapView);
				return true;
			}
		});

		// Enables Always-on
		setAmbientEnabled();
		initView();
	}

	private void centerMap() {
		mPanHandler.removeCallbacksAndMessages(null);
		mOffsetX = 0;
		mOffsetY = 0;
		mPanHandler.postDelayed(mPanRunnable, 0);
	}


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		int w = appCache.getScreenHeight();
		int h = appCache.getScreenHeight();
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		if (action == MotionEvent.ACTION_UP ||
				(mDrawer != null && mDrawer.isOpened())) {
			// finish possible panning
			if (!mScrollLock) {
				mPanHandler.removeCallbacksAndMessages(null);
				mPanHandler.postDelayed(mPanRunnable, 0);
			}
			mScrollLock = true;
			Logger.logW(TAG, "dispatch touch event - KEY UP DETECTED");
		} else if (action == MotionEvent.ACTION_DOWN &&
				ev.getX() > w / 5 &&
				ev.getY() > h / 5) {
			mScrollLock = false;
			Logger.logW(TAG, "dispatch touch event - KEY DOWN DETECTED");
		}
		return mScrollLock ? super.dispatchTouchEvent(ev) : mDetector.onTouchEvent(ev);
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
			changeZoom(mRequestedZoom, data.getZoomDevice() - mRequestedZoom);
		}
		mDeviceZoom = data.getZoomDevice();
	}

	/**
	 * Refreshes map image view
	 */
	private void refreshMapView(MapContainer data) {
		if (testMapContainerAndImageNotNull(data)) {
			Bitmap map = data.getLoadedMap().getAsImage();
			mLastMapLocation = data.getLastLocation();
			mLastRenderedOffsetX = data.getOffsetX();
			mLastRenderedOffsetY = data.getOffsetY();

			if (INVERT_MAP_IN_AMBIENT && isAmbient()) {
				Bitmap bm = getMapAmbientBitmap(map.getWidth(), map.getHeight());
				Canvas c = new Canvas(bm);
				Paint paintInvertImage = new Paint();
				paintInvertImage.setColorFilter(new ColorMatrixColorFilter(getInvertColorMatrix()));
				c.drawBitmap(map, 0, 0, paintInvertImage);
				mMapView.setImageBitmap(bm);
			} else {
				Logger.logW(TAG, "updating map 1");
				refreshMapOffset(mOffsetX, mOffsetY, mLastRenderedOffsetX, mLastRenderedOffsetY);
				Logger.logW(TAG, "updating map 2");
				mMapView.setImageDrawable(new BitmapDrawable(getResources(), map));
			}
			if (data.getZoomWear() == mRequestedZoom && mIsScaled) {
// TODO cejnar debug
				//				mMapView.animate().cancel();
//				mMapView.setScaleX(1f);
//				mMapView.setScaleY(1f);
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

	private void refreshMapOffset(int offsetX, int offsetY, int renderOffsetX, int renderOffsetY) {
		runOnUiThread(() -> {
			cancelFling();
			Logger.logW(TAG, "Refreshing with new traslation X: " + (-offsetX + renderOffsetX));
			mMapView.setTranslationX(-offsetX + renderOffsetX);
			mMapView.setTranslationY(-offsetY + renderOffsetY);
		});
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
				MapContainer tmp = (MapContainer) data;
				if (tmp != null) {
					mLastContainer = tmp;
					refreshLayout(mLastContainer);
				}
				if (!testMapContainerAndImageNotNull(tmp)) {
					getMainApplication()
							.sendDataWithWatchDogConditionable(getInitialCommandType(),
									getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS,
									(MapContainer cont) -> testMapContainerAndImageNotNull(cont));
				} else if (tmp.getLoadedMap().getNumOfNotYetLoadedTiles() > 0) {
					//TODO cejnar debug
//					getMainApplication().sendDataWithWatchDog(getInitialCommandType(),
//							getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS);
				} else {
					getMainApplication().addWatchDog(getInitialCommandType(), getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS);
				}
				break;
		}
	}

	private static boolean testMapContainerAndImageNotNull(MapContainer m) {
		return m != null && m.getLoadedMap() != null && m.getLoadedMap().isValid();
	}

	public void onZoomClicked(View v) {
		final int zoomDiff;
		int viewId = v.getId();
		if (viewId == R.id.btn_zoom_in || viewId == R.id.area_zoom_in) {
			zoomDiff = 1;
		} else if (viewId == R.id.btn_zoom_out || viewId == R.id.area_zoom_out) {
			zoomDiff = -1;
		} else {
			return;
		}
		doZoomClicked(zoomDiff);
	}

	public void doZoomClicked(int zoomDiff) {
		if (mDeviceZoom == Const.ZOOM_UNKOWN || mZoomLock) {
			return;
		}
		if (mRequestedZoom == Const.ZOOM_UNKOWN) {
			mRequestedZoom = mDeviceZoom;
		}

		mZoomLock = true;
		if (changeZoom(mRequestedZoom, zoomDiff)) {
			float scale = zoomDiff < 0 ? 0.5f : 2f;
			mIsScaled = true;
			// TODO cejnar debug
//			mMapView.setScaleX(scale);
//			mMapView.setScaleY(scale);
//			mMapView.animate()
//					.scaleX(scale)
//					.scaleY(scale)
//					.setDuration(SCALE_ANIMATION_DURATION_MS)
//					.setInterpolator(new DecelerateInterpolator())
//					.withEndAction(() -> mZoomLock = false)
//					.start();
		} else {
			mZoomLock = false;
		}
	}

	private boolean changeZoom(int currentZoom, int zoomDiff) {
		int newZoom = currentZoom + zoomDiff;
		newZoom = Math.min(Math.max(newZoom, Const.ZOOM_MIN), Const.ZOOM_MAX);
		if (newZoom == mRequestedZoom) {
			return false;
		}
		Logger.logW(TAG, "New zoom from " + currentZoom + " to " + newZoom);
		// correct offset before zooming
		Logger.logW(TAG, "Old offset x: " + mOffsetX + " y: " + mOffsetY);
		if (zoomDiff < 0) {
			mOffsetX >>= -zoomDiff;
			mOffsetY >>= -zoomDiff;
		} else if (zoomDiff > 0) {
			mOffsetX <<= zoomDiff;
			mOffsetY <<= zoomDiff;
		}
		Logger.logW(TAG, "New offset x: " + mOffsetX + " y: " + mOffsetY);
		mRequestedZoom = newZoom;
		DataPayload<TimeStampStorable> refreshCmd = getInitialCommandType();
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

	private void cancelFling() {
		mFlingAnimator.cancel();
	}

	private void doCenterButtonClicked() {
		Logger.logD(TAG, "CENTER BTN clicked");
	}

	@Override
	public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {
		HwButtonActionDescEnum upPrimaryBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_PRIMARY_OR_UP);
		HwButtonActionDescEnum downBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_DOWN);
		HwButtonActionDescEnum secondaryActionBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_SECONDARY);

		HwButtonAction centerAction = (b, e, a) -> doCenterButtonClicked();
		HwButtonAction zoomInAction = (b, e, a) -> doZoomClicked(1);
		HwButtonAction zoomOutAction = (b, e, a) -> doZoomClicked(-1);
		// only single button available
		if (downBtn == null) {
			delegate.registerHwButtonListener(upPrimaryBtn, centerAction);
		} else {
			delegate.registerHwButtonListener(upPrimaryBtn, zoomInAction);
			delegate.registerHwButtonListener(downBtn, zoomOutAction);
			delegate.registerHwButtonListener(secondaryActionBtn, centerAction);
		}

	}
}
