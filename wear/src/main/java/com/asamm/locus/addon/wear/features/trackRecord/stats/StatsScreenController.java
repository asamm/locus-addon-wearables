package com.asamm.locus.addon.wear.features.trackRecord.stats;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState;
import com.asamm.locus.addon.wear.features.trackRecord.recording.TrackRecordingControllerUpdatable;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration;
import com.asamm.locus.addon.wear.features.trackRecord.stats.view.TrackStatsScreenView;
import com.asamm.locus.addon.wear.features.trackRecord.stats.view.TrackStatsScreenViewFactory;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class StatsScreenController implements TrackRecordingControllerUpdatable {
    private TrackStatsScreenView view;

    private final int mScreenIdx;

    public StatsScreenController(ViewGroup contentView, int screenIdx) {
        this.mScreenIdx = screenIdx;
        TrackRecordActivityConfiguration config = TrackRecordActivityConfiguration.getConfiguration(contentView.getContext());
        view = TrackStatsScreenViewFactory.createViewForConfig(contentView, screenIdx, config.getScreenConfigAtPosition(getControllerScreenIdx()));
    }

    @Override
    public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {

    }

    @Override
    public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
        view.handleNewTrackRecData(newData);
    }

    @Override
    public ViewGroup getControllersView() {
        return view.getInflatedLayout();
    }

    @Override
    public void setAmbient(boolean isAmbient) {
        view.setAmbientMode(isAmbient);
    }

    @Override
    public int getControllerScreenIdx() {
        return mScreenIdx;
    }

    @Override
    public void refreshStatisticsConfiguration(Context ctx) {
        TrackRecordActivityConfiguration cfg = TrackRecordActivityConfiguration.getConfiguration(ctx);
        view.refreshConfiguration(cfg.getScreenConfigAtPosition(getControllerScreenIdx()));
    }

}
