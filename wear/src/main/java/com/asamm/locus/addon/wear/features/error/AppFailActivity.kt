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
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.wear.phone.interactions.PhoneTypeHelper
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.ConfirmationOverlay
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

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
        findViewById<TextView>(R.id.text_view_screen_header)
                .text = getText(failType.headerTitle)
        tvErrMsg = findViewById(R.id.fail_msg)
        tvErrMsg.text = getText(failType.errorMsgId)
        installButton = findViewById(R.id.fail_install_button)
        retryButton = findViewById(R.id.fail_img_retry)
        refresh()
    }

    /**
     * Handle click on the 'install' button.
     */
    @Suppress("UNUSED_PARAMETER")
    fun onInstallClicked(view: View) {
        val phoneDeviceType = PhoneTypeHelper.getPhoneDeviceType(applicationContext)
        // Only target AW2.0+ since AW 1.x user should not see this at all and should reinstall manually
        if (phoneDeviceType != PhoneTypeHelper.DEVICE_TYPE_ANDROID) {
            runOnUiThread {
                Toast.makeText(this, getText(R.string.toast_err_device_not_supported), Toast.LENGTH_LONG)
                        .show()

                // mark as successful to show retry button as install button will not work anyway
                installReqResultReceived = true
                refresh()
            }
            return
        }

        // handle outdated Watch app
        if (failType === AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED) {
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
        val appUri = if (failType == AppFailType.UNSUPPORTED_LOCUS_VERSION) {
            Const.ANDROID_MARKET_LOCUS_MAP_URI
        } else {
            Const.ANDROID_MARKET_APP_URI
        }
        requestInstallOnDevice(appUri)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRetryClicked(view: View) {
        startActivity(Intent(this, PreferencesEx.lastActivity))
        finish()
    }

    private fun refresh() {
        when (failType) {
            AppFailType.UNSUPPORTED_LOCUS_VERSION,
            AppFailType.CONNECTION_ERROR_DEVICE_APP_OUTDATED,
            AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED -> {
                installButton.text =
                        getText(R.string.update)
                installButton.visibility =
                        if (installReqResultReceived) View.GONE else View.VISIBLE
                retryButton.visibility =
                        if (!installReqResultReceived) View.GONE else View.VISIBLE
            }
            AppFailType.CONNECTION_FAILED -> {
                // nothing to do
            }
            AppFailType.CONNECTION_ERROR_NODE_NOT_CONNECTED -> {
                // nothing to do
            }
            AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE -> {
                installButton.text =
                        getText(R.string.install)
                installButton.visibility =
                        if (installReqResultReceived) View.GONE else View.VISIBLE
                retryButton.visibility =
                        if (!installReqResultReceived) View.GONE else View.VISIBLE
            }
        }
    }

    /**
     * Request install of certain app (defined by [uri] on the phone.
     */
    private fun requestInstallOnDevice(uri: String) {
        // prepare request
        val intentAndroid = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(uri))
        val remoteActivityHelper = RemoteActivityHelper(this)
        val remoteRequest = remoteActivityHelper.startRemoteActivity(intentAndroid, null)
        Futures.addCallback(remoteRequest, object : FutureCallback<Void> {

            override fun onSuccess(result: Void?) {
                installReqResultReceived = true
                tvErrMsg.setText(R.string.continue_installation)
                ConfirmationOverlay()
                        .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                        .showOn(this@AppFailActivity)
            }

            override fun onFailure(t: Throwable) {
                installReqResultReceived = true
                ConfirmationOverlay()
                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                        .showOn(this@AppFailActivity)
            }
        }, HandlerExecutor(Looper.getMainLooper()))
    }

    companion object {

        const val ARG_ERROR_TYPE = "AppFailActivity.ARG_ERROR_TYPE"
    }
}