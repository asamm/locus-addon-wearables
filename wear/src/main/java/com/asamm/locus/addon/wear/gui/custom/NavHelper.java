package com.asamm.locus.addon.wear.gui.custom;

import com.asamm.locus.addon.wear.R;

import locus.api.objects.enums.PointRteAction;

import static locus.api.objects.enums.PointRteAction.ARRIVE_DEST;
import static locus.api.objects.enums.PointRteAction.ARRIVE_DEST_LEFT;
import static locus.api.objects.enums.PointRteAction.ARRIVE_DEST_RIGHT;
import static locus.api.objects.enums.PointRteAction.CONTINUE_STRAIGHT;
import static locus.api.objects.enums.PointRteAction.EXIT_LEFT;
import static locus.api.objects.enums.PointRteAction.EXIT_RIGHT;
import static locus.api.objects.enums.PointRteAction.LEFT;
import static locus.api.objects.enums.PointRteAction.LEFT_SHARP;
import static locus.api.objects.enums.PointRteAction.LEFT_SLIGHT;
import static locus.api.objects.enums.PointRteAction.MERGE;
import static locus.api.objects.enums.PointRteAction.MERGE_LEFT;
import static locus.api.objects.enums.PointRteAction.MERGE_RIGHT;
import static locus.api.objects.enums.PointRteAction.PASS_PLACE;
import static locus.api.objects.enums.PointRteAction.RAMP_ON_LEFT;
import static locus.api.objects.enums.PointRteAction.RAMP_ON_RIGHT;
import static locus.api.objects.enums.PointRteAction.RAMP_STRAIGHT;
import static locus.api.objects.enums.PointRteAction.RIGHT;
import static locus.api.objects.enums.PointRteAction.RIGHT_SHARP;
import static locus.api.objects.enums.PointRteAction.RIGHT_SLIGHT;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_1;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_2;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_3;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_4;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_5;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_6;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_7;
import static locus.api.objects.enums.PointRteAction.ROUNDABOUT_EXIT_8;
import static locus.api.objects.enums.PointRteAction.STAY_LEFT;
import static locus.api.objects.enums.PointRteAction.STAY_RIGHT;
import static locus.api.objects.enums.PointRteAction.STAY_STRAIGHT;
import static locus.api.objects.enums.PointRteAction.U_TURN;
import static locus.api.objects.enums.PointRteAction.U_TURN_LEFT;
import static locus.api.objects.enums.PointRteAction.U_TURN_RIGHT;

/**
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */

public class NavHelper {
    public static int getNavPointImageRes(int pointRteActionEnumId) {

        if (pointRteActionEnumId == CONTINUE_STRAIGHT.getId())
            return R.drawable.ic_direction_straight;
        if (pointRteActionEnumId == LEFT_SLIGHT.getId())
            return R.drawable.ic_direction_left1;
        if (pointRteActionEnumId == LEFT.getId())
            return R.drawable.ic_direction_left2;
        if (pointRteActionEnumId == LEFT_SHARP.getId())
            return R.drawable.ic_direction_left3;
        if (pointRteActionEnumId == RIGHT_SLIGHT.getId())
            return R.drawable.ic_direction_right1;
        if (pointRteActionEnumId == RIGHT.getId())
            return R.drawable.ic_direction_right2;
        if (pointRteActionEnumId == RIGHT_SHARP.getId())
            return R.drawable.ic_direction_right3;
        if (pointRteActionEnumId == STAY_LEFT.getId())
            return R.drawable.ic_direction_stay_left;
        if (pointRteActionEnumId == STAY_RIGHT.getId())
            return R.drawable.ic_direction_stay_right;
        if (pointRteActionEnumId == STAY_STRAIGHT.getId())
            return R.drawable.ic_direction_straight;
        if (pointRteActionEnumId == U_TURN.getId() ||
                pointRteActionEnumId == U_TURN_LEFT.getId() ||
                pointRteActionEnumId == U_TURN_RIGHT.getId())
            return R.drawable.ic_direction_turnaround;
        if (pointRteActionEnumId == EXIT_LEFT.getId())
            return R.drawable.ic_direction_exit_left;
        if (pointRteActionEnumId == EXIT_RIGHT.getId())
            return R.drawable.ic_direction_exit_right;
        if (pointRteActionEnumId == RAMP_ON_LEFT.getId())
            return R.drawable.ic_direction_left1;
        if (pointRteActionEnumId == RAMP_ON_RIGHT.getId())
            return R.drawable.ic_direction_right1;
        if (pointRteActionEnumId == RAMP_STRAIGHT.getId())
            return R.drawable.ic_direction_straight;
        if (pointRteActionEnumId == MERGE_LEFT.getId())
            return R.drawable.ic_direction_merge_left;
        if (pointRteActionEnumId == MERGE_RIGHT.getId())
            return R.drawable.ic_direction_merge_right;
        if (pointRteActionEnumId == MERGE.getId())
            return R.drawable.ic_direction_straight;
        if (pointRteActionEnumId == ARRIVE_DEST.getId() ||
                pointRteActionEnumId == ARRIVE_DEST_LEFT.getId() ||
                pointRteActionEnumId == ARRIVE_DEST_RIGHT.getId())
            return R.drawable.ic_direction_finnish;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_1.getId())
            return R.drawable.ic_direction_roundabout_1;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_2.getId())
            return R.drawable.ic_direction_roundabout_2;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_3.getId())
            return R.drawable.ic_direction_roundabout_3;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_4.getId())
            return R.drawable.ic_direction_roundabout_4;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_5.getId())
            return R.drawable.ic_direction_roundabout_5;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_6.getId())
            return R.drawable.ic_direction_roundabout_6;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_7.getId())
            return R.drawable.ic_direction_roundabout_7;
        if (pointRteActionEnumId == ROUNDABOUT_EXIT_8.getId())
            return R.drawable.ic_direction_roundabout_8;
        if (pointRteActionEnumId == PASS_PLACE.getId())
            return R.drawable.ic_direction_finnish;

        return PointRteAction.UNDEFINED.getId();
    }
}
