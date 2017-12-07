package com.asamm.locus.addon.wear.gui.error;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.phone.PhoneDeviceType;
import android.support.wearable.view.ConfirmationOverlay;
import android.view.View;
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
	}

	public void onInstallCliecked(View v) {
		int phoneDeviceType = PhoneDeviceType.getPhoneDeviceType(getApplicationContext());
		if (phoneDeviceType == PhoneDeviceType.DEVICE_TYPE_ANDROID) {
			// Paired to Android phone, use Play Store URI.

			// Create Remote Intent to open Play Store listing of app on remote device.
			Intent intentAndroid =
					new Intent(Intent.ACTION_VIEW)
							.addCategory(Intent.CATEGORY_BROWSABLE)
							.setData(Uri.parse(Const.ANDROID_MARKET_APP_URI));

			RemoteIntent.startRemoteActivity(
					getApplicationContext(),
					intentAndroid,
					new ResultReceiver(new Handler()) {
						@Override
						protected void onReceiveResult(int resultCode, Bundle resultData) {

							if (resultCode == RemoteIntent.RESULT_OK) {
								new ConfirmationOverlay().showOn(AppFailActivity.this);

							} else if (resultCode == RemoteIntent.RESULT_FAILED) {
								new ConfirmationOverlay()
										.setType(ConfirmationOverlay.FAILURE_ANIMATION)
										.showOn(AppFailActivity.this);

							} else {
								throw new IllegalStateException("Unexpected result " + resultCode);
							}
						}
					});

		} else {
			Toast.makeText(this, getText(R.string.toast_err_device_not_supported),
					Toast.LENGTH_LONG).show();
		}

	}

	public void onRetryClicked(View v) {
		if (mFailType == AppFailType.CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE) {
			onInstallCliecked(v);
			return;
		}
		Class<? extends LocusWearActivity> c = ((MainApplication) getApplication()).getLastAppTask();
		Intent i = new Intent(this, c);
		startActivity(i);
		finish();
	}
}
