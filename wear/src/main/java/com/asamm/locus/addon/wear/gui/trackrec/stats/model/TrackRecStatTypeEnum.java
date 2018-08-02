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

public enum TrackRecStatTypeEnum implements TrackStatConsumable {
	// serves as a blank placeholder displaying nothing
	BLANK((byte) 0, R.string.empty_string, R.drawable.blank_1px,
			TrackStatConsumable.TscFactory.createBlankConsumable()),
	TOTAL_LENGTH_MOVE((byte) 1, R.string.stat_distance, R.drawable.ic_symbol_distance,
			TrackStatConsumable.TscFactory.createTotalLengthMoveConsumable()),
	TOTAL_TIME((byte) 2, R.string.stat_track_time, R.drawable.ic_symbol_track_time,
			TrackStatConsumable.TscFactory.createTotalTimeConsumable()),
	SPEED_AVG_MOVE((byte) 3, R.string.stat_speed_avg_moving, R.drawable.ic_symbol_speed_avg_move,
			TrackStatConsumable.TscFactory.createAvgSpeedMoveConsumable()),
	SPEED((byte) 4, R.string.stat_speed, R.drawable.ic_symbol_speed,
			TrackStatConsumable.TscFactory.createSpeedMoveConsumable()),
	ELEVATION_UPHILL((byte) 5, R.string.stat_elevation_up, R.drawable.ic_symbol_elevation_up,
			TrackStatConsumable.TscFactory.createElevationUpConsumable()),
	ELEVATION_DOWNHILL((byte) 6, R.string.stat_elevation_down, R.drawable.ic_symbol_elevation_down,
			TrackStatConsumable.TscFactory.createElevationDownConsumable());

	private final byte id;
	private final int mIconId;
	private final TrackStatConsumable mConsumer;
	private final int mNameStringId;

	TrackRecStatTypeEnum(byte id, int nameStringId, int iconId, TrackStatConsumable consumer) {
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

	private static TrackRecStatTypeEnum[] values = values();

	public static TrackRecStatTypeEnum getById(byte id) {
		for(TrackRecStatTypeEnum it : values) {
			if (it.id == id) return it;
		}
		return BLANK;
	}
}
