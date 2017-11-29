package com.asamm.locus.addon.wear.gui.trackrec;

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
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;

import java.io.IOException;
import java.util.HashMap;

import locus.api.android.utils.UtilsBitmap;
import locus.api.utils.Logger;

public class ProfileListActivity extends LocusWearActivity {

	private static final String TAG = "ProfileListActivity";
	public static final String ARG_PROFILES = "ARG_PROFILES";
	public static final String RESULT_PROFILES = "RESULT_PROFILES";


	private WearableRecyclerView mRecyclerVeiw;
	private WearableRecyclerView.Adapter adapter;

	private volatile TrackProfileInfoValue.ValueList profiles;

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
		mRecyclerVeiw = findViewById(R.id.profile_list);
		mRecyclerVeiw.setEdgeItemsCenteringEnabled(true);
		mRecyclerVeiw.setLayoutManager(
				new WearableLinearLayoutManager(this, new CustomScrollingLayoutCallback()));

		mRecyclerVeiw.setHasFixedSize(true);
		byte[] arr = getIntent().getExtras().getByteArray(ARG_PROFILES);
		if (arr != null && arr.length > 0) {
			try {
				profiles = new TrackProfileInfoValue.ValueList(arr);
			} catch (IOException e) {
				Logger.logE(TAG, "profile info constructor failed", e);
				finish();
			}
		}

		adapter = new ProfileListAdapter(profiles.getStorables().toArray(new TrackProfileInfoValue[0]));
		mRecyclerVeiw.setAdapter(adapter);


		// Enables Always-on
		setAmbientEnabled();
	}

	public class CustomScrollingLayoutCallback extends WearableLinearLayoutManager.LayoutCallback {
		/**
		 * How much should we scale the icon at most.
		 */
		private static final float MAX_ICON_PROGRESS = 0.65f;

		private float mProgressToCenter;

		@Override
		public void onLayoutFinished(View child, RecyclerView parent) {
			// Figure out % progress from top to bottom
			float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
			float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

			// Normalize for center
			mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);
			// Adjust to the maximum scale
			mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS);

			child.setScaleX(1 - mProgressToCenter);
			child.setScaleY(1 - mProgressToCenter);
		}
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

	class ProfileListAdapter extends RecyclerView.Adapter<ProfileListAdapter.ViewHolder> {
		private long modelIds[];
		private HashMap<Long, TrackProfileModelHolder> model;

		class ViewHolder extends RecyclerView.ViewHolder {
			// each data item is just a string in this case
			public final TextView mTextViewName;
			public final TextView mTextViewDesc;
			public final ImageView mIcon;

			public ViewHolder(View root) {
				super(root);
				mTextViewName = root.findViewById(R.id.profile_list_item_name);
				mTextViewDesc = root.findViewById(R.id.profile_list_item_desc);
				mIcon = root.findViewById(R.id.profile_list_item_image);
			}
		}

		private void onItemSelected(long modelId) {
			TrackProfileModelHolder selectedHolder = model.get(modelId);
			Intent resultIntent = new Intent();
			resultIntent.putExtra(RESULT_PROFILES, selectedHolder.mProfileInfo.getAsBytes());
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
		}

		// Provide a suitable constructor (depends on the kind of dataset)
		public ProfileListAdapter(TrackProfileInfoValue[] myDataset) {
			modelIds = new long[myDataset.length];
			model = new HashMap<>(myDataset.length);
			for (int i = 0; i < myDataset.length; i++) {
				TrackProfileInfoValue v = myDataset[i];
				modelIds[i] = v.getId();
				TrackProfileIconValue icon = AppStorageManager.getIcon(ProfileListActivity.this, v.getId());
				// TODO cejnar load icons globally and receive icon change
				model.put(v.getId(), new TrackProfileModelHolder(v.getId(), v, icon));
			}
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
			final long id = modelIds[position];
			View.OnClickListener clickHandler = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onItemSelected(id);
				}
			};
			TrackProfileModelHolder value = model.get(id);
			holder.mTextViewName.setTag(Long.valueOf(id));
			holder.mTextViewName.setText(value.mProfileInfo.getName());
			holder.mTextViewDesc.setText(value.mProfileInfo.getDesc());
			if (value.mProfileIcon != null && value.mProfileIcon.getIcon() != null) {
				holder.mIcon.setImageBitmap(UtilsBitmap.getBitmap(value.mProfileIcon.getIcon()));
			}
			holder.mTextViewName.setOnClickListener(clickHandler);
			holder.mIcon.setOnClickListener(clickHandler);
		}

		// Return the size of your dataset (invoked by the layout manager)
		@Override
		public int getItemCount() {
			return modelIds.length;
		}
	}
}
