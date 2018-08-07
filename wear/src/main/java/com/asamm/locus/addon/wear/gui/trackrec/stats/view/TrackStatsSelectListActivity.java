/*
 * Created by milan on 02.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;

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

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackStatTypeEnum;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackStatViewId;

/**
 * Activity containing single recycler view for choosing recording profile.
 */
public class TrackStatsSelectListActivity extends LocusWearActivity {

    private static final String TAG = "TrackStatsSelectListActivity";
    public static final String RESULT_STAT_ID = "RESULT_STAT_ID";
    public static final String PARAM_SCREEN_IDX = "PARAM_SCREEN_IDX";
    public static final String PARAM_CELL_IDX = "PARAM_CELL_IDX";
    public static final String PARAM_STAT_ID = "PARAM_STAT_ID";
    public static final int REQUEST_CODE_STATS_SELECT_LIST_ACTIVITY = 1793130860;

    private WearableRecyclerView mRecyclerView;
    private StatsTypeListAdapter mAdapter;
    private TrackStatViewId viewId;

    /**
     * Id of currently selected statistics for this cell
     */
    private byte mCurrentStatId;

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

        // TODO cejnar set default to blank
        mCurrentStatId = getIntent().getExtras().getByte(PARAM_STAT_ID, (byte) 0);
        viewId = new TrackStatViewId(getIntent().getExtras().getInt(PARAM_SCREEN_IDX, -1),
                getIntent().getExtras().getInt(PARAM_CELL_IDX, -1));
        mAdapter = new StatsTypeListAdapter();
        mRecyclerView.setAdapter(mAdapter);

        // Enables Always-on
        //setAmbientEnabled();
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

    private class StatsTypeListAdapter extends RecyclerView.Adapter<StatsTypeListAdapter.ViewHolder> {
        private TrackStatTypeEnum[] mModel = TrackStatTypeEnum.VALUES_FOR_SELECTIONS;

        private void onItemSelected(TrackStatTypeEnum selectedType) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_STAT_ID, selectedType.getId());
            resultIntent.putExtra(PARAM_SCREEN_IDX, viewId.getScreenIdx());
            resultIntent.putExtra(PARAM_CELL_IDX, viewId.getCellIdx());
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }

        // Create new views (invoked by the layout manager)
        @Override
        public StatsTypeListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_layout, parent, false);
            // set the view's size, margins, paddings and layout parameters
            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final TrackStatTypeEnum value = mModel[position];


            holder.mTextViewName.setText(getString(value.getNameStringId()));
            holder.mIcon.setImageDrawable(getDrawable(value.getIconId()));
            View.OnClickListener clickHandler = view -> onItemSelected(value);
            holder.mTextViewName.setOnClickListener(clickHandler);
            holder.mIcon.setOnClickListener(clickHandler);

            if (mCurrentStatId == value.getId()) {
                holder.mTextViewName.setTextColor(getColor(R.color.crimson));
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mModel.length;
        }

        /**
         * View holder for this recycler view
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            final TextView mTextViewName;
            final ImageView mIcon;

            ViewHolder(View root) {
                super(root);
                mTextViewName = root.findViewById(R.id.profile_list_item_name);
                mIcon = root.findViewById(R.id.profile_list_item_image);
            }
        }
    }
}
