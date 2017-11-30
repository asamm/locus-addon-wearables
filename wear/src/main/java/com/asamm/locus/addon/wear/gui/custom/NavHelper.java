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
	public static int getNavPointImageRes(PointRteAction action) {
		switch (action) {
			case CONTINUE_STRAIGHT:
				return R.drawable.ic_direction_straight;
			case LEFT_SLIGHT:
				return R.drawable.ic_direction_left_1;
			case LEFT:
				return R.drawable.ic_direction_left_2;
			case LEFT_SHARP:
				return R.drawable.ic_direction_left_3;
			case RIGHT_SLIGHT:
				return R.drawable.ic_direction_right_1;
			case RIGHT:
				return R.drawable.ic_direction_right_2;
			case RIGHT_SHARP:
				return R.drawable.ic_direction_right_3;
			case STAY_LEFT:
				return R.drawable.ic_direction_stay_left;
			case STAY_RIGHT:
				return R.drawable.ic_direction_stay_right;
			case STAY_STRAIGHT:
				return R.drawable.ic_direction_straight;
			case U_TURN:
			case U_TURN_LEFT:
				return R.drawable.ic_direction_turnaround_left;
			case U_TURN_RIGHT:
				return R.drawable.ic_direction_turnaround_right;
			case EXIT_LEFT:
				return R.drawable.ic_direction_exit_left;
			case EXIT_RIGHT:
				return R.drawable.ic_direction_exit_right;
			case RAMP_ON_LEFT:
				return R.drawable.ic_direction_left_1;
			case RAMP_ON_RIGHT:
				return R.drawable.ic_direction_right_1;
			case RAMP_STRAIGHT:
				return R.drawable.ic_direction_straight;
			case MERGE_LEFT:
				return R.drawable.ic_direction_merge_left;
			case MERGE_RIGHT:
				return R.drawable.ic_direction_merge_right;
			case MERGE:
				return R.drawable.ic_direction_straight;
			case ARRIVE_DEST:
			case ARRIVE_DEST_LEFT:
			case ARRIVE_DEST_RIGHT:
				return R.drawable.ic_direction_finish;
			case ROUNDABOUT_EXIT_1:
				return R.drawable.ic_direction_roundabout_1;
			case ROUNDABOUT_EXIT_2:
				return R.drawable.ic_direction_roundabout_2;
			case ROUNDABOUT_EXIT_3:
				return R.drawable.ic_direction_roundabout_3;
			case ROUNDABOUT_EXIT_4:
				return R.drawable.ic_direction_roundabout_4;
			case ROUNDABOUT_EXIT_5:
				return R.drawable.ic_direction_roundabout_5;
			case ROUNDABOUT_EXIT_6:
				return R.drawable.ic_direction_roundabout_6;
			case ROUNDABOUT_EXIT_7:
				return R.drawable.ic_direction_roundabout_7;
			case ROUNDABOUT_EXIT_8:
				return R.drawable.ic_direction_roundabout_8;
			case PASS_PLACE:
				return R.drawable.ic_direction_waypoint;
			default:
				return R.drawable.ic_direction_unknown;
		}
	}
}
