package com.asamm.locus.addon.wear.gui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.assam.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;

import java.io.IOException;

import locus.api.android.utils.UtilsBitmap;

/**
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordProfileSelectFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PROFILE = "ARG_PROFILE";
    private static final String ARG_ICON = "ARG_ICON";

    private TrackProfileInfoValue mProfile;
    private TrackProfileIconValue mIcon;

    private TextView mTextProfileName;
    private ImageView mImageSelectIcon;
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
        handleModelChanged();
    }

    private void handleModelChanged() {
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
        return inflater.inflate(R.layout.track_record_profile_select_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        handleModelChanged();
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
