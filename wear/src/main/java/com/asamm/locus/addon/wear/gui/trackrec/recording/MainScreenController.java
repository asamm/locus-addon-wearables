package com.asamm.locus.addon.wear.gui.trackrec.recording;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import androidx.wear.widget.CircularProgressLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.DisableGuiHelper;
import com.asamm.locus.addon.wear.gui.trackrec.TrackRecActivityState;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecordActivityConfiguration;
import com.asamm.locus.addon.wear.gui.trackrec.stats.view.TrackStatLayout;

import java.util.Arrays;

/**
 * Controller class for main/control screen of active track recording
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainScreenController implements TrackRecordingControllerUpdatable {
    private ViewGroup mLayout;

    // recording active screen fields
    private ImageView mImgPauseRecording, mImgStopRecording, mImgAddWaypoint;
    private TrackStatLayout mStatsTop, mStatsBottom;
    private CircularProgressLayout mCircularProgress;

    private Drawable mPauseDrawable;
    private Drawable mResumeDrawable;
    private Drawable mStopDrawable;

    public MainScreenController(ViewGroup parentViewGroup) {
        LayoutInflater inflater = LayoutInflater.from(parentViewGroup.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_main, parentViewGroup, false);
        mImgAddWaypoint = mLayout.findViewById(R.id.image_view_track_rec_add_wpt);
        mImgStopRecording = mLayout.findViewById(R.id.image_view_track_rec_stop);
        mImgPauseRecording = mLayout.findViewById(R.id.image_view_track_rec_pause);
        mStatsTop = mLayout.findViewById(R.id.track_main_top);
        mStatsBottom = mLayout.findViewById(R.id.track_main_bottom);
        mCircularProgress = mLayout.findViewById(R.id.circular_progress);

        mStatsTop.setTrackStatViewPositionId(0, 0);
        mStatsBottom.setTrackStatViewPositionId(0, 1);
        refreshStatisticsConfiguration(parentViewGroup.getContext());
        setDisabledDrawables(parentViewGroup.getContext());
    }

    @Override
    public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
        switch (newState) {
            case UNINITIALIZED:
            case REC_WAITING:
                mImgPauseRecording.setImageDrawable(mPauseDrawable);
                setRecScreenEnabled(false);
                break;
            case PAUSED_WAITING:
                mImgPauseRecording.setImageDrawable(mResumeDrawable);
                setRecScreenEnabled(false);
                break;
            case PAUSED:
            case REC:
                setRecScreenEnabled(true);
                break;
        }
    }

    @Override
    public void onNewTrackRecordingData(Activity context, TrackRecordingValue newData) {
        mStatsTop.consumeNewStatistics(newData);
        mStatsBottom.consumeNewStatistics(newData);
    }

    private void setRecScreenEnabled(boolean isEnabled) {
        mImgPauseRecording.setEnabled(isEnabled);
        mImgAddWaypoint.setEnabled(isEnabled);
        mImgStopRecording.setEnabled(isEnabled);
        mCircularProgress.setEnabled(isEnabled);
    }

    @Override
    public ViewGroup getControllersView() {
        return mLayout;
    }

    private void setDisabledDrawables(Context ctx) {
        Drawable d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_stop));
        mImgStopRecording.setImageDrawable(d);
        mStopDrawable = d;

        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_pause_normal));
        mPauseDrawable = d;
        mImgPauseRecording.setImageDrawable(d);

        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_pause_pressed));
        mResumeDrawable = d;


        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_add_wpt));
        mImgAddWaypoint.setImageDrawable(d);
    }

    @Override
    public void setAmbient(boolean isAmbient) {
        mStatsTop.setAmbientMode(isAmbient);
        mStatsBottom.setAmbientMode(isAmbient);
        for (ImageView v : Arrays.asList(mImgPauseRecording, mImgStopRecording, mImgAddWaypoint)) {
            v.setVisibility(isAmbient ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    public int getControllerScreenIdx() {
        return 0;
    }

    @Override
    public void refreshStatisticsConfiguration(Context ctx) {
        TrackRecordActivityConfiguration mConfig = TrackRecordActivityConfiguration.getConfiguration(ctx);
        mStatsTop.setType(mConfig.getStatConfigAtPosition(getControllerScreenIdx(), 0));
        mStatsBottom.setType(mConfig.getStatConfigAtPosition(getControllerScreenIdx(), 1));

    }

    public void setProgressionVisible(boolean enableProgression) {
        mImgStopRecording.setTag(Boolean.valueOf(enableProgression));
        mImgStopRecording.setImageDrawable(enableProgression ?
                getControllersView().getContext().getDrawable(R.drawable.ic_track_record_cancel)
                : mStopDrawable);
        setRecScreenEnabled(!enableProgression);
        mImgStopRecording.setEnabled(true);
        mCircularProgress.setEnabled(true);
    }

    public Boolean isProgressionVisible() {
        return Boolean.TRUE.equals(mImgStopRecording.getTag());
    }
}
