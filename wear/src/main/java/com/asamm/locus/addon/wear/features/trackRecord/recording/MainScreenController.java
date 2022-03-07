/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.features.trackRecord.recording;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration;
import com.asamm.locus.addon.wear.features.trackRecord.stats.view.TrackStatLayout;
import com.asamm.locus.addon.wear.gui.custom.DisableGuiHelper;

import java.util.Arrays;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.wear.widget.CircularProgressLayout;

/**
 * Controller class for main/control screen of active track recording
 */
public class MainScreenController implements TrackRecordingControllerUpdatable {

    private ViewGroup mLayout;

    // recording active screen fields
    private final ImageView imgPauseRecording;
    private final ImageView imgStopRecording;
    private final ImageView imgAddWaypoint;
    private final TrackStatLayout statsTop;
    private final TrackStatLayout statsBottom;
    private final CircularProgressLayout circularProgress;

    private Drawable pauseDrawable;
    private Drawable resumeDrawable;
    private Drawable stopDrawable;

    public MainScreenController(ViewGroup parentViewGroup) {
        LayoutInflater inflater = LayoutInflater.from(parentViewGroup.getContext());
        mLayout = (ViewGroup) inflater.inflate(R.layout.track_record_recording_screen_main, parentViewGroup, false);
        imgAddWaypoint = mLayout.findViewById(R.id.image_view_track_rec_add_wpt);
        imgStopRecording = mLayout.findViewById(R.id.image_view_track_rec_stop);
        imgPauseRecording = mLayout.findViewById(R.id.image_view_track_rec_pause);
        statsTop = mLayout.findViewById(R.id.track_main_top);
        statsBottom = mLayout.findViewById(R.id.track_main_bottom);
        circularProgress = mLayout.findViewById(R.id.circular_progress);

        statsTop.setTrackStatViewPositionId(0, 0);
        statsBottom.setTrackStatViewPositionId(0, 1);
        refreshStatisticsConfiguration(parentViewGroup.getContext());
        setDisabledDrawables(parentViewGroup.getContext());
    }

    @Override
    public void onTrackActivityStateChange(Activity context, TrackRecActivityState newState) {
        switch (newState) {
            case UNINITIALIZED:
            case REC_WAITING:
                imgPauseRecording.setImageDrawable(pauseDrawable);
                setRecScreenEnabled(false);
                break;
            case PAUSED_WAITING:
                imgPauseRecording.setImageDrawable(resumeDrawable);
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
        statsTop.consumeNewStatistics(newData);
        statsBottom.consumeNewStatistics(newData);
    }

    private void setRecScreenEnabled(boolean isEnabled) {
        imgPauseRecording.setEnabled(isEnabled);
        imgAddWaypoint.setEnabled(isEnabled);
        imgStopRecording.setEnabled(isEnabled);
        circularProgress.setEnabled(isEnabled);
    }

    @Override
    public ViewGroup getControllersView() {
        return mLayout;
    }

    private void setDisabledDrawables(Context ctx) {
        Drawable d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_stop));
        imgStopRecording.setImageDrawable(d);
        stopDrawable = d;

        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_pause_normal));
        pauseDrawable = d;
        imgPauseRecording.setImageDrawable(d);

        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_pause_pressed));
        resumeDrawable = d;


        d = DisableGuiHelper.getImageWithDisabled(ctx,
                BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_track_record_add_wpt));
        imgAddWaypoint.setImageDrawable(d);
    }

    @Override
    public void setAmbient(boolean isAmbient) {
        statsTop.setAmbientMode(isAmbient);
        statsBottom.setAmbientMode(isAmbient);
        for (ImageView v : Arrays.asList(imgPauseRecording, imgStopRecording, imgAddWaypoint)) {
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
        statsTop.setType(mConfig.getStatConfigAtPosition(getControllerScreenIdx(), 0));
        statsBottom.setType(mConfig.getStatConfigAtPosition(getControllerScreenIdx(), 1));

    }

    public void setProgressionVisible(boolean enableProgression) {
        imgStopRecording.setTag(enableProgression);
        imgStopRecording.setImageDrawable(enableProgression ?
                AppCompatResources.getDrawable(mLayout.getContext(), R.drawable.ic_track_record_cancel)
                : stopDrawable);
        setRecScreenEnabled(!enableProgression);
        imgStopRecording.setEnabled(true);
        circularProgress.setEnabled(true);
    }

    public Boolean isProgressionVisible() {
        return Boolean.TRUE.equals(imgStopRecording.getTag());
    }
}
