package com.asamm.locus.addon.wear.gui.trackrec.profiles;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.asamm.locus.addon.wear.AppStorageManager;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;

import locus.api.android.utils.UtilsBitmap;

/**
 * Component for displaying and selection of track recording profile
 * <p>
 * Call to setProfileSelectCallback() required to supply implementation of selection handling
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordProfileSelectLayout extends ConstraintLayout {

	public static final int PICK_PROFILE_REQUEST = 1;

	private TrackProfileInfoValue mProfile;
	private TrackProfileIconValue mIcon;

	private Button mbtnOpenProfile;
	private volatile TrackProfileInfoValue.ValueList mProfileList;
	private View.OnClickListener mProfileSelectCallback;


	public TrackRecordProfileSelectLayout(Context context) {
		this(context, null);
	}

	public TrackRecordProfileSelectLayout(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TrackRecordProfileSelectLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView(context, attrs);
	}

	private void initView(Context ctx, AttributeSet attrs) {
		View.inflate(ctx, R.layout.track_record_profile_select_content, this);
		mbtnOpenProfile = findViewById(R.id.btnOpenProfile);
	}

	public void setParameters(TrackProfileInfoValue profile) {
		mProfile = profile;
		// empty icon, try icon cache for match
		mIcon = AppStorageManager.getIcon(getContext(), profile.getId());
		refreshModel();
	}

	@Override
	public void setEnabled(boolean enabled) {
		enabled = enabled && hasProfileList();
		super.setEnabled(enabled);
		mbtnOpenProfile.setEnabled(enabled);
	}

	private void refreshModel() {
		if (mbtnOpenProfile == null) {
			return;
		}
		mbtnOpenProfile.setText(mProfile == null ? "" : mProfile.getName());
		if (mIcon != null) {
			Drawable icon = new BitmapDrawable(getResources(), UtilsBitmap.getBitmap(mIcon.getIcon()));
			float density = getContext().getResources().getDisplayMetrics().density;
			int dp28 = (int)(28 * density + 0.5f);
			icon.setBounds(0,0,dp28, dp28);

			Drawable arrow = ContextCompat.getDrawable(getContext(), R.drawable.ic_arrow_basic_down);
			int dp16 = (int)(16 * density + 0.5f);
			arrow.setBounds(0,0,dp16, dp16);
			mbtnOpenProfile.setCompoundDrawables( icon,null, arrow,null);
		}
	}

	public TrackProfileInfoValue getProfile() {
		return mProfile;
	}

	public TrackProfileIconValue getIcon() {
		return mIcon;
	}

	public void setProfileList(TrackProfileInfoValue.ValueList profileList) {
		this.mProfileList = profileList;
	}

	public TrackProfileInfoValue.ValueList getProfileList() {
		return mProfileList;
	}

	public void setProfileSelectCallback(View.OnClickListener profileSelectCallback) {
		mProfileSelectCallback = profileSelectCallback;
		mbtnOpenProfile.setOnClickListener(mProfileSelectCallback);
	}

	public boolean hasProfileList() {
		return mProfileList != null;
	}

	public void setPlaceHolder(CharSequence s) {
		mbtnOpenProfile.setHint(s);
	}
}
