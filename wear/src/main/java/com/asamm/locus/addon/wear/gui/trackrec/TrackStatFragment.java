package com.asamm.locus.addon.wear.gui.trackrec;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackStatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackStatFragment extends Fragment {

	private static final String ARG_TYPE = "ARG_TYPE";
	private static final String ARG_ICON_BOTTOM = "ARG_ICON_BOTTOM";

	private TrackRecStatTypeEnum mType;
	private boolean mIsIconBottom;
	private TextView mTextViewValue;
	private ImageView mImageViewIcon;

	public TrackStatFragment() {
		// Required empty public constructor
	}

	public static TrackStatFragment newInstance(TrackRecStatTypeEnum type, boolean isIconBottom) {
		TrackStatFragment fragment = new TrackStatFragment();
		Bundle args = new Bundle();
		args.putString(ARG_TYPE, type.name());
		args.putBoolean(ARG_ICON_BOTTOM, isIconBottom);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mType = TrackRecStatTypeEnum.BLANK;
		if (getArguments() != null) {
			try {
				mType = TrackRecStatTypeEnum.valueOf(getArguments().getString(ARG_TYPE));
			} catch (Exception e) {
			}

			mIsIconBottom = getArguments().getBoolean(ARG_ICON_BOTTOM, false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(
				mIsIconBottom ? R.layout.fragment_track_stat_icon_bottom :
						R.layout.fragment_track_stat_icon_top,
				container, false);
		mImageViewIcon = v.findViewById(R.id.statIcon);
		mImageViewIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), mType.getIconId()));
		mTextViewValue = v.findViewById(R.id.statText);
		mTextViewValue.setText("-");
		return v;
	}

	public void consumeNewStatistics(TrackRecordingValue trv) {
		String newValue = mType.consumeAndFormat(getActivity(), trv.getTrackRecStats());
		mTextViewValue.setText(newValue);

	}

}
