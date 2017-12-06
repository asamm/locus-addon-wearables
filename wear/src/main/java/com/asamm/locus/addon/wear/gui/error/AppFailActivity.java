package com.asamm.locus.addon.wear.gui.error;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

/**
 * Activity for showing application failure error message with button to try to restart last activity
 *
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

	public void onRetryClicked(View v) {
		Class<? extends LocusWearActivity> c = ((MainApplication) getApplication()).getLastAppTask();
		Intent i = new Intent(this, c);
		startActivity(i);
		finish();
	}
}
