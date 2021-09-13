package com.asamm.locus.addon.wear.gui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wear.ApplicationMemoryCache;
import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.application.AppPreferencesManager;
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
import com.asamm.locus.addon.wear.gui.custom.WearMapActionMoveFling;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAction;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.PointRteAction;
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
	private FloatingActionButton mFabRotAndPan;
	private ImageView mIvAmbient;

	private volatile MapContainer mLastContainer;

	private GestureDetector mDetector;

	/**
	 * simple mutex for temporary locking zooming function while animating
	 */
	private volatile boolean mZoomLock;
	private volatile boolean mIsScaled = false;

	private volatile int mDeviceZoom;
	private volatile int mRequestedZoom = Const.ZOOM_UNKOWN;
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
		DataPayload<TimeStampStorable> refreshCmd = getInitialCommandType();
		WearCommService.getInstance().sendDataItem(refreshCmd.getPath(), refreshCmd.getStorable());
	};
	private static final int PAN_DELAY = 300;
	private float mDefaultFabScale;
	private volatile MapActivityState mStatus = new MapActivityState();

	// fling handling
	private WearMapActionMoveFling.OffsetUpdatable flingUpdatable = (x, y, isLast) -> {
		runOnUiThread(() -> {
			mStatus.addOffset(x, y);
			onOffsetChanged();
			mMapView.setTranslationX(-mStatus.mMapOffsetX + mLastRenderedOffsetX);
			mMapView.setTranslationY(-mStatus.mMapOffsetY + mLastRenderedOffsetY);
			if (isLast) {
				cancelFling();
				mPanHandler.removeCallbacksAndMessages(null);
				mPanHandler.postDelayed(mPanRunnable, 0);
			}
		});
	};
	private WearMapActionMoveFling mFlingAnimator = new WearMapActionMoveFling(0, 0, flingUpdatable);

	// variable used to signal !scrolling
	private boolean mScrollLock = true;

	// handler for button hiding
	private final Handler mHandlerBtnHide = new Handler();
	private static final int BUTTON_HIDE_TIME_MS = 4000;

	// ********** METHODS ********** //

	@Override
	protected DataPayload<TimeStampStorable> getInitialCommandType() {
		final MapPeriodicParams params =
				new MapPeriodicParams(mRequestedZoom,
						appCache.getScreenWidth(),
						appCache.getScreenHeight(),
						mStatus.mMapOffsetX, mStatus.mMapOffsetY, mDensityDpi, mStatus.isAutoRotateEnabled(),
						mStatus.getLastBearing(), mDiagonal,
						mLastMapLocation.getLatitude(), mLastMapLocation.getLongitude());

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
		mIvAmbient = findViewById(R.id.imageview_ambient);
		mFabRotAndPan = findViewById(R.id.fab_rot_and_pan);

		TypedValue typedFabScale = new TypedValue();
		getResources().getValue(R.dimen.map_fab_scale, typedFabScale, true);
		mDefaultFabScale = typedFabScale.getFloat();
		mDensityDpi = getResources().getDisplayMetrics().densityDpi / 2;

		int w = appCache.getScreenWidth();
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
				mStatus.mIsPanning = true;
				mStatus.addOffset((int) (distanceX + 0.5f), (int) (distanceY + 0.5f));
				onOffsetChanged();
				refreshMapOffset(mStatus.mMapOffsetX, mStatus.mMapOffsetY, mLastRenderedOffsetX, mLastRenderedOffsetY);
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
				mStatus.mIsFlinging = true;
				mFlingAnimator = new WearMapActionMoveFling(velocityX, velocityY, flingUpdatable);
				mFlingAnimator.start(mMapView);
				return true;
			}
		});

		// Enables Always-on
		setAmbientEnabled();
		initView();
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
				mDetector.onTouchEvent(ev);
				if (!mStatus.mIsFlinging && mStatus.mIsPanning) {
					// this is the end of simple panning, request new map immediately
					mPanHandler.removeCallbacksAndMessages(null);
					mPanHandler.postDelayed(mPanRunnable, 0);
				}
			}
			doShowButtons();
			mStatus.mIsPanning = false;
			mScrollLock = true;
		} else if (action == MotionEvent.ACTION_DOWN &&
				ev.getX() > w / 7 &&
				ev.getY() > h / 5) {
			mScrollLock = false;
			super.dispatchTouchEvent(ev);
		}
		return mScrollLock ? super.dispatchTouchEvent(ev) : mDetector.onTouchEvent(ev);
	}

	@Override
	protected void onResume() {
		super.onResume();
		doShowButtons();
	}

	@Override
	protected void onStart() {
		Pair<Integer, Integer> zooms = AppPreferencesManager.getZoomValues(this);
		mDeviceZoom = zooms.first;
		mRequestedZoom = zooms.second;
		mStatus.setAutoRotate(AppPreferencesManager.isMapAutoRotateEnabled(this));
		Pair<Integer, Integer> offset = AppPreferencesManager.getMapOffsetValues(this);
		mStatus.setOffset(offset.first, offset.second);
		mStatus.setLastBearing(AppPreferencesManager.getMapBearing(this));
		onOffsetChanged();
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		AppPreferencesManager.persistZoomValues(this, mDeviceZoom, mRequestedZoom);
		AppPreferencesManager.persistMapAutoRotateEnabled(this, mStatus.isAutoRotateEnabled());
		AppPreferencesManager.persistMapOffsetValues(this, mStatus.mMapOffsetX, mStatus.mMapOffsetY);
		AppPreferencesManager.persistMapBearing(this, mStatus.getLastBearing());
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
			mStatus.setLastBearing(data.getBearing());

			refreshMapOffset(mStatus.mMapOffsetX, mStatus.mMapOffsetY, mLastRenderedOffsetX, mLastRenderedOffsetY);
			mMapView.setImageDrawable(new BitmapDrawable(getResources(), map));

			if (data.getZoomWear() == mRequestedZoom && mIsScaled) {
				mMapView.animate().cancel();
				mMapView.setScaleX(1f);
				mMapView.setScaleY(1f);
				mZoomLock = false;
				mIsScaled = false;
			}
		} else {
			Logger.INSTANCE.logE(TAG, (data == null ? "data" : data.getLoadedMap() == null ?
					"data.loadedMap" : "data.loadedMap.image") + " is null.");
		}
	}

	private void refreshMapOffset(int offsetX, int offsetY, int renderOffsetX, int renderOffsetY) {
		runOnUiThread(() -> {
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
			mTvNavPanelDistValue.setText(UtilsFormat.INSTANCE.formatDistance(
					data.getUnitsFormatLength(), data.getNavPoint1Dist(), true));
			mTvNavPanelDistUnits.setText(UtilsFormat.INSTANCE.formatDistanceUnits(
					data.getUnitsFormatLength(), data.getNavPoint1Dist()));
		} else {
			mIvNavPanelTop.setVisibility(View.INVISIBLE);
			mTvNavPanelDistValue.setText("");
			mTvNavPanelDistUnits.setText("");
		}
	}

	private void setNavImageForAction(ImageView view, int pointRteActionId) {
		PointRteAction action = PointRteAction.Companion.getActionById(pointRteActionId);
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
				} else if (tmp.getLoadedMap().getNumOfNotYetLoadedTiles() > 0 && !mStatus.mIsFlinging) {
					getMainApplication().sendDataWithWatchDog(getInitialCommandType(),
							getInitialCommandResponseType(), WATCHDOG_TIMEOUT_MS);
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
		if (!mStatus.mButtonsVisible) {
			return;
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

	private boolean changeZoom(int currentZoom, int zoomDiff) {
		int newZoom = currentZoom + zoomDiff;
		newZoom = Math.min(Math.max(newZoom, Const.ZOOM_MIN), Const.ZOOM_MAX);
		if (newZoom == mRequestedZoom) {
			return false;
		}
		// correct offset before zooming
		if (zoomDiff < 0) {
			mStatus.divideOffset(1 << -zoomDiff);
		} else if (zoomDiff > 0) {
			mStatus.multiplyOffset(1 << zoomDiff);
		}
		onOffsetChanged();
		mRequestedZoom = newZoom;
		DataPayload<TimeStampStorable> refreshCmd = getInitialCommandType();
		WearCommService.getInstance().sendDataItem(refreshCmd.getPath(), refreshCmd.getStorable());
		return true;
	}

	@Override
	public boolean isUsePeriodicData() {
		return true;
	}

	private void doHideButtons() {
		mHandlerBtnHide.removeCallbacksAndMessages(null);
		animateButton(mBtnZoomIn, false);
		animateButton(mBtnZoomOut, false);
		animateButton(mFabRotAndPan, false);
		mStatus.mButtonsVisible = false;
	}

	private void doShowButtons() {
		if (mStatus.mIsAmbient) {
			return;
		}
		animateButton(mBtnZoomIn, true);
		animateButton(mBtnZoomOut, true);
		animateButton(mFabRotAndPan, true);
		// give a little time to animate the buttons for a bit before enabling buttons function
		new Handler().postDelayed(() -> mStatus.mButtonsVisible = true, SCALE_ANIMATION_DURATION_MS / 2);
		mHandlerBtnHide.removeCallbacksAndMessages(null);
		mHandlerBtnHide.postDelayed(this::doHideButtons, BUTTON_HIDE_TIME_MS);
	}

	private void animateButton(View v, boolean visible) {
		v.animate().cancel();
		v.animate()
				.scaleX(visible ? mDefaultFabScale : 0)
				.scaleY(visible ? mDefaultFabScale : 0)
				.setDuration(SCALE_ANIMATION_DURATION_MS)
				.setInterpolator(new DecelerateInterpolator())
				.withStartAction(() -> v.setVisibility(View.VISIBLE))
				.withEndAction(() -> v.setVisibility(visible ? View.VISIBLE : View.GONE))
				.start();
	}

	@Override
	public void onEnterAmbient(Bundle ambientDetails) {
		super.onEnterAmbient(ambientDetails);
		doHideButtons();
		mIvAmbient.setVisibility(View.VISIBLE);
        mLlNavPanel.setBackgroundColor(getColor(R.color.base_dark_primary));
        mTvNavPanelDistValue.setTextColor(Color.WHITE);
        mTvNavPanelDistUnits.setTextColor(Color.WHITE);
		refreshMapView(mLastContainer);
		mStatus.mIsAmbient = true;
	}

	@Override
	public void onExitAmbient() {
		super.onExitAmbient();
		mStatus.mIsAmbient = false;
		doShowButtons();
		mIvAmbient.setVisibility(View.GONE);
        mLlNavPanel.setBackgroundColor(getColor(R.color.panel_map_side));
        mTvNavPanelDistValue.setTextColor(getColor(R.color.base_dark_primary));
        mTvNavPanelDistUnits.setTextColor(getColor(R.color.base_dark_primary));
		refreshMapView(mLastContainer);
	}

	private void cancelFling() {
		mFlingAnimator.cancel();
		mStatus.mIsFlinging = false;
	}

	public void onCenterRotateButtonClicked(View v) {
		if (!mStatus.mButtonsVisible) {
			return;
		}
		doCenterRotateButtonClick();
	}

	private void doCenterRotateButtonClick() {
		if (!mStatus.isMapCentered()) {
			cancelFling();
			mStatus.mIsPanning = false;
			mMapView.animate().cancel();
			mMapView.animate()
					.translationXBy(mStatus.mMapOffsetX)
					.translationYBy(mStatus.mMapOffsetY)
					.setDuration(SCALE_ANIMATION_DURATION_MS)
					.setInterpolator(new DecelerateInterpolator());
			mStatus.setOffset(0,0);
		} else if (mStatus.isAutoRotateEnabled()) {
			mStatus.setAutoRotate(false);
		} else {
			mStatus.setAutoRotate(true);
		}
		onOffsetChanged();
		mPanHandler.removeCallbacksAndMessages(null);
		mPanHandler.post(mPanRunnable);
	}

	private void onOffsetChanged() {
		if (mStatus.isMapCentered()) {
			setRotPanBtnToRotation();
		} else {
			mFabRotAndPan.clearColorFilter();
			if (mFabRotAndPan.getTag() == null || !mFabRotAndPan.getTag().equals(R.drawable.ic_my_location)) {
				mFabRotAndPan.setTag(R.drawable.ic_my_location);
				mFabRotAndPan.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_my_location));
			}
		}
	}

	private void setRotPanBtnToRotation() {
		mFabRotAndPan.setColorFilter(ContextCompat.getColor(this, R.color.base_primary));
		int resId = mStatus.isAutoRotateEnabled() ? R.drawable.ic_rotate_screen : R.drawable.ic_my_location;
		if (mFabRotAndPan.getTag() == null || !mFabRotAndPan.getTag().equals(resId)) {
			mFabRotAndPan.setTag(resId);
			mFabRotAndPan.setImageDrawable(ContextCompat.getDrawable(this, resId));
		}
	}

	@Override
	public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {
		enableCustomRotatryActions();
		HwButtonActionDescEnum upPrimaryBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_PRIMARY_OR_UP);
		HwButtonActionDescEnum downBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_DOWN);
		HwButtonActionDescEnum secondaryActionBtn =
				delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_SECONDARY);

		HwButtonAction centerAction = () -> {
			doShowButtons();
			doCenterRotateButtonClick();
		};
		HwButtonAction zoomInAction = () -> doZoomClicked(1);
		HwButtonAction zoomOutAction = () -> doZoomClicked(-1);

		delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_UP, zoomOutAction);
		delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_DOWN, zoomInAction);
		// only single button available
		if (delegate.getNumMultifunctionButtons() < 2) {
			delegate.registerHwButtonListener(upPrimaryBtn, centerAction);
		} else {
			delegate.registerHwButtonListener(HwButtonActionDescEnum.BTN_2_LONG_PRESS,
					() -> {
						delegate.setUseHwButtons(false);
						startLocusWearActivity(TrackRecordActivity.class);
					});
			delegate.registerHwButtonListener(upPrimaryBtn, zoomInAction);
			delegate.registerHwButtonListener(downBtn, zoomOutAction);
			delegate.registerHwButtonListener(secondaryActionBtn, centerAction);
		}
	}

	private class MapActivityState {
		private volatile boolean mIsAmbient = false;
		private volatile boolean mButtonsVisible = true;
		private volatile boolean mAutoRotate = false;
		private volatile boolean mIsFlinging = false;
		private volatile boolean mIsPanning = false;
		private volatile int mMapOffsetX = 0;
		private volatile int mMapOffsetY = 0;
		private volatile short mLastBearing = 0;

		private boolean isAutoRotateEnabled() {
			return mAutoRotate;
		}

		private void setAutoRotate(boolean autoRotate) {
			this.mAutoRotate = autoRotate;
		}

		private boolean isMapCentered() {
			return mMapOffsetX == 0 && mMapOffsetY == 0;
		}

		/**
		 * @return if map has no offset then returns APPLY_DEVICE_BEARING to let device
		 * set last known bearing automatically. If there is offset, return last "locked bearing"
		 */
		private short getLastBearing() {
			return isMapCentered() ? MapPeriodicParams.APPLY_DEVICE_BEARING : mLastBearing;
		}

		/**
		 * sets new bearing if there is no map offset. If there is offset, the bearing is "locked"
		 * and calling this setter is ignored
		 *
		 * @param lastBearing
		 */
		private void setLastBearing(short lastBearing) {
			if (isMapCentered()) {
				mLastBearing = lastBearing;
			}
		}

		private void setOffset(int newOffsetX, int newOffsetY) {
			mMapOffsetX = newOffsetX;
			mMapOffsetY = newOffsetY;
		}
		private void addOffset(int x, int y) {
			mMapOffsetX += x;
			mMapOffsetY += y;
		}

		private void multiplyOffset(int multiplier) {
			mMapOffsetX *= multiplier;
			mMapOffsetY *= multiplier;
		}

		private void divideOffset(int divisor) {
			mMapOffsetX /= divisor;
			mMapOffsetY /= divisor;
		}
	}
}
