package com.asamm.locus.addon.wear.gui.trackrec;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.AppStorageManager;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;

import locus.api.android.utils.UtilsBitmap;

/**
 * Component for displaying and selection of track recording profile
 *
 * Call to setProfileSelectCallback() required to supply implementation of selection handling
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordProfileSelectLayout extends ConstraintLayout {

	static final int PICK_PROFILE_REQUEST = 1;

	private TrackProfileInfoValue mProfile;
	private TrackProfileIconValue mIcon;

	private TextView mTextProfileName;
	private ImageView mImageProfileIcon;
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
		mTextProfileName = findViewById(R.id.track_profile_select_text);
		mImageProfileIcon = findViewById(R.id.track_profile_select_icon);
	}

	public void setParameters(TrackProfileInfoValue profile) {
		mProfile = profile;
		// empty icon, try icon cache for match
		mIcon = AppStorageManager.getIcon(getContext(), profile.getId());;
		refreshModel();
	}

	@Override
	public void setEnabled(boolean enabled) {
		enabled = enabled && hasProfileList();
		super.setEnabled(enabled);
		mTextProfileName.setEnabled(enabled);
	}

	private void refreshModel() {
		if (mTextProfileName == null) {
			return;
		}
		mTextProfileName.setText(mProfile == null ? "" : mProfile.getName());
		if (mIcon != null) {
			mImageProfileIcon.setImageBitmap(UtilsBitmap.getBitmap(mIcon.getIcon()));
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
		mTextProfileName.setOnClickListener(mProfileSelectCallback);
		mImageProfileIcon.setOnClickListener(mProfileSelectCallback);
	}

	public boolean hasProfileList() {
		return mProfileList != null;
	}

	public void setPlaceHolder(CharSequence s) {
		mTextProfileName.setHint(s);
	}
}
