package com.asamm.locus.addon.wearables.gui;

import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.gui.lists.ListItemAdapter;
import com.asamm.locus.addon.wearables.gui.lists.ListItemLayout;
import com.asamm.locus.addon.wearables.utils.DataContainer;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.UtilsBitmap;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.extra.TrackStats;

/**
 * Created by menion on 15/08/15.
 * Asamm Software, s. r. o.
 */
public class TrackRecordActivity extends CustomActivity {

    // tag for logger
    private static final String TAG = "TrackRecordActivity";

    // time format
    public static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    static {
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC+0"));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // request profiles
        getDeviceComm().performTrackRecordingGetProfiles();
    }

    // ABSTRACT FUNCTIONS

    @Override
    protected boolean checkIfDeviceReady() {
        return true;
    }

    @Override
    public void refreshLayout() {
        refreshLayout(false);
    }

    /**
     * Refresh current layout.
     * @param forceReload <code>true</code> to ignore for example confirmation screen
     */
    private void refreshLayout(boolean forceReload) {
        // check activity
        if (isFinishing()) {
            return;
        }

        // check confirmation view
        if (isConfirmationActionVisible() && !forceReload) {
            return;
        }

        // run in UI thread
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // check profiles
                if (getDeviceComm().getDataContainer().getTrackRecProfiles() == null) {
                    displayScreenProgress(
                            getString(R.string.init),
                            getString(R.string.init_loading_profiles));
                    return;
                }

                // check if data are available
                if (getDeviceComm().getLastUpdate() == null) {
                    displayScreenInfo("Info", "Info");
                    return;
                }

                // display main layout
                refreshTrackRecordingLayout(getDeviceComm().getLastUpdate());
            }
        });
    }

    /**************************************************/
    // WORK WITH LAYOUT
    /**************************************************/

    /**
     * Refresh layout for track recording.
     */
    private void refreshTrackRecordingLayout(UpdateContainer cont) {
        // get data
        DataContainer container = getDeviceComm().getDataContainer();
        if (!cont.isTrackRecRecording()) {
            // check existence of track record profiles
            if (container.getTrackRecProfiles() == null ||
                    container.getTrackRecProfiles().size() == 0) {
                displayScreenInfo(
                        getString(R.string.warning),
                        getString(R.string.track_record_no_profiles));
            } else {
                displayScreenTrackRecordProfiles();
            }
        } else {
            displayScreenTrackRecordRunning(cont);
        }
    }

    /**
     * Display screen with list of recording profiles.
     */
    private void displayScreenTrackRecordProfiles() {
        // do not reset layout if profiles are already visible
        if (isContainerVisible(R.layout.layout_track_record_profiles)) {
            return;
        }

        // prepare items
        List<ActionTools.TrackRecordProfileSimple> profiles =
                getDeviceComm().getDataContainer().getTrackRecProfiles();

        // create adapter
        ListItemAdapter adapter = new ListItemAdapter<ActionTools.TrackRecordProfileSimple>(this, profiles) {

            @Override
            public void setItemView(ActionTools.TrackRecordProfileSimple item, ListItemLayout layout) {
                // set title
                layout.getTextViewTitle().setText(item.getName());

                // set text for description
                layout.getTextViewDesc().setText(item.getDesc());

                // set icon
                ImageView ivIcon = layout.getImageViewIcon();
                if (item.getIcon() == null) {
                    ivIcon.setVisibility(View.INVISIBLE);
                } else {
                    ivIcon.setVisibility(View.VISIBLE);
                    ivIcon.setImageBitmap(UtilsBitmap.getBitmap(item.getIcon()));
                }
            }
        };

        // set content
        WearableListView lv = (WearableListView)
                clearContainer(R.layout.layout_track_record_profiles);
        lv.setAdapter(adapter);
        lv.setClickListener(new WearableListView.ClickListener() {

            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                ListItemLayout itemLayout = ((ListItemAdapter.ItemViewHolder)
                        viewHolder).getLayout();
                ActionTools.TrackRecordProfileSimple item = (ActionTools.TrackRecordProfileSimple)
                        itemLayout.getTag();
                onTrackRecordProfileSelected(item.getId());
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });
    }

    private void displayScreenTrackRecordRunning(UpdateContainer cont) {
        boolean initialize = !isContainerVisible(R.layout.layout_track_record_control);
        TrackStats recStats = cont.getTrackRecStats();

        // prepare core
        View view = clearContainer(R.layout.layout_track_record_control);
        LocusInfo locusInfo = getDeviceComm().getDataContainer().getLocusInfo();
        /*TextView tv01Title = (TextView)
                view.findViewById(R.id.text_view_title_01);*/
        TextView tv01Value = (TextView)
                view.findViewById(R.id.text_view_info_01);
        /*TextView tv02Title = (TextView)
                view.findViewById(R.id.text_view_title_02);*/
        TextView tv02Value = (TextView)
                view.findViewById(R.id.text_view_info_02);
        TextView tv03Value = (TextView)
                view.findViewById(R.id.text_view_info_03);
        TextView tv04Value = (TextView)
                view.findViewById(R.id.text_view_info_04);


        // set buttons
        final ImageButton btnStop = (ImageButton)
                view.findViewById(R.id.image_view_track_rec_stop);
        final ImageButton btnPause = (ImageButton)
                view.findViewById(R.id.image_view_track_rec_pause);
        final ImageButton btnAddWpt = (ImageButton)
                view.findViewById(R.id.image_view_track_rec_add_wpt);

        // set icons
        if (!cont.isTrackRecPaused()) {
            btnPause.setImageResource(R.drawable.ic_96_track_recording_pause);
        } else {
            btnPause.setImageResource(R.drawable.ic_96_track_recording_pause_pressed);
        }

        // set basics
        if (initialize) {
            // listener for delay events
            final DelayedConfirmationView.DelayedConfirmationListener onDelay =
                    new DelayedConfirmationView.DelayedConfirmationListener() {

                        @Override
                        public void onTimerFinished(View view) {
                            // stop recording
                            getDeviceComm().performTrackRecordStop();

                            // finish activity
                            finish();
                        }

                        @Override
                        public void onTimerSelected(View view) {
                            // cancel timer with small delay, so it will not allow to click-through
                            // button to refreshed layout
                            new Handler().postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    refreshLayout(true);
                                }
                            }, 500);
                        }
                    };

            // listener for onClick events
            View.OnClickListener onClick = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (v == btnStop) {
                        displayConfirmationAction(2000,
                                R.string.cancel, onDelay);
                    } else if (v == btnPause) {
                        getDeviceComm().performTrackRecordPause();
                    } else if (v == btnAddWpt) {
                        getDeviceComm().performTrackRecordAddWpt();
                    }
                }
            };

            // set basic listeners
            btnStop.setOnClickListener(onClick);
            btnPause.setOnClickListener(onClick);
            btnAddWpt.setOnClickListener(onClick);
        }

        // update title
        setScreenHeader(cont.getTrackRecProfileName());

        // text view with value 1
        //tv01Title.setText(R.string.time);
        tv01Value.setText(TIME_FORMAT.format(recStats.getTotalTime()));

        // text view with value 2
        //tv02Title.setText(R.string.distance);
        String avgspeed = UtilsFormat.formatSpeed(
                locusInfo.getUnitsFormatSpeed(), recStats.getSpeedAverage(false), false);
        tv02Value.setText(avgspeed);

        // text view with value 3
        String elevationgain = UtilsFormat.formatAltitude(
                locusInfo.getUnitsFormatAltitude(), recStats.getElePositiveHeight(), false);
        tv03Value.setText(elevationgain);

        // text view with value 4
        String distance = UtilsFormat.formatDistance(
                locusInfo.getUnitsFormatLength(), recStats.getTotalLength(), false);
        tv04Value.setText(distance);

    }


    /**
     * Handle tap on certain track recording profile.
     * @param profileId ID of tapped profile
     */
    private void onTrackRecordProfileSelected(long profileId) {
        // get selected profile
        List<ActionTools.TrackRecordProfileSimple> profiles =
                getDeviceComm().getDataContainer().getTrackRecProfiles();
        ActionTools.TrackRecordProfileSimple selectedProfile = null;
        for (ActionTools.TrackRecordProfileSimple profile : profiles) {
            if (profile.getId() == profileId) {
                selectedProfile = profile;
                break;
            }
        }

        // check found profile
        if (selectedProfile == null) {
//            displayScreenInfoUnknownProblem(ERROR_CODE_TRACK_REC_PROFILE_NOT_FOUND);
            return;
        }

        // start track recording
        getDeviceComm().performTrackRecordStart(selectedProfile.getName());
    }
}
