/**
 * Created by Milan Cejnar
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.features.map

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.WatchDogPredicate
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.MapPeriodicParams
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand
import com.asamm.locus.addon.wear.communication.WearCommService
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordActivity
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate
import com.asamm.locus.addon.wear.gui.custom.NavHelper
import com.asamm.locus.addon.wear.gui.custom.WearMapActionMoveFling
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAction
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum
import com.asamm.locus.addon.wear.utils.UtilsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import locus.api.android.features.periodicUpdates.UpdateContainer
import locus.api.android.utils.UtilsFormat
import locus.api.objects.extra.Location
import locus.api.objects.extra.PointRteAction
import locus.api.utils.Logger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Activity with map preview
 */
@Suppress("DEPRECATION")
open class MapActivity : LocusWearActivity() {

    // reference to map view
    private lateinit var mapView: ImageView

    // main container
    private lateinit var llNavPanel: LinearLayout

    // top navigation command (next)
    private lateinit var ivNavPanelTop: ImageView

    // main navigation command (current)
    private lateinit var ivNavPanelMiddle: ImageView

    // distance to next command (value)
    private lateinit var tvNavPanelDistValue: TextView

    // distance to next command (units)
    private lateinit var tvNavPanelDistUnits: TextView

    // zoom-in button
    private lateinit var btnZoomIn: ImageView

    // zoom-out button
    private lateinit var btnZoomOut: ImageView

    // center/rotate dual button
    private lateinit var fabRotPan: FloatingActionButton

    // info image when ambient mode is active
    private lateinit var ivAmbient: ImageView

    @Volatile
    private var lastContainer: MapContainer? = null

    /**
     * Detector for gestures.
     */
    private lateinit var detector: GestureDetector

    /**
     * simple mutex for temporary locking zooming function while animating
     */
    @Volatile
    private var zoomLock = false

    @Volatile
    private var isScaled = false

    @Volatile
    private var mapZoom = PreferencesEx.mapZoom

    @Volatile
    private var mapZoomRequest = Const.ZOOM_UNKOWN.toInt()
    private var densityDpi = 0
    private var diagonal = 0

    // reference to app cache
    private val appCache by lazy {
        (application as MainApplication).cache
    }

    /**
     * Last rendered location and offset
     */
    @Volatile
    private var lastMapLocation = Location(0.0, 0.0)

    @Volatile
    private var lastRenderedOffsetX = 0

    @Volatile
    private var lastRenderedOffsetY = 0

    // map panning handler to postpone new map request if map is currently scrolling
    private val panHandler = Handler()

    private val panRunnable = Runnable {
        val refreshCmd = initialCommandType
        WearCommService.instance.sendDataItem(refreshCmd.path, refreshCmd.storable)
    }
    private var defaultFabScale = 0f

    // current status
    private val mapState = MapActivityState()

    // fling handling
    private val flingUpdatable = WearMapActionMoveFling.OffsetUpdatable { x: Int, y: Int, isLast: Boolean ->
        runOnUiThread {
            mapState.addOffset(x, y)
            refreshCenterRotateButton()
            mapView.translationX = (-mapState.mapOffsetX + lastRenderedOffsetX).toFloat()
            mapView.translationY = (-mapState.mapOffsetY + lastRenderedOffsetY).toFloat()
            if (isLast) {
                cancelFling()
                panHandler.removeCallbacksAndMessages(null)
                panHandler.postDelayed(panRunnable, 0)
            }
        }
    }
    private var flingAnimator = WearMapActionMoveFling(0.0f, 0.0f, flingUpdatable)

    // variable used to signal !scrolling
    private var scrollLock = true

    // handler for button hiding
    private val handlerBtnHide = Handler()

