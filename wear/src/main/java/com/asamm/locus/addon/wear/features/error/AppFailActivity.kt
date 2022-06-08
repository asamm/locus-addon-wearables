/**
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.features.error

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.wearable.phone.PhoneDeviceType
import android.support.wearable.view.ConfirmationOverlay
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.google.android.wearable.intent.RemoteIntent

/**
 * Activity for showing application failure error message with button to try to restart last activity
 */
class AppFailActivity : FragmentActivity() {

    private lateinit var failType: AppFailType

    private lateinit var installButton: Button

    private lateinit var retryButton: ImageView

    private lateinit var tvErrMsg: TextView

    private var installReqResultReceived = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fail)
        failType = AppFailType.valueOf(intent.extras!!.getString(ARG_ERROR_TYPE)!!)

        // setup UI
        tvErrMsg = findViewById(R.id.fail_msg)
        tvErrMsg.text = getText(failType.errorMsgId)
        val tvHeader = findViewById<TextView>(R.id.text_view_screen_header)
        if (failType === AppFailType.CONNECTION_FAILED
                || failType === AppFailType.CONNECTION_ERROR_NODE_NOT_CONNECTED) {
            tvHeader.text = getText(R.string.title_activity_error)
        } else {
            tvHeader.text = getText(R.string.app_name)
        }
        installButton = findViewById(R.id.fail_install_button)
        retryButton = findViewById(R.id.fail_img_retry)
        refresh()
    }

    /**
     * Handle click on the 'install' button.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onInstallClicked(view: View) {
        val phoneDeviceType = PhoneDeviceType.getPhoneDeviceType(applicationContext)
        // Only target AW2.0+ since AW 1.x user should not see this at all and should reinstall manually
        if (Build.VERSION.SDK_INT >= 25
                && phoneDeviceType == PhoneDeviceType.DEVICE_TYPE_ANDROID) {
            if (isWatchAppOutdated) {
                val appPackageName = packageName
                try {
                    startActivity(
                            Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=$appPackageName")
                            )
                    )
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                            Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                            )
                    )
                }
                finish()
                return
            }

            // create Remote Intent to open Play Store listing of app on remote device.
            val intentAndroid = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(Const.ANDROID_MARKET_APP_URI))
            RemoteIntent.startRemoteActivity(
                    applicationContext, intentAndroid,
                    object : ResultReceiver(Handler()) {

                        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                            installReqResultReceived = true
                            val isResultOk: Boolean
                            when (resultCode) {
                                RemoteIntent.RESULT_OK -> {
                                    isResultOk = true
                                    ConfirmationOverlay()
                                            .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                            .showOn(this@AppFailActivity)
                                }
                                RemoteIntent.RESULT_FAILED -> {
                                    isResultOk = false
                                    ConfirmationOverlay()
                                            .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                                            .showOn(this@AppFailActivity)
                                }
                                else -> {
                                    throw IllegalStateException("Unexpected result $resultCode")
                                }
                            }
                            runOnUiThread {
                                if (isResultOk) {
                                    tvErrMsg.setText(R.string.continue_installation)
                                }
                                refresh()
                            }
                        }
                    })
        } else {
            runOnUiThread {
                Toast.makeText(
                        this, getText(R.string.toast_err_device_not_supported),
                        Toast.LENGTH_LONG
                ).show()

                // mark as successful to show retry button as install button will not work anyway
                installReqResultReceived = true
                refresh()
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRetryClicked(view: View) {
        startActivity(Intent(this, PreferencesEx.lastActivity))
        finish()
    }

    private val isDeviceAppNotInstalled: Boolean
        get() = failType === AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE

    private val isDeviceAppOutdated: Boolean
        get() = failType === AppFailType.CONNECTION_ERROR_DEVICE_APP_OUTDATED

    private val isWatchAppOutdated: Boolean
        get() = failType === AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED

    private fun refresh() {
        if (isDeviceAppNotInstalled || isDeviceAppOutdated || isWatchAppOutdated) {
            installButton.text =
                    getText(if (isDeviceAppNotInstalled) R.string.install else R.string.update)
            installButton.visibility =
                    if (installReqResultReceived) View.GONE else View.VISIBLE
            retryButton.visibility =
                    if (!installReqResultReceived) View.GONE else View.VISIBLE
        }
    }

    companion object {

        const val ARG_ERROR_TYPE = "AppFailActivity.ARG_ERROR_TYPE"
    }
}