package com.asamm.locus.addon.wear.gui.trackrec;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.AppPreferencesManager;
import com.asamm.locus.addon.wear.AppStorageManager;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.gui.trackrec.ProfileListActivity;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecordActivity;

import java.io.IOException;

import locus.api.android.utils.UtilsBitmap;
import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordProfileSelectFragment extends Fragment {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PROFILE = "ARG_PROFILE";

	private TrackProfileInfoValue mProfile;
	private TrackProfileIconValue mIcon;

	private TextView mTextProfileName;
	private ImageView mImageProfileIcon;

	public TrackRecordProfileSelectFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = savedInstanceState != null ? savedInstanceState : getArguments();
		if (b != null) {
			byte[] arr = b.getByteArray(ARG_PROFILE);
			try {
				mProfile = arr == null ? null : new TrackProfileInfoValue(arr);
			} catch (IOException e) {
				mProfile = null;
			}
		}
	}

	public void setParameters(TrackProfileInfoValue profile, TrackProfileIconValue icon) {
		mProfile = profile;
		// empty icon, try icon cache for match
		if (icon == null && profile != null) {
			icon = AppStorageManager.getIcon(getActivity(), profile.getId());
		}
		mIcon = icon;
		refreshModel();
	}

	public void handleOpenProfileListActivityClick(View v) {
		if (getActivity() instanceof TrackRecordActivity) {
			TrackRecordActivity a = (TrackRecordActivity) getActivity();
			Intent i = new Intent(a, ProfileListActivity.class);
			TrackProfileInfoValue.ValueList profiles = a.getProfileList();
			Bundle b = new Bundle();
			b.putByteArray(ProfileListActivity.ARG_PROFILES, profiles.getAsBytes());
			i.putExtras(b);
			startActivityForResult(i, PICK_PROFILE_REQUEST);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_PROFILE_REQUEST && resultCode == Activity.RESULT_OK) {
			byte[] profileBytes = data.getByteArrayExtra(ARG_PROFILE);
			try {
				setParameters(new TrackProfileInfoValue(profileBytes), null);
			} catch (IOException e) {
				Logger.logE("TAG", "empty profile bytes", e);

			}
		}
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.track_record_profile_select_fragment, container, false);
		mTextProfileName = v.findViewById(R.id.trackProfileSelectText);
		mImageProfileIcon = v.findViewById(R.id.trackProfileSelectIcon);
		View.OnClickListener onClick = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				handleOpenProfileListActivityClick(view);
			}
		};
		mTextProfileName.setOnClickListener(onClick);
		mImageProfileIcon.setOnClickListener(onClick);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		refreshModel();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putByteArray(ARG_PROFILE, mProfile.getAsBytes());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	public TrackProfileInfoValue getProfile() {
		return mProfile;
	}

	public TrackProfileIconValue getIcon() {
		return mIcon;
	}
}
