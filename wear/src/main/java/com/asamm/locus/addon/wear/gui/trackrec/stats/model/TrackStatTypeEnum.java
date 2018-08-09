package com.asamm.locus.addon.wear.gui.trackrec.stats.model;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.TrackStatConsumable;

/**
 * Types of available track recording statistics with supplied
 * description text, description icon and formatting function
 * <p>
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public enum TrackStatTypeEnum implements TrackStatConsumable {
	// serves as a blank placeholder displaying nothing
	BLANK((byte) 0, R.string.empty_string, R.drawable.blank_1px,
			TrackStatConsumable.TscFactory.createBlankConsumable()),
//    BLANK((byte) 0, R.string.stat_blank_info_text, R.drawable.ic_circle_edit,
//            TrackStatConsumable.TscFactory.createBlankConsumable()),
	TOTAL_LENGTH_MOVE((byte) 1, R.string.stat_distance, R.drawable.ic_symbol_distance,
			TrackStatConsumable.TscFactory.createTotalLengthMoveConsumable()),
	TRACK_TIME((byte) 2, R.string.stat_track_time, R.drawable.ic_symbol_track_time,
			TrackStatConsumable.TscFactory.createTotalTimeConsumable()),
	TRACK_TIME_MOVEMENT((byte) 3, R.string.stat_track_time_move, R.drawable.ic_symbol_track_time_move,
			TrackStatConsumable.TscFactory.createTotalTimeMoveConsumable()),

	SPEED((byte) 4, R.string.stat_speed, R.drawable.ic_symbol_speed,
			TrackStatConsumable.TscFactory.createSpeedMoveConsumable()),
	SPEED_AVG((byte) 5, R.string.stat_speed_avg, R.drawable.ic_symbol_speed_avg,
			TrackStatConsumable.TscFactory.createAvgSpeedConsumable(false)),
	SPEED_AVG_MOVE((byte) 6, R.string.stat_speed_avg_moving, R.drawable.ic_symbol_speed_avg_move,
			TrackStatConsumable.TscFactory.createAvgSpeedConsumable(true)),
	SPEED_MAX((byte) 7, R.string.stat_speed_max, R.drawable.ic_symbol_speed_max,
			TrackStatConsumable.TscFactory.createSpeedMaxConsumable()),

	ELEVATION_UPHILL((byte) 8, R.string.stat_elevation_up, R.drawable.ic_symbol_elevation_up,
			TrackStatConsumable.TscFactory.createElevationUpConsumable()),
	ELEVATION_DOWNHILL((byte) 9, R.string.stat_elevation_down, R.drawable.ic_symbol_elevation_down,
			TrackStatConsumable.TscFactory.createElevationDownConsumable()),
	DISTANCE_UPHILL((byte) 10, R.string.stat_distance_up, R.drawable.ic_symbol_distance_up,
			TrackStatConsumable.TscFactory.createDistanceUphillConsumable()),
	DISTANCE_DOWNHILL((byte) 11, R.string.stat_distance_down, R.drawable.ic_symbol_distance_down,
			TrackStatConsumable.TscFactory.createDistanceDownhillConsumable()),
	ALTITUDE_MAX((byte) 12, R.string.stat_altitude_max, R.drawable.ic_symbol_altitude_max,
			TrackStatConsumable.TscFactory.createAltitudeConsumable(TscFactory.TrackStatConsumableModifierEnum.MAX)),
	ALTITUDE_MIN((byte) 13, R.string.stat_altitude_min, R.drawable.ic_symbol_altitude_min,
			TrackStatConsumable.TscFactory.createAltitudeConsumable(TscFactory.TrackStatConsumableModifierEnum.MIN)),
	ALTITUDE((byte) 22, R.string.stat_altitude, R.drawable.ic_symbol_elevation,
			TrackStatConsumable.TscFactory.createAltitudeConsumable(TscFactory.TrackStatConsumableModifierEnum.CURRENT)),

	HEART_RATE((byte) 14, R.string.stat_hrm, R.drawable.ic_symbol_heart,
			TrackStatConsumable.TscFactory.createHrmConsumable(TscFactory.TrackStatConsumableModifierEnum.CURRENT)),
	HEART_RATE_AVG((byte) 15, R.string.stat_hrm_avg, R.drawable.ic_symbol_heart_avg,
			TrackStatConsumable.TscFactory.createHrmConsumable(TscFactory.TrackStatConsumableModifierEnum.AVG)),
	HEART_RATE_MAX((byte) 16, R.string.stat_hrm_max, R.drawable.ic_symbol_heart_max,
			TrackStatConsumable.TscFactory.createHrmConsumable(TscFactory.TrackStatConsumableModifierEnum.MAX)),
	ENERGY((byte) 17, R.string.stat_energy, R.drawable.ic_symbol_energy,
			TrackStatConsumable.TscFactory.createEnergyConsumable()),
	CADENCE_AVG((byte) 18, R.string.stat_cadence_avg, R.drawable.ic_symbol_cadence_avg,
			TrackStatConsumable.TscFactory.createCadenceAvgConsumable()),
	CADENCE_MAX((byte) 19, R.string.stat_cadence_max, R.drawable.ic_symbol_cadence_max,
			TrackStatConsumable.TscFactory.createCadenceMaxConsumable()),
//	PACE((byte) 20, R.string.stat_elevation_up, R.drawable.ic_symbol_cadence,
//			TrackStatConsumable.TscFactory.createElevationUpConsumable()),

	CURRENT_TIME((byte) 21, R.string.stat_current_time, R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createCurrentTimeConsumable()),
	BATTERY((byte) 23, R.string.stat_battery_level, R.drawable.ic_symbol_battery,
			TrackStatConsumable.TscFactory.createBatteryConsumable());

	private final byte id;
	private final int mIconId;
	private final TrackStatConsumable mConsumer;
	private final int mNameStringId;

	TrackStatTypeEnum(byte id, int nameStringId, int iconId, TrackStatConsumable consumer) {
		this.id = id;
		this.mNameStringId = nameStringId;
		this.mIconId = iconId;
		this.mConsumer = consumer;
	}

	@Override
	public ValueUnitContainer consumeAndFormat(TrackRecordingValue rec) {
		return mConsumer.consumeAndFormat(rec);
	}

	public int getIconId() {
		return mIconId;
	}

	public int getNameStringId() {
		return mNameStringId;
	}

	public byte getId() {
		return id;
	}

	/** This list and its items order is used in dashboard item select list activity*/
	public static final TrackStatTypeEnum[] VALUES_FOR_SELECTIONS = {
			TOTAL_LENGTH_MOVE,
			TRACK_TIME,
			TRACK_TIME_MOVEMENT,

			SPEED,
			SPEED_AVG,
			SPEED_AVG_MOVE,
			SPEED_MAX,

			ELEVATION_UPHILL,
			ELEVATION_DOWNHILL,
			DISTANCE_UPHILL,
			DISTANCE_DOWNHILL,
			ALTITUDE_MAX,
			ALTITUDE_MIN,
			ALTITUDE,

			HEART_RATE,
			HEART_RATE_AVG,
			HEART_RATE_MAX,
			ENERGY,
			CADENCE_AVG,
			CADENCE_MAX,

			CURRENT_TIME,
			BATTERY
	};

	private static TrackStatTypeEnum[] values = values();

	public static TrackStatTypeEnum getById(byte id) {
		for(TrackStatTypeEnum it : values) {
			if (it.id == id) return it;
		}
		return BLANK;
	}
}
