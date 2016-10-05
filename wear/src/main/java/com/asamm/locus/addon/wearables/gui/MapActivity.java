package com.asamm.locus.addon.wearables.gui;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.utils.DataContainer;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.UtilsFormat;
import locus.api.objects.extra.ExtraData;
import locus.api.utils.Logger;

/**
 * Created by menion on 16/08/15.
 * Asamm Software, s. r. o.
 */
public class MapActivity extends CustomActivity {

    // reference to map view
    private ImageView mMapView;

    // NAVIGATION PANEL

    // main container
    private LinearLayout mLlNavPanel;
    // top navigation command (next)
    private ImageView mIvNavPanelTop;
    // main navigation command (current)
    private ImageView mIvNavPanelMiddle;
    // distance to next command (value)
    private TextView mTvNavPanelDistValue;
    // distance to next command (units)
    private TextView mTvNavPanelDistUnits;

    @Override
    protected boolean checkIfDeviceReady() {
        return true;
    }

    @Override
    public void refreshLayout() {
        // run in UI thread
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // check if preview data exists
                DataContainer baseData = getDeviceComm().getDataContainer();
                if (baseData.getMapPreview() == null) {
                    displayScreenInfo("Loading", "map preview ...");
                    return;
                }

                // check image data
                if (!baseData.getMapPreview().isValid()) {
                    displayScreenInfo("Problem", "invalid image preview ...");
                    return;
                }

                // refresh layout
                refreshMapPreview();
            }
        });
    }

    /**
     * Refresh main view.
     */
    private void refreshMapPreview() {
        // set main view
        boolean initialize = false;
        if (!isContainerVisible(R.layout.layout_map_screen)) {
            initialize = true;
        }
        View view = clearContainer(R.layout.layout_map_screen);

        // prepare views
        if (initialize) {
            mMapView = (ImageView)
                    view.findViewById(R.id.image_view_map);
            mLlNavPanel = (LinearLayout)
                    view.findViewById(R.id.linear_layout_panel_navigation);
            mIvNavPanelTop = (ImageView)
                    view.findViewById(R.id.image_view_next);
            mIvNavPanelMiddle = (ImageView)
                    view.findViewById(R.id.image_view_main);
            mTvNavPanelDistValue = (TextView)
                    view.findViewById(R.id.text_view_dist_value);
            mTvNavPanelDistUnits = (TextView)
                    view.findViewById(R.id.text_view_dist_units);
        }

        // view for map
        Bitmap img = getDeviceComm().getDataContainer().getMapPreview().getImage();
		Logger.logD("XXX", "img: " + img.getWidth() + ", " + img.getHeight() + ", " +
				mMapView.getWidth() + ", " + mMapView.getHeight() + ", " +
				view.getWidth() + ", " + view.getHeight() + ", " +
				   mContainer.getWidth() + ", " + mContainer.getHeight());
        mMapView.setImageBitmap(img);

        // set navigation panel
        refreshPanelNavigation();
    }

    /**************************************************/
    // NAVIGATION STUFF
    /**************************************************/

    /**
     * Refresh panel with navigation.
     */
    private void refreshPanelNavigation() {
        // get parameters
        LocusInfo locusInfo = getDeviceComm().getDataContainer().getLocusInfo();
        UpdateContainer lastUpdate = getDeviceComm().getLastUpdate();

        // check if navigation is active
        if (lastUpdate.getGuideType() != UpdateContainer.GUIDE_TYPE_TRACK_NAVIGATION) {
            mLlNavPanel.setVisibility(View.GONE);
            return;
        } else {
            mLlNavPanel.setVisibility(View.VISIBLE);
        }

        // get navigation info
        UpdateContainer.GuideTypeTrack navInfo = lastUpdate.getGuideTypeTrack();

        // action for current point
        int action1 = navInfo.getNavPoint1Action();
        int img1 = getNavPointImageRes(action1);
        if (img1 != -1) {
            mIvNavPanelMiddle.setImageResource(img1);
        } else {
            mIvNavPanelMiddle.setImageResource(R.drawable.ic_direction_unknown);
        }

        // action for next point
        int action2 = navInfo.getNavPoint2Action();
        int img2 = getNavPointImageRes(action2);
        if (img2 != -1) {
            mIvNavPanelTop.setImageResource(img2);
        } else {
            mIvNavPanelTop.setImageResource(R.drawable.ic_direction_unknown);
        }

        // set time to target
        mTvNavPanelDistValue.setText(UtilsFormat.formatDistance(
                locusInfo.getUnitsFormatLength(), navInfo.getNavPoint1Dist(), true));
        mTvNavPanelDistUnits.setText(UtilsFormat.formatDistanceUnits(
                locusInfo.getUnitsFormatLength(), navInfo.getNavPoint1Dist()));
    }

    /**
     * Get image that represents certain navigation action.
     * @param action navigation point action
     * @return reference to resource image
     */
    private static int getNavPointImageRes(int action) {
        switch (action) {
            case ExtraData.VALUE_RTE_ACTION_CONTINUE_STRAIGHT:
                return R.drawable.ic_direction_straight;
            case ExtraData.VALUE_RTE_ACTION_LEFT_SLIGHT:
                return R.drawable.ic_direction_left1;
            case ExtraData.VALUE_RTE_ACTION_LEFT:
                return R.drawable.ic_direction_left2;
            case ExtraData.VALUE_RTE_ACTION_LEFT_SHARP:
                return R.drawable.ic_direction_left3;
            case ExtraData.VALUE_RTE_ACTION_RIGHT_SLIGHT:
                return R.drawable.ic_direction_right1;
            case ExtraData.VALUE_RTE_ACTION_RIGHT:
                return R.drawable.ic_direction_right2;
            case ExtraData.VALUE_RTE_ACTION_RIGHT_SHARP:
                return R.drawable.ic_direction_right3;
            case ExtraData.VALUE_RTE_ACTION_STAY_LEFT:
                return R.drawable.ic_direction_stay_left;
            case ExtraData.VALUE_RTE_ACTION_STAY_RIGHT:
                return R.drawable.ic_direction_stay_right;
            case ExtraData.VALUE_RTE_ACTION_STAY_STRAIGHT:
                return R.drawable.ic_direction_straight;
            case ExtraData.VALUE_RTE_ACTION_U_TURN:
            case ExtraData.VALUE_RTE_ACTION_U_TURN_LEFT:
            case ExtraData.VALUE_RTE_ACTION_U_TURN_RIGHT:
                return R.drawable.ic_direction_turnaround;
            case ExtraData.VALUE_RTE_ACTION_EXIT_LEFT:
                return R.drawable.ic_direction_exit_left;
            case ExtraData.VALUE_RTE_ACTION_EXIT_RIGHT:
                return R.drawable.ic_direction_exit_right;
            case ExtraData.VALUE_RTE_ACTION_RAMP_ON_LEFT:
                return R.drawable.ic_direction_left1;
            case ExtraData.VALUE_RTE_ACTION_RAMP_ON_RIGHT:
                return R.drawable.ic_direction_right1;
            case ExtraData.VALUE_RTE_ACTION_RAMP_STRAIGHT:
                return R.drawable.ic_direction_straight;
            case ExtraData.VALUE_RTE_ACTION_MERGE_LEFT:
                return R.drawable.ic_direction_merge_left;
            case ExtraData.VALUE_RTE_ACTION_MERGE_RIGHT:
                return R.drawable.ic_direction_merge_right;
            case ExtraData.VALUE_RTE_ACTION_MERGE:
                return R.drawable.ic_direction_straight;
            case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST:
            case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST_LEFT:
            case ExtraData.VALUE_RTE_ACTION_ARRIVE_DEST_RIGHT:
                return R.drawable.ic_direction_finnish;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_1:
                return R.drawable.ic_direction_roundabout_1;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_2:
                return R.drawable.ic_direction_roundabout_2;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_3:
                return R.drawable.ic_direction_roundabout_3;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_4:
                return R.drawable.ic_direction_roundabout_4;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_5:
                return R.drawable.ic_direction_roundabout_5;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_6:
                return R.drawable.ic_direction_roundabout_6;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_7:
                return R.drawable.ic_direction_roundabout_7;
            case ExtraData.VALUE_RTE_ACTION_ROUNDABOUT_EXIT_8:
                return R.drawable.ic_direction_roundabout_8;
            case ExtraData.VALUE_RTE_ACTION_PASS_PLACE:
                return R.drawable.ic_direction_finnish;
            default:
                return -1;
        }
    }
}
