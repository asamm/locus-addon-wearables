package com.asamm.locus.addon.wear.features.error;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.wearable.phone.PhoneDeviceType;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.application.PreferencesEx;
import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.google.android.wearable.intent.RemoteIntent;

import androidx.fragment.app.FragmentActivity;

/**
 * Activity for showing application failure error message with button to try to restart last activity
 * <p>
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */

public class AppFailActivity extends FragmentActivity {

    public static final String ARG_ERROR_TYPE = "AppFailActivity.ARG_ERROR_TYPE";

    private AppFailType failType;
    private Button installButton;
    private ImageView retryButton;
    private TextView tvErrMsg;

    private boolean mInstallReqResultReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fail);
        String type = getIntent().getExtras().getString(ARG_ERROR_TYPE);
        failType = AppFailType.valueOf(type);
        tvErrMsg = findViewById(R.id.fail_msg);
        tvErrMsg.setText(getText(failType.getErrorMsgId()));
        TextView tvHeader = findViewById(R.id.text_view_screen_header);
        if (failType == AppFailType.CONNECTION_FAILED
                || failType == AppFailType.CONNECTION_ERROR_NODE_NOT_CONNECTED) {
            tvHeader.setText(getText(R.string.title_activity_error));
        } else {
            tvHeader.setText(getText(R.string.app_name));
        }
        installButton = findViewById(R.id.fail_install_button);
        retryButton = findViewById(R.id.fail_img_retry);

        refresh();
    }

    public void onInstallClicked(View v) {
        int phoneDeviceType = PhoneDeviceType.getPhoneDeviceType(getApplicationContext());
        // Only target AW2.0+ since AW 1.x user should not see this at all and should reinstall manually
        if (Build.VERSION.SDK_INT >= 25
                && phoneDeviceType == PhoneDeviceType.DEVICE_TYPE_ANDROID) {

            if (isWatchAppOutdated()) {
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                finish();
                return;
            }

            // create Remote Intent to open Play Store listing of app on remote device.
            Intent intentAndroid = new Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(Const.ANDROID_MARKET_APP_URI));
            RemoteIntent.startRemoteActivity(getApplicationContext(), intentAndroid,
                    new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            mInstallReqResultReceived = true;
                            final boolean isResultOk;
                            if (resultCode == RemoteIntent.RESULT_OK) {
                                isResultOk = true;
                                new ConfirmationOverlay()
                                        .setType(ConfirmationOverlay.OPEN_ON_PHONE_ANIMATION)
                                        .showOn(AppFailActivity.this);
                            } else if (resultCode == RemoteIntent.RESULT_FAILED) {
                                isResultOk = false;
                                new ConfirmationOverlay()
                                        .setType(ConfirmationOverlay.FAILURE_ANIMATION)
                                        .showOn(AppFailActivity.this);
                            } else {
                                throw new IllegalStateException("Unexpected result " + resultCode);
                            }
                            runOnUiThread(() -> {
                                if (isResultOk) {
                                    tvErrMsg.setText(R.string.continue_installation);
                                }
                                refresh();
                            });
                        }
                    });
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, getText(R.string.toast_err_device_not_supported),
                        Toast.LENGTH_LONG).show();
                // mark as successful to show retry button as install button will not work anyway
                mInstallReqResultReceived = true;
                refresh();
            });
        }
    }

    public void onRetryClicked(View v) {
        Class<? extends LocusWearActivity> c = PreferencesEx.INSTANCE.getLastActivity();
        Intent i = new Intent(this, c);
        startActivity(i);
        finish();
    }

    private boolean isDeviceAppNotInstalled() {
        return failType == AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE;
    }

    private boolean isDeviceAppOutdated() {
        return failType == AppFailType.CONNECTION_ERROR_DEVICE_APP_OUTDATED;
    }

    private boolean isWatchAppOutdated() {
        return failType == AppFailType.CONNECTION_ERROR_WATCH_APP_OUTDATED;
    }

    private void refresh() {
        if (isDeviceAppNotInstalled() || isDeviceAppOutdated() || isWatchAppOutdated()) {
            installButton.setText(getText(isDeviceAppNotInstalled() ? R.string.install : R.string.update));
            installButton.setVisibility(mInstallReqResultReceived ? View.GONE : View.VISIBLE);
            retryButton.setVisibility(!mInstallReqResultReceived ? View.GONE : View.VISIBLE);
        }
    }
}
