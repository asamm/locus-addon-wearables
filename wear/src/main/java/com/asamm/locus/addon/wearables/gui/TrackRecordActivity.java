package com.asamm.locus.addon.wearables.gui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WearableListView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.gui.lists.ListItemAdapter;
import com.asamm.locus.addon.wearables.gui.lists.ListItemLayout;
import com.asamm.locus.addon.wearables.utils.DataContainer;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.UtilsBitmap;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.extra.Location;
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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private boolean m_isGridCreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // request profiles
        getDeviceComm().performTrackRecordingGetProfiles();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        setAmbientEnabled();
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
     *
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
                    displayScreenInfo("Reconnecting", "Reconnecting...");
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
        if (!m_isGridCreated)
        {
            // prepare core
            View view = clearContainer(R.layout.layout_grid_view_pager);

            GridViewPager pager = (GridViewPager) findViewById(R.id.pager);

            //---Assigns an adapter to provide the content for this pager---
            pager.setAdapter(new TrackRecordAdapter(this));
            DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
            dotsPageIndicator.setPager(pager);
            m_isGridCreated = true;

        }

        boolean cpanel_visible = !isContainerVisible(R.layout.layout_track_record_control);
        boolean stat_visible = !isContainerVisible(R.layout.layout_track_statistic);
        boolean stat2_visible = !isContainerVisible(R.layout.layout_track_statistic2);

        TrackStats recStats = cont.getTrackRecStats();

        // prepare core
        View view = clearContainer(R.layout.layout_grid_view_pager);
        LocusInfo locusInfo = getDeviceComm().getDataContainer().getLocusInfo();

        if (cpanel_visible) {
            // set buttons
            final ImageButton btnStop = (ImageButton)
                    view.findViewById(R.id.image_view_track_rec_stop);
            final ImageButton btnPause = (ImageButton)
                    view.findViewById(R.id.image_view_track_rec_pause);
            final ImageButton btnAddWpt = (ImageButton)
                    view.findViewById(R.id.image_view_track_rec_add_wpt);

            // set icons
            if (!cont.isTrackRecPaused()) {
                if (btnPause != null)
                    btnPause.setImageResource(R.drawable.ic_96_track_recording_pause);
            } else {
                if (btnPause != null)
                    btnPause.setImageResource(R.drawable.ic_96_track_recording_pause_pressed);
            }

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
            if (btnStop != null)
                btnStop.setOnClickListener(onClick);
            if (btnPause != null)
                btnPause.setOnClickListener(onClick);
            if (btnAddWpt != null)
                btnAddWpt.setOnClickListener(onClick);

            TextView tvBatteryLevelPhoneValue = (TextView)
                    view.findViewById(R.id.text_view_battery_level_phone);
            TextView tvBatteryLevelWatchValue = (TextView)
                    view.findViewById(R.id.text_view_battery_level_watch);

            if (tvBatteryLevelPhoneValue != null)
                tvBatteryLevelPhoneValue.setText(cont.getDeviceBatteryValue() + "%");

            if (tvBatteryLevelWatchValue != null) {
                Intent batteryStatus = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = 0;

                if (scale != 0)
                    batteryPct = (int)((level / (float) scale) * 100);

                if (batteryPct != 0)
                    tvBatteryLevelWatchValue.setText(batteryPct + "%");
                else
                    tvBatteryLevelWatchValue.setText(batteryPct + "-");
            }


        }

        // update title
        //setScreenHeader(cont.getTrackRecProfileName());

        if (stat_visible) {
            TextView tvTimeValue = (TextView)
                    view.findViewById(R.id.text_view_info_time);
            TextView tvAltitudeValue = (TextView)
                    view.findViewById(R.id.text_view_info_altitude);
            TextView tvAvgSpeedValue = (TextView)
                    view.findViewById(R.id.text_view_info_avgspeed);
            TextView tvDistanceValue = (TextView)
                    view.findViewById(R.id.text_view_info_distance);

            // text view time
            if (tvTimeValue != null)
                tvTimeValue.setText(TIME_FORMAT.format(recStats.getTotalTime()));

            // text view avg speed
            String avgspeed = UtilsFormat.formatSpeed(
                    locusInfo.getUnitsFormatSpeed(), recStats.getSpeedAverage(false), false);
            if (tvAvgSpeedValue != null)
                tvAvgSpeedValue.setText(avgspeed);

            // text view elevation
            String elevationgain = UtilsFormat.formatAltitude(
                    locusInfo.getUnitsFormatAltitude(), recStats.getElePositiveHeight(), false);
            if (tvAltitudeValue != null)
                tvAltitudeValue.setText(elevationgain);

            // text view distance
            String distance = UtilsFormat.formatDistance(
                    locusInfo.getUnitsFormatLength(), recStats.getTotalLength(), false);
            if (tvDistanceValue != null)
                tvDistanceValue.setText(distance);
        }

        if (stat2_visible) {
            TextView tvHrRateValue = (TextView)
                    view.findViewById(R.id.text_view_info_hrrate);
            TextView tvHrMaxValue = (TextView)
                    view.findViewById(R.id.text_view_info_hrmax);
            TextView tvHrAvgValue = (TextView)
                    view.findViewById(R.id.text_view_info_hravg);

            // text view heart rate avg
            String hrmAvg = String.valueOf(recStats.getHrmAverage());
            if (tvHrAvgValue != null)
                tvHrAvgValue.setText(hrmAvg);

            // text view heart rate max
            String hrmMax = String.valueOf(recStats.getHrmMax());
            if (tvHrMaxValue != null)
                tvHrMaxValue.setText(hrmMax);

            //get heart rate
            int hrRate = 0;
            Location myLocation = cont.getLocMyLocation();
            if (myLocation != null)
                if (myLocation.hasSensorHeartRate())
                    hrRate = myLocation.getSensorHeartRate();

            // text view heart rate bpm
            if (tvHrRateValue != null) {
                if (hrRate == 0)
                    tvHrRateValue.setText("-");
                else
                    tvHrRateValue.setText(String.valueOf(hrRate));
            }
        }
    }

    /**
     * Handle tap on certain track recording profile.
     *
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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("TrackRecord Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


    public class TrackRecordAdapter extends GridPagerAdapter {
        final Context mContext;

        public TrackRecordAdapter(final Context context) {
            mContext = context;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int i) {
            return 3;
        }

        //---Go to current column when scrolling up or down (instead of default column 0)---
        @Override
        public int getCurrentColumnForRow(int row, int currentColumn) {
            return currentColumn;
        }

        //---Return our car image based on the provided row and column---
        @Override
        public Object instantiateItem(ViewGroup viewGroup, int row, int col) {
            View view;

            switch (col) {
                default:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_track_statistic, null);
                    break;
                case 1:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_track_statistic2, null);
                    break;
                case 2:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_track_record_control, null);
            }

            viewGroup.addView(view);

            refreshLayout(false);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup viewGroup, int i, int i2, Object o) {
            viewGroup.removeView((View) o);
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view.equals(o);
        }
    }
}
