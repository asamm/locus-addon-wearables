package com.asamm.locus.addon.wear.gui.trackrec.profiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.AppStorageManager;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;

import java.io.IOException;
import java.util.ArrayList;

import locus.api.android.utils.UtilsBitmap;
import locus.api.utils.Logger;

/**
 * Activity containing single recycler view for choosing recording profile.
 * Created by Milan Cejnar
 * Asamm Software, s.r.o.
 */
public class ProfileListActivity extends LocusWearActivity {

	private static final String TAG = "ProfileListActivity";
	public static final String ARG_PROFILES = "ARG_PROFILES";
	public static final String ARG_SELECTED_PROFILE_NAME = "ARG_SELECTED_PROFILE_NAME";
	public static final String RESULT_PROFILES = "RESULT_PROFILES";

	private WearableRecyclerView mRecyclerView;
	private ProfileListAdapter mAdapter;

	/**
	 * list of available recording profiles
	 */

	private TrackProfileInfoValue.ValueList mProfiles;
	/**
	 * Name of currently selected profile from parent activity
	 */
	private String mSelectedProfileName;

	@Override
	protected DataPayload<EmptyCommand> getInitialCommandType() {
		return null;
	}

	@Override
	protected DataPath getInitialCommandResponseType() {
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_list);
		TextView header = findViewById(R.id.text_view_screen_header);
		if (header != null) {
			header.setText(getText(R.string.title_activity_profile_list));
		}
		mRecyclerView = findViewById(R.id.profile_list);
		mRecyclerView.setEdgeItemsCenteringEnabled(true);
		mRecyclerView.setLayoutManager(
				new WearableLinearLayoutManager(this));
		mRecyclerView.setHasFixedSize(true);

		mSelectedProfileName = getIntent().getExtras().getString(ARG_SELECTED_PROFILE_NAME, "");
		byte[] arr = getIntent().getExtras().getByteArray(ARG_PROFILES);
		if (arr != null && arr.length > 0) {
			try {
				mProfiles = new TrackProfileInfoValue.ValueList(arr);
			} catch (IOException e) {
				Logger.logE(TAG, "profile info constructor failed", e);
				finish();
			}
		}

		mAdapter = new ProfileListAdapter(mProfiles.getStorables().toArray(new TrackProfileInfoValue[0]));
		mRecyclerView.setAdapter(mAdapter);

		// Enables Always-on
		//setAmbientEnabled();
	}

	@Override
	public void consumeNewData(DataPath path, TimeStampStorable data) {
		super.consumeNewData(path, data);
		switch (path) {
			case PUT_PROFILE_ICON:
				final TrackProfileIconValue icon = (TrackProfileIconValue) data;
				runOnUiThread(() -> {
					mAdapter.refreshDataModel();
					mAdapter.notifyDataSetChanged();
				});
				break;
		}
	}

	/**
	 * @return false for this activity, no handshaking required
	 */
	@Override
	protected boolean isMakeHandshakeOnStart() {
		return false;
	}

	@Override
	public boolean isChildLocusWearActivity() {
		return true; // runs on top of TrackRecording activity, not independetly
	}

	@Override
	public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {

	}

	private class TrackProfileModelHolder {
		final long mId;
		final TrackProfileInfoValue mProfileInfo;
		private TrackProfileIconValue mProfileIcon;

		TrackProfileModelHolder(long id, TrackProfileInfoValue info, TrackProfileIconValue icon) {
			this.mId = id;
			this.mProfileInfo = info;
			if (icon == null) {
				icon = new TrackProfileIconValue(id, null);
			}
			this.mProfileIcon = icon;
		}

		void setIcon(byte[] img) {
			mProfileIcon.setIcon(img);
		}
	}

	private class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ViewHolder> {
		private final TrackProfileInfoValue[] mMyDataset;
		private volatile ArrayList<TrackProfileModelHolder> mModel;

		// Provide a suitable constructor (depends on the kind of dataset)
		private ProfileListAdapter(TrackProfileInfoValue[] myDataset) {
			if (myDataset == null || myDataset.length == 0) {
				throw new IllegalArgumentException("Got empty profile list!");
			}
			mMyDataset = myDataset;
			refreshDataModel();
		}

		private void refreshDataModel() {
			ArrayList<TrackProfileModelHolder> model = new ArrayList<>(mMyDataset.length);
			for (TrackProfileInfoValue v : mMyDataset) {
				TrackProfileIconValue icon = AppStorageManager.getIcon(ProfileListActivity.this, v.getId());
				model.add(new TrackProfileModelHolder(v.getId(), v, icon));
			}
			mModel = model;
		}

		private void onItemSelected(TrackProfileModelHolder selectedHolder) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra(RESULT_PROFILES, selectedHolder.mProfileInfo.getAsBytes());
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
		}

		// Create new views (invoked by the layout manager)
		@Override
		public ProfileListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
																int viewType) {
			View v = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.list_item_layout, parent, false);
			// set the view's size, margins, paddings and layout parameters
			ViewHolder vh = new ViewHolder(v);
			return vh;
		}

		// Replace the contents of a view (invoked by the layout manager)
		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			// - get element from your dataset at this position
			// - replace the contents of the view with that element
			final TrackProfileModelHolder value = mModel.get(position);

			View.OnClickListener clickHandler = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onItemSelected(value);
				}
			};
			holder.mTextViewName.setText(value.mProfileInfo.getName());
			if (value.mProfileIcon != null && value.mProfileIcon.getIcon() != null) {
				holder.mIcon.setImageBitmap(UtilsBitmap.getBitmap(value.mProfileIcon.getIcon()));
			}
			holder.mTextViewName.setOnClickListener(clickHandler);
			holder.mIcon.setOnClickListener(clickHandler);

			if (mSelectedProfileName.equals(value.mProfileInfo.getName())) {
				holder.mTextViewName.setTextColor(getColor(R.color.crimson));
			}
		}

		// Return the size of your dataset (invoked by the layout manager)
		@Override
		public int getItemCount() {
			return mModel.size();
		}

		/**
		 * View holder for this recycler view
		 */
		class ViewHolder extends RecyclerView.ViewHolder {
			// each data item is just a string in this case
			public final TextView mTextViewName;
			public final ImageView mIcon;

			public ViewHolder(View root) {
				super(root);
				mTextViewName = root.findViewById(R.id.profile_list_item_name);
				mIcon = root.findViewById(R.id.profile_list_item_image);
			}
		}
	}
}
