/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.gui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import com.asamm.locus.addon.wear.BuildConfig
import com.asamm.locus.addon.wear.MainApplication
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.utils.TriStateLogicEnum
import com.asamm.locus.addon.wear.communication.WearCommService
import com.asamm.locus.addon.wear.features.error.AppFailType
import com.asamm.locus.addon.wear.features.map.MapActivity
import com.asamm.locus.addon.wear.features.settings.MainSettingsActivity
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordActivity
import com.asamm.locus.addon.wear.gui.custom.MainNavigationDrawer
import com.asamm.logger.Logger
import com.asamm.loggerV2.logD
import com.google.android.gms.wearable.NodeApi.GetConnectedNodesResult
import java.text.DateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base class for wearable activities containing basic comm handling and other common features
 */
abstract class LocusWearActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    var state = WearActivityState.ON_CREATE

    @JvmField
    protected var drawer: MainNavigationDrawer? = null

    private var drawerCloseArrowImg: ImageView? = null
    private var tvNavDrawerTime: TextView? = null

    // handler for the nav drawer
    private val navDrawerTimeHandler = Handler(Looper.getMainLooper())
    private var dateFormat: DateFormat? = null

    /**
     * Can be set by inheriting activity to skip drawer peek behavior on next resume
     */
    private var ignoreNextDrawerPeek = false

    /**
     * number of ticks of mConnectionFailedTimer
     */
    @Volatile
    private var ticks: Byte = 0

    /**
     * Activated on start for monitoring initial handshake exchange.
     */
    private var connectionFailedTimer: CountDownTimer? = null
    private val connectionTimerLock = Any()

    /**
     * only used on start in connection failed timer to monitor initial handshake request
     */
    @Volatile
    private var isHandShakeReceived = false

    /**
     * only used on start in connection failed timer to monitor initial command request
     */
    @Volatile
    protected var isInitialRequestReceived = false

    private val getConnectedNodesSent = AtomicBoolean(false)

    private val handshakeSent = AtomicBoolean(false)

    private val handshakeRetrySent = AtomicBoolean(false)
    private var dbgClickCounter: Byte = 0

    /**
     * Flag specifying if mobile phone is connected.
     */
    @Volatile
    private var isNodeConnected = false

    /**
     * Delegate for handling HW keys actions
     */
    private var hwKeyDelegate: LocusWearActivityHwKeyDelegate? = null

    /**
     * Each activity should define initial command which is sent automatically onStart()
     *
     * @return DataPath or null if activity want to just check connection on start and
     * not issue any other custom command.
     */
    protected abstract val initialCommandType: DataPayload<*>?

    /**
     * Expected type of response for initialCommand
     */
    protected abstract val initialCommandResponseType: DataPath?

    /**
     * This method is called only once as a callback, that activity's initial command
     * got response and that comm is ready
     */
    private fun onGotInitialCommandResponse() {}

    /**
     * Flag if ambient mode should be support for this activity.
     *
     * When this flag is set to 'true', methods [onEnterAmbient] and [onExitAmbient] should be overwrote.
     */
    open val supportAmbientMode: Boolean = false

    // LIFE CYCLE

    override fun onCreate(savedInstanceState: Bundle?) {
        state = WearActivityState.ON_CREATE
        super.onCreate(savedInstanceState)

        // set dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // enable ambient support
        if (supportAmbientMode) {
            AmbientModeSupport.attach(this)
        }
    }

    override fun onStart() {
        state = WearActivityState.ON_START
        super.onStart()

        drawer = findViewById(R.id.navigation_drawer)
        drawerCloseArrowImg = findViewById(R.id.imageViewDrawerOpened)
        tvNavDrawerTime = findViewById(R.id.navDrawerTvTime)
        if (tvNavDrawerTime != null && IS_DEBUG_MODE_ENABLED) {
            tvNavDrawerTime!!.setOnClickListener {
                dbgClickCounter++
                if (dbgClickCounter.toInt() == 6) {
                    dbgClickCounter = 0
                    val isDebug = PreferencesEx.isDebug
                    PreferencesEx.isDebug = !isDebug
                    Toast.makeText(
                        this,
                        "Debug mode " + if (isDebug) "disabled" else "enabled",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        dateFormat = android.text.format.DateFormat.getTimeFormat(applicationContext)
    }

    override fun onResume() {
        state = WearActivityState.ON_RESUME
        super.onResume()

        if (tvNavDrawerTime != null) {
            navDrawerTimeHandler.post(object : Runnable {

                @SuppressLint("SetTextI18n")
                override fun run() {
                    val time = dateFormat!!.format(Date())
                    tvNavDrawerTime!!.text = (if (time.length <= 4) " " else "") + time
                    navDrawerTimeHandler.postDelayed(this, 999)
                }
            })
        }
        hwKeyDelegate = null
        registerHwKeyActions(getHwKeyDelegate())
        // checks connection and state of initial command, if not ready, initiates countDownTimer
        if (connectionFailedTimer == null) {
            startConnectionFailTimer()
        }
        if (drawer != null && PreferencesEx.isFirstAppStart()) {
            PreferencesEx.persistFirstAppStart()
            Handler().postDelayed({ drawer?.controller?.openDrawer() }, 800)
        } else if (drawer != null && !ignoreNextDrawerPeek) {
            Handler().postDelayed({ drawer?.controller?.peekDrawer() }, 800)
        }
        ignoreNextDrawerPeek = false
    }

    override fun onPause() {
        state = WearActivityState.ON_PAUSE
        super.onPause()

        navDrawerTimeHandler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        state = WearActivityState.ON_STOP
        super.onStop()

        cancelConnectionFailedTimer()
    }

    override fun onDestroy() {
        state = WearActivityState.ON_DESTROY
        super.onDestroy()
    }

    /**
     * Flag is current activity is just a "child". This means whether this activity is allowed to run on top of other
     * locus wear activities (ie. doesn't call finish() on previous activity after this activity is resumed).
     */
    open val isChildLocusWearActivity: Boolean
        get() = false

    /**
     * Consumes new data coming from WearListenerService
     */
    open fun consumeNewData(path: DataPath, data: TimeStampStorable?) {
        //Logger.d(TAG, "consumeNewData($path, $data)")
        if (connectionFailedTimer != null) {
            when {
                path === DataPath.PUT_ON_CONNECTED_EVENT -> {
                    verifyConnection()
                }

                path === DataPath.TW_PUT_HAND_SHAKE -> {
                    isHandShakeReceived = true
                    verifyConnection()
                }

                path === initialCommandResponseType -> {
                    isInitialRequestReceived = true
                    verifyConnection()
                    onGotInitialCommandResponse()
                }
            }
        }
    }

    private fun getHwKeyDelegate(): LocusWearActivityHwKeyDelegate {
        if (hwKeyDelegate == null) {
            hwKeyDelegate = LocusWearActivityHwKeyDelegate.Factory.createDelegate(this)
        }
        return hwKeyDelegate!!
    }

    /**
     * Called by mConnectionFailedTimer tick,
     * checks state of initial handshaking initiated after on start.
     *
     * @return true if handshaking finished successfully
     */
    private fun verifyConnection(): Boolean {
        val wcs = WearCommService.instance
        logD {
            "verifyConnection(), " +
                    "API connected: ${wcs.isConnected}, " +
                    "isNodeConnected: $isNodeConnected, " +
                    "deviceAppInstalled: ${wcs.isAppInstalledOnDevice}"
        }

        // check connection to Google API
        if (!wcs.isConnected) {
            wcs.reconnectIfNeeded()
            return false
        }

        // check connection to phone
        if (!isNodeConnected) {
            logD { "verifyConnection(), node not connected" }
            if (!getConnectedNodesSent.getAndSet(true)) {
                wcs.getConnectedNodes { result: GetConnectedNodesResult ->
                    for (node in result.nodes) {
                        Logger.d(TAG, "  testing node $node, nearby: ${node.isNearby}")
                        if (node.isNearby) {
                            isNodeConnected = true
                            break
                        }
                    }

                    // check connection
                    Logger.d(TAG, "  all nodes tested, connected: $isNodeConnected")
                    if (isNodeConnected) {
                        verifyConnection()
                    } else {
                        cancelConnectionFailedTimer()
                        (application as MainApplication).doApplicationFail(AppFailType.CONNECTION_ERROR_NODE_NOT_CONNECTED)
                    }
                }
            }
            return false
        }

        // check if phone companion app is installed
        if (wcs.isAppInstalledOnDevice != TriStateLogicEnum.TRUE) {
            // app is probably not installed on the device
            if (wcs.isAppInstalledOnDevice == TriStateLogicEnum.FALSE) {
                mainApplication.doApplicationFail(AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE)
            }
            return false
        }

        // in approx. half of timeout resent requests one more time
        if (ticks.toInt() == HANDSHAKE_TIMEOUT_MS / 2 / HANDSHAKE_TICK_MS
            && !handshakeRetrySent.getAndSet(true)
        ) {
            Logger.d(TAG, "verifyConnection(), attempting second handshake")
            if (!isHandShakeReceived) {
                wcs.sendCommand(DataPath.TD_GET_HAND_SHAKE)
            }
            if (!isInitialRequestReceived) {
                val p = initialCommandType
                if (p != null) {
                    wcs.sendDataItem(p.path, p.storable)
                }
            }
        }

        // handle first tick - send handshake and initial command request
        if (!handshakeSent.getAndSet(true) && isMakeHandshakeOnStart) {
            wcs.sendCommand(DataPath.TD_GET_HAND_SHAKE)
            val p = initialCommandType
            if (p != null) {
                wcs.sendDataItem(p.path, p.storable)
            } else {
                isInitialRequestReceived = true
            }
        }
        val result = !isMakeHandshakeOnStart
                || isHandShakeReceived && isInitialRequestReceived
        Logger.d(
            TAG,
            "verifyConnection(), result: $result, " +
                    "isMakeHandshakeOnStart: $isMakeHandshakeOnStart, " +
                    "isHandShakeReceived: $isHandShakeReceived, " +
                    "isInitialRequestReceived: $isInitialRequestReceived"
        )
        if (result) {
            cancelConnectionFailedTimer()
            onHandShakeFinished()
        }
        return result
    }

    /**
     * Called after successful initial communication of the activity
     */
    protected open fun onHandShakeFinished() {}

    private fun startConnectionFailTimer() {
        if (connectionFailedTimer != null
            || state == WearActivityState.ON_STOP
            || state == WearActivityState.ON_DESTROY
        ) {
            return
        }
        ticks = 0
        isHandShakeReceived = false
        isInitialRequestReceived = false
        getConnectedNodesSent.set(false)
        handshakeSent.set(false)
        handshakeRetrySent.set(false)

        synchronized(connectionTimerLock) {
            if (connectionFailedTimer != null
                || state == WearActivityState.ON_STOP
                || state == WearActivityState.ON_DESTROY
            ) {
                return
            }

            connectionFailedTimer = object : CountDownTimer(HANDSHAKE_TIMEOUT_MS.toLong(), HANDSHAKE_TICK_MS.toLong()) {

                override fun onTick(l: Long) {
                    ticks++
                    verifyConnection()
                }

                override fun onFinish() {
                    Logger.e(Exception(), "Connection Failed!")
                    cancelConnectionFailedTimer()
                    // could not establish handshake connection
                    (application as MainApplication).doApplicationFail(
                        AppFailType.CONNECTION_FAILED
                    )
                }
            }
            connectionFailedTimer?.start()
        }
    }

    protected fun cancelConnectionFailedTimer() {
        synchronized(connectionTimerLock) {
            connectionFailedTimer?.cancel()
            connectionFailedTimer = null // and canceling and nulling timer
        }
    }

    // current activity state
    enum class WearActivityState {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY
    }

    /**
     * Handling of item click in main menu/navigation drawer
     */
    fun handleNavigationDrawerItemClicked(v: View) {
        val activityToStart = when (v.id) {
            R.id.navigation_drawer_item_map -> MapActivity::class.java
            R.id.navigation_drawer_item_track_rec -> TrackRecordActivity::class.java
            R.id.btn_settings -> MainSettingsActivity::class.java
            else -> null
        }
        startLocusWearActivity(activityToStart)
    }

    protected fun startLocusWearActivity(activityToStart: Class<out LocusWearActivity>?) {
        drawer?.controller?.closeDrawer()
        if (activityToStart == null
            || this.javaClass.simpleName == activityToStart.simpleName
        ) {
            return
        }
        PreferencesEx.lastActivity = activityToStart
        startActivity(Intent(this, activityToStart))
    }

    /**
     * Activities that use request for periodic data ie. Map or active track recording should
     * override this method and return true.
     *
     * Used when transitioning between activities to automatically disable any currently
     * receiving periodic data if no activity or activity without periodic data use is shown.
     */
    open val isUsePeriodicData: Boolean
        get() = false

    /**
     * Overriding and returning false means that all handshaking and even initial command request
     * response are skipped!
     *
     * @return if should do initialization handshake and init communication
     */
    protected open val isMakeHandshakeOnStart: Boolean
        get() = true

    val mainApplication: MainApplication
        get() = application as MainApplication

    // HARDWARE KEYS

    /**
     * This function can be overridden in inheriting activities and is called during each on resume.
     * Activity that wishes to use custom HW button handling should register appropriate listeners
     * in the provided [delegate]
     */
    open fun registerHwKeyActions(delegate: LocusWearActivityHwKeyDelegate) {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (getHwKeyDelegate().onKeyDown(
                keyCode,
                event
            )
        ) true else super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return if (getHwKeyDelegate().onKeyLongPress(
                keyCode,
                event
            )
        ) true else super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return if (getHwKeyDelegate().onKeyUp(keyCode, event)) true else super.onKeyUp(
            keyCode,
            event
        )
    }

    protected fun setIgnoreNextDrawerPeek() {
        ignoreNextDrawerPeek = true
    }

    // AMBIENT SUPPORT

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                this@LocusWearActivity.onEnterAmbient(ambientDetails)
            }

            override fun onExitAmbient() {
                this@LocusWearActivity.onExitAmbient()
            }
        }
    }

    open fun onEnterAmbient(ambientDetails: Bundle?) {
        drawer?.let {
            it.setBackgroundColor(Color.BLACK)
            drawerCloseArrowImg?.setBackgroundColor(Color.BLACK)
        }
    }

    open fun onExitAmbient() {
        drawer?.let {
            it.setBackgroundColor(getColor(R.color.base_primary))
            drawerCloseArrowImg?.setBackgroundColor(getColor(R.color.nav_drawer_dark_background))
        }
    }

    companion object {

        private const val TAG = "LocusWearActivity"
        private val IS_DEBUG_MODE_ENABLED = BuildConfig.DEBUG
        private const val HANDSHAKE_TIMEOUT_MS = 8000
        private const val HANDSHAKE_TICK_MS = 400
    }
}