package com.asamm.locus.addon.wear.gui.fragments;

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

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.gui.ProfileListActivity;
import com.asamm.locus.addon.wear.gui.TrackRecordActivity;

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
	private static final String ARG_ICON = "ARG_ICON";
	private static final int PICK_PROFILE_REQUEST = 917001;

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

			arr = b.getByteArray(ARG_ICON);
			try {
				mIcon = arr == null ? null : new TrackProfileIconValue(arr);
			} catch (IOException e) {
				mIcon = null;
			}
		}
	}

	public void setParameters(TrackProfileInfoValue profile, TrackProfileIconValue icon) {
		mProfile = profile;
		mIcon = icon;
		refreshModel();
	}

	public void handleOpenProfileListActivityClick(View v) {
		if (getActivity() instanceof TrackRecordActivity) {
			TrackRecordActivity a = (TrackRecordActivity) getActivity();
			Intent i = new Intent(a, ProfileListActivity.class);
			TrackProfileInfoValue.ValueList profiles = a.getProfileList();
			TrackProfileIconValue.ValueList icons = a.getProfileIcons();
			Bundle b = new Bundle();
			b.putByteArray(ProfileListActivity.ARG_PROFILES, profiles.getAsBytes());
			b.putByteArray(ProfileListActivity.ARG_ICONS, icons.getAsBytes());
			i.putExtras(b);
			startActivityForResult(i, PICK_PROFILE_REQUEST);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PICK_PROFILE_REQUEST && resultCode == Activity.RESULT_OK) {
			byte[] profileBytes = data.getByteArrayExtra(ARG_PROFILE);
			byte[] iconBytes = data.getByteArrayExtra(ARG_ICON);
			try {
				mProfile = new TrackProfileInfoValue(profileBytes);
			} catch (IOException e) {
				Logger.logE("TAG", "empty profile bytes", e);
				return;
			}
			try {
				mIcon = new TrackProfileIconValue(iconBytes);
			} catch (IOException e) {
				// do nothing, icon is nullable so this exception is OK
			}
			refreshModel();
		}
	}

	private void refreshModel() {
		if (mTextProfileName == null) {
			return;
		}
		mTextProfileName.setText(mProfile == null ? "" : mProfile.getName());
		if (mIcon == null) {
			mImageProfileIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.xxx_load, null));
		} else {
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
		outState.putByteArray(ARG_ICON, mIcon.getAsBytes());
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

	public TrackProfileInfoValue getmProfile() {
		return mProfile;
	}

	public TrackProfileIconValue getmIcon() {
		return mIcon;
	}
}
