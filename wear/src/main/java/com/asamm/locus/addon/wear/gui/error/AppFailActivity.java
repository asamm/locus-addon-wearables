package com.asamm.locus.addon.wear.gui.error;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.phone.PhoneDeviceType;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.Const;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.google.android.wearable.intent.RemoteIntent;

/**
 * Activity for showing application failure error message with button to try to restart last activity
 * <p>
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */

public class AppFailActivity extends WearableActivity {

	public static final String ARG_ERROR_TYPE = "AppFailActivity.ARG_ERROR_TYPE";
	private AppFailType mFailType;
	private Button mInstallButton;
	private ImageView mRetryButton;
	private TextView mTvErrMsg;

	private boolean mInstallReqResultReceived;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fail);
		String type = getIntent().getExtras().getString(ARG_ERROR_TYPE);
		mFailType = AppFailType.valueOf(type);

		TextView tvMsg = findViewById(R.id.fail_msg);
		tvMsg.setText(getText(mFailType.getErrorMsgId()));
		TextView tvHeader = findViewById(R.id.text_view_screen_header);
		tvHeader.setText(getText(R.string.title_activity_error));
		mInstallButton = findViewById(R.id.fail_install_button);
		mRetryButton = findViewById(R.id.fail_img_retry);
		mTvErrMsg = findViewById(R.id.fail_msg);

		refresh();
	}

	public void onInstallClicked(View v) {
		int phoneDeviceType = PhoneDeviceType.getPhoneDeviceType(getApplicationContext());
		// Only target AW2.0+ since AW 1.x user should not see this at all and should reinstall manually
		if (Build.VERSION.SDK_INT >= 25 && phoneDeviceType == PhoneDeviceType.DEVICE_TYPE_ANDROID) {
			// Create Remote Intent to open Play Store listing of app on remote device.
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
									mTvErrMsg.setText(R.string.continue_installation);
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
		Class<? extends LocusWearActivity> c = ((MainApplication) getApplication()).getLastAppTask();
		Intent i = new Intent(this, c);
		startActivity(i);
		finish();
	}

	private boolean isNotInstalledError() {
		return (mFailType == AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE);
	}

	private void refresh() {
		if (isNotInstalledError()) {
			mInstallButton.setVisibility(mInstallReqResultReceived ? View.GONE : View.VISIBLE);
			mRetryButton.setVisibility(!mInstallReqResultReceived ? View.GONE : View.VISIBLE);
		}
	}
}