    override val initialCommandType: DataPayload<TimeStampStorable>
        get() {
            // prepare parameters
            val params = MapPeriodicParams(
                    mapZoomRequest,
                    appCache.screenWidth,
                    appCache.screenHeight,
                    mapState.mapOffsetX,
                    mapState.mapOffsetY,
                    densityDpi,
                    mapState.isAutoRotateEnabled,
                    mapState.lastBearing,
                    diagonal,
                    lastMapLocation.latitude,
                    lastMapLocation.longitude
            )

            // generate payload container
            return DataPayload(
                    DataPath.GET_PERIODIC_DATA,
                    PeriodicCommand(
                            PeriodicCommand.IDX_PERIODIC_MAP,
                            MAP_REFRESH_PERIOD_MS, params
                    )
            )
        }

    override val initialCommandResponseType: DataPath
        get() = DataPath.PUT_MAP

    override val supportAmbientMode: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // get references to views
        mapView = findViewById(R.id.image_view_map)
        llNavPanel = findViewById(R.id.linear_layout_panel_navigation)
        ivNavPanelTop = findViewById(R.id.image_view_next)
        ivNavPanelMiddle = findViewById(R.id.image_view_main)
        tvNavPanelDistValue = findViewById(R.id.text_view_dist_value)
        tvNavPanelDistUnits = findViewById(R.id.text_view_dist_units)
        btnZoomIn = findViewById(R.id.btn_zoom_in)
        btnZoomOut = findViewById(R.id.btn_zoom_out)
        ivAmbient = findViewById(R.id.imageview_ambient)
        fabRotPan = findViewById(R.id.fab_rot_and_pan)
        val typedFabScale = TypedValue()
        resources.getValue(R.dimen.map_fab_scale, typedFabScale, true)
        defaultFabScale = typedFabScale.float
        densityDpi = resources.displayMetrics.densityDpi / 2
        val w = appCache.screenWidth
        val h = appCache.screenHeight
        val isRound = resources.configuration.isScreenRound
        diagonal = if (isRound) {
            (max(w, h) + 1) / 2
        } else {
            (sqrt((h * h + w * w).toDouble()) + 1).toInt() / 2
        }
        detector = GestureDetector(this, object : SimpleOnGestureListener() {

            override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float): Boolean {
                mapState.isPanning = true
                mapState.addOffset((distanceX + 0.5f).toInt(), (distanceY + 0.5f).toInt())
                refreshCenterRotateButton()
                refreshMapOffset(
                        mapState.mapOffsetX,
                        mapState.mapOffsetY,
                        lastRenderedOffsetX,
                        lastRenderedOffsetY
                )
                panHandler.removeCallbacksAndMessages(null)
                panHandler.postDelayed(panRunnable, PAN_DELAY.toLong())
                return true
            }

            override fun onDown(event: MotionEvent): Boolean {
                cancelFling()
                Log.d(TAG, "onDown is scrolling: $scrollLock")
                return true
            }

            override fun onFling(
                    event1: MotionEvent, event2: MotionEvent,
                    velocityX: Float, velocityY: Float): Boolean {
                cancelFling()
                mapState.isFlinging = true
                flingAnimator = WearMapActionMoveFling(velocityX, velocityY, flingUpdatable)
                flingAnimator.start(mapView)
                return true
            }
        })
        initView()
    }

    override fun onStart() {
        mapState.isAutoRotateEnabled = PreferencesEx.mapAutoRotateEnabled
        mapState.setOffset(
                PreferencesEx.mapOffsetX,
                PreferencesEx.mapOffsetY
        )
        mapState.lastBearing = PreferencesEx.mapBearing
        refreshCenterRotateButton()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        doShowButtons()
    }

    override fun onStop() {
        super.onStop()
        PreferencesEx.mapZoom = mapZoom
        PreferencesEx.mapAutoRotateEnabled = mapState.isAutoRotateEnabled
        PreferencesEx.mapOffsetX = mapState.mapOffsetX
        PreferencesEx.mapOffsetY = mapState.mapOffsetY
        PreferencesEx.mapBearing = mapState.lastBearing
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val w = appCache.screenHeight
        val h = appCache.screenHeight
        val action = ev.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_UP
                || drawer?.isOpened == true) {
            // finish possible panning
            if (!scrollLock) {
                detector.onTouchEvent(ev)
                if (!mapState.isFlinging && mapState.isPanning) {
                    // this is the end of simple panning, request new map immediately
                    panHandler.removeCallbacksAndMessages(null)
                    panHandler.postDelayed(panRunnable, 0)
                }
            }
            doShowButtons()
            mapState.isPanning = false
            scrollLock = true
        } else if (action == MotionEvent.ACTION_DOWN && ev.x > w / 7.0f && ev.x < w - w / 7.0f && ev.y < h - h / 5.0f) {
            // free to handle by gesture detector. Keep left/right border free for gestures and
            // bottom area for swipe of bottom drawer
            scrollLock = false
            super.dispatchTouchEvent(ev)
        }

        // forward event
        return if (scrollLock) {
            super.dispatchTouchEvent(ev)
        } else {
            detector.onTouchEvent(ev)
        }
    }

    /**
     * Initialize view before first data arrives
     */
    private fun initView() {
        val cache = (application as MainApplication).cache
        val savedContainer = cache.lastMapData
        if (savedContainer?.isMapPresent == true) {
            refreshMapView(savedContainer)
        } else {
            mapView.background = AppCompatResources.getDrawable(
                    this,
                    R.drawable.var_map_loading_tile_256
            )
        }

        // hide navigation panel
        refreshPanelNavigation(null)
    }

    private fun setNavImageForAction(view: ImageView, pointRteActionId: Int) {
        val action = PointRteAction.getActionById(pointRteActionId)
        val img = NavHelper.getNavPointImageRes(action)
        if (Integer.valueOf(img) != view.tag) {
            view.setImageResource(img)
            view.tag = img
        }
    }

    override fun consumeNewData(path: DataPath, data: TimeStampStorable?) {
        super.consumeNewData(path, data)
        when (path) {
            DataPath.PUT_MAP -> {
                val tmp = data as MapContainer?
                if (tmp != null) {
                    lastContainer = tmp
                    refreshLayout(tmp)
                }

                if (!testMapContainerAndImageNotNull(tmp)) {
                    mainApplication.sendDataWithWatchDogConditionable(
                            initialCommandType,
                            initialCommandResponseType,
                            WATCHDOG_TIMEOUT_MS.toLong(),
                            WatchDogPredicate { cont: MapContainer? ->
                                testMapContainerAndImageNotNull(cont)
                            })
                } else if ((tmp?.loadedMap?.numOfNotYetLoadedTiles ?: 0) > 0 && !mapState.isFlinging) {
                    mainApplication.sendDataWithWatchDog(
                            initialCommandType,
                            initialCommandResponseType,
                            WATCHDOG_TIMEOUT_MS.toLong()
                    )
                } else {
                    mainApplication.addWatchDog(
                            initialCommandType,
                            initialCommandResponseType,
                            WATCHDOG_TIMEOUT_MS.toLong()
                    )
                }
            }
            else -> {
                Logger.logD(TAG, "consumeNewData($path, $data), data not handled")
            }
        }
    }

    fun onZoomClicked(v: View) {
        if (!mapState.buttonsVisible) {
            return
        }
        val zoomDiff: Int
        val viewId = v.id
        zoomDiff = when (viewId) {
            R.id.btn_zoom_in,
            R.id.area_zoom_in -> {
                1
            }
            R.id.btn_zoom_out,
            R.id.area_zoom_out -> {
                -1
            }
            else -> {
                return
            }
        }
        doZoomClicked(zoomDiff)
    }

    private fun doZoomClicked(zoomDiff: Int) {
        // check state
        if (zoomLock) {
            return
        }

        // lock zoom level
        zoomLock = true
        if (changeZoom(mapZoom, zoomDiff)) {
            val scale = if (zoomDiff < 0) 0.5f else 2f
            // cancel currently running animation. This will break a flow little, so it may be improved
            // by keeping unfinished values and using them here again
            mapView.animate()
                    .cancel()
            isScaled = true
            mapView.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(SCALE_ANIMATION_DURATION_MS.toLong())
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { zoomLock = false }
                    .start()
        } else {
            zoomLock = false
        }
    }

    override val isUsePeriodicData: Boolean
        get() = true

    private fun doHideButtons() {
        handlerBtnHide.removeCallbacksAndMessages(null)
        animateButton(btnZoomIn, false)
        animateButton(btnZoomOut, false)
        animateButton(fabRotPan, false)
        mapState.buttonsVisible = false
    }

    private fun doShowButtons() {
        if (mapState.isAmbient) {
            return
        }
        animateButton(btnZoomIn, true)
        animateButton(btnZoomOut, true)
        animateButton(fabRotPan, true)

        // give a little time to animate the buttons for a bit before enabling buttons function
        Handler().postDelayed(
                { mapState.buttonsVisible = true },
                (SCALE_ANIMATION_DURATION_MS / 2).toLong()
        )
        handlerBtnHide.removeCallbacksAndMessages(null)
        handlerBtnHide.postDelayed({ doHideButtons() }, BUTTON_HIDE_TIME_MS.toLong())
    }

    private fun animateButton(v: View, visible: Boolean) {
        v.animate()
                .cancel()
        v.animate()
                .scaleX(if (visible) defaultFabScale else 0.0f)
                .scaleY(if (visible) defaultFabScale else 0.0f)
                .setDuration(SCALE_ANIMATION_DURATION_MS.toLong())
                .setInterpolator(DecelerateInterpolator())
                .withStartAction { v.visibility = View.VISIBLE }
                .withEndAction { v.visibility = if (visible) View.VISIBLE else View.GONE }
                .start()
    }

    //*************************************************
    // REFRESH MAP CONTENT
    //*************************************************

    /**
     * Refresh all map data.
     */
    private fun refreshLayout(data: MapContainer) {
        Logger.logD(TAG, "refreshLayout($data)")
        runOnUiThread {
            refreshMapView(data)
            refreshPanelNavigation(data)
        }
    }

    /**
     * Refreshes map image view
     */
    private fun refreshMapView(data: MapContainer) {
        if (testMapContainerAndImageNotNull(data)) {
            val map = data.loadedMap?.getAsImage()
            lastMapLocation = data.lastLocation
            lastRenderedOffsetX = data.offsetX
            lastRenderedOffsetY = data.offsetY
            mapState.lastBearing = data.bearing
            refreshMapOffset(
                    mapState.mapOffsetX,
                    mapState.mapOffsetY,
                    lastRenderedOffsetX,
                    lastRenderedOffsetY
            )
            mapView.setImageDrawable(BitmapDrawable(resources, map))

            // reset scale if valid data received
            if (data.zoomRequest.toInt() == mapZoom && isScaled) {
                mapView.animate().cancel()
                mapView.scaleX = 1f
                mapView.scaleY = 1f
                zoomLock = false
                isScaled = false
            }
        } else {
            Logger.logE(
                    TAG, (when (data.loadedMap) {
                null -> {
                    "data.loadedMap"
                }
                else -> {
                    "data.loadedMap.image"
                }
            }) + " is null."
            )
        }
    }

    private fun refreshMapOffset(
            offsetX: Int,
            offsetY: Int,
            renderOffsetX: Int,
            renderOffsetY: Int) {
        runOnUiThread {
            mapView.translationX = (-offsetX + renderOffsetX).toFloat()
            mapView.translationY = (-offsetY + renderOffsetY).toFloat()
        }
    }

    // ZOOM SYSTEM

    private fun changeZoom(currentZoom: Int, zoomDiff: Int): Boolean {
        // compute new zoom value
        var newZoom = currentZoom + zoomDiff
        newZoom = min(max(newZoom, Const.ZOOM_MIN), Const.ZOOM_MAX)
        if (newZoom == currentZoom) {
            return false
        }

        // correct offset before zooming
        if (zoomDiff < 0) {
            mapState.divideOffset(1 shl -zoomDiff)
        } else if (zoomDiff > 0) {
            mapState.multiplyOffset(1 shl zoomDiff)
        }

        // perform zoom
        refreshCenterRotateButton()
        mapZoom = newZoom
        mapZoomRequest = newZoom
        val refreshCmd = initialCommandType
        WearCommService.instance.sendDataItem(refreshCmd.path, refreshCmd.storable)
        return true
    }

    /**
     * Refresh panel with navigation.
     */
    private fun refreshPanelNavigation(data: MapContainer?) {
        if (data == null || data.guideType != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION) {
            llNavPanel.visibility = View.GONE
            return
        }
        llNavPanel.visibility = View.VISIBLE

        // action for current point
        setNavImageForAction(
                ivNavPanelMiddle,
                if (data.isNavValid) data.navPointAction1Id else PointRteAction.UNDEFINED.id
        )
        if (data.isNavValid) {
            if (ivNavPanelTop.visibility != View.VISIBLE) {
                ivNavPanelTop.visibility = View.VISIBLE
            }
            // action for next point
            setNavImageForAction(ivNavPanelTop, data.navPointAction2Id)
            tvNavPanelDistValue.text = UtilsFormat.formatDistance(
                    data.unitsFormatLength, data.navPoint1Dist, true
            )
            tvNavPanelDistUnits.text = UtilsFormat.formatDistanceUnits(
                    data.unitsFormatLength, data.navPoint1Dist
            )
        } else {
            ivNavPanelTop.visibility = View.INVISIBLE
            tvNavPanelDistValue.text = ""
            tvNavPanelDistUnits.text = ""
        }
    }

    // AMBIENT

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
        doHideButtons()
        ivAmbient.visibility = View.VISIBLE
        llNavPanel.setBackgroundColor(getColor(R.color.base_dark_primary))
        tvNavPanelDistValue.setTextColor(Color.WHITE)
        tvNavPanelDistUnits.setTextColor(Color.WHITE)
        lastContainer?.let {
            refreshMapView(it)
        }
        mapState.isAmbient = true
    }

    override fun onExitAmbient() {
        super.onExitAmbient()
        mapState.isAmbient = false
        doShowButtons()
        ivAmbient.visibility = View.GONE
        llNavPanel.setBackgroundColor(getColor(R.color.panel_map_side))
        tvNavPanelDistValue.setTextColor(getColor(R.color.base_dark_primary))
        tvNavPanelDistUnits.setTextColor(getColor(R.color.base_dark_primary))
        lastContainer?.let {
            refreshMapView(it)
        }
    }

    private fun cancelFling() {
        flingAnimator.cancel()
        mapState.isFlinging = false
    }

    /**
     * Handle click on the center/rotate button.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onCenterRotateButtonClicked(view: View) {
        // ignore if button is not visible
        if (!mapState.buttonsVisible) {
            return
        }

        // handle click
        handleCenterRotateButtonClicked()
    }

    /**
     * Handle clicked center/rotate button.
     */
    private fun handleCenterRotateButtonClicked() {
        if (!mapState.isMapCentered) {
            cancelFling()
            mapState.isPanning = false

            // animate centering
            mapView.animate().cancel()
            mapView.animate()
                    .translationXBy(mapState.mapOffsetX.toFloat())
                    .translationYBy(mapState.mapOffsetY.toFloat())
                    .setDuration(SCALE_ANIMATION_DURATION_MS.toLong()).interpolator =
                    DecelerateInterpolator()
            mapState.setOffset(0, 0)
        } else {
            mapState.isAutoRotateEnabled = !mapState.isAutoRotateEnabled
        }

        // handle change in offset
        refreshCenterRotateButton()
        panHandler.removeCallbacksAndMessages(null)
        panHandler.post(panRunnable)
    }

    private fun refreshCenterRotateButton() {
        // prepare image
        val imgToSet = if (mapState.isMapCentered) {
            if (mapState.isAutoRotateEnabled) {
                R.drawable.ic_rotate_screen
            } else {
                R.drawable.ic_my_location_on
            }
        } else {
            R.drawable.ic_my_location_off
        }

        // set if image is new
        mapState.lastBearing
        if (fabRotPan.tag == null
                || fabRotPan.tag != imgToSet) {
            fabRotPan.tag = imgToSet
            fabRotPan.setImageResource(imgToSet)
        }
    }

    override fun registerHwKeyActions(delegate: LocusWearActivityHwKeyDelegate) {
        enableCustomRotaryActions()
        val upPrimaryBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_PRIMARY_OR_UP)
        val downBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_DOWN)
        val secondaryActionBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_SECONDARY)
        val centerAction = HwButtonAction {
            doShowButtons()
            handleCenterRotateButtonClicked()
        }
        val zoomInAction = HwButtonAction { doZoomClicked(1) }
        val zoomOutAction = HwButtonAction { doZoomClicked(-1) }
        delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_UP, zoomOutAction)
        delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_DOWN, zoomInAction)
        // only single button available
        if (delegate.numMultifunctionButtons < 2) {
            delegate.registerHwButtonListener(upPrimaryBtn, centerAction)
        } else {
            delegate.registerHwButtonListener(
                    HwButtonActionDescEnum.BTN_2_LONG_PRESS
            ) {
                delegate.isUseHwButtons = false
                startLocusWearActivity(TrackRecordActivity::class.java)
            }
            delegate.registerHwButtonListener(upPrimaryBtn, zoomInAction)
            delegate.registerHwButtonListener(downBtn, zoomOutAction)
            delegate.registerHwButtonListener(secondaryActionBtn, centerAction)
        }
    }

    private inner class MapActivityState {

        var isAmbient = false

        var buttonsVisible = true

        var isAutoRotateEnabled = false

        var isFlinging = false

        var isPanning = false

        var mapOffsetX = 0

        var mapOffsetY = 0

        /**
         * If map has no offset then returns APPLY_DEVICE_BEARING to let device
         * set last known bearing automatically. If there is offset, return last "locked bearing".
         *
         * If there is no map offset. If there is offset, the bearing is "locked"
         * and calling this setter is ignored
         */
        var lastBearing: Short = 0
            get() {
                return if (isMapCentered) MapPeriodicParams.APPLY_DEVICE_BEARING else field
            }
            set(value) {
                if (isMapCentered) {
                    field = value
                }
            }

        val isMapCentered: Boolean
            get() = mapOffsetX == 0 && mapOffsetY == 0


        fun setOffset(newOffsetX: Int, newOffsetY: Int) {
            mapOffsetX = newOffsetX
            mapOffsetY = newOffsetY
        }

        fun addOffset(x: Int, y: Int) {
            mapOffsetX += x
            mapOffsetY += y
        }

        fun multiplyOffset(multiplier: Int) {
            mapOffsetX *= multiplier
            mapOffsetY *= multiplier
        }

        fun divideOffset(divisor: Int) {
            mapOffsetX /= divisor
            mapOffsetY /= divisor
        }
    }

    companion object {

        private const val TAG = "MapActivity"

        /**
         * Refresh of the map.
         */
        private val MAP_REFRESH_PERIOD_MS = if (UtilsCompat.isAPI30orMore) {
            2500
        } else {
            5000
        }

        /**
         * Timeout for WatchDog.
         */
        private val WATCHDOG_TIMEOUT_MS = MAP_REFRESH_PERIOD_MS * 3

        /**
         * Animation for the scale.
         */
        private const val SCALE_ANIMATION_DURATION_MS = 200

        private const val PAN_DELAY = 300

        /**
         * Timeout for the automatic button hiding.
         */
        private const val BUTTON_HIDE_TIME_MS = 4000

        private fun testMapContainerAndImageNotNull(m: MapContainer?): Boolean {
            return m?.loadedMap != null && m.loadedMap?.isValid() == true
        }
    }
}