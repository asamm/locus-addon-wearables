package com.asamm.locus.addon.wear.gui.trackrec;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.TrackStatConsumable;

import locus.api.objects.extra.TrackStats;

/**
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public enum TrackRecStatTypeEnum {
	TOTAL_LENGTH_MOVE(R.string.stat_distance, R.drawable.ic_symbol_distance,
			TrackStatConsumable.TscFactory.createTotalLengthMoveConsumable()),
	TOTAL_TIME(R.string.stat_track_time, R.drawable.ic_symbol_track_time,
			TrackStatConsumable.TscFactory.createTotalTimeConsumable()),
	SPEED_AVG_MOVE(R.string.stat_speed_avg_moving, R.drawable.ic_symbol_speed_avg_move,
			TrackStatConsumable.TscFactory.createAvgSpeedMoveConsumable()),
	SPEED(R.string.stat_speed, R.drawable.ic_symbol_speed,
			TrackStatConsumable.TscFactory.createSpeedMoveConsumable()),
	ELEVATION_UPHILL(R.string.stat_elevation_up,R.drawable.ic_symbol_elevation_up,
		TrackStatConsumable.TscFactory.createElevationUpConsumable()),
	ELEVATION_DOWNHILL(R.string.stat_elevation_down,R.drawable.ic_symbol_elevation_down,
			TrackStatConsumable.TscFactory.createElevationDownConsumable()),
	BLANK(R.string.empty_string, R.drawable.blank_1px,
			TrackStatConsumable.TscFactory.createBlankConsumable());

	private final int mIconId;
	private final TrackStatConsumable mConsumer;
	private final int mNameStringId;

	private static final boolean DISPLAY_UNITS = true;

	TrackRecStatTypeEnum(int nameStringId, int iconId, TrackStatConsumable consumer) {
		this.mNameStringId = nameStringId;
		this.mIconId = iconId;
		this.mConsumer = consumer;
	}

	public String consumeAndFormat(TrackRecordingValue rec) {
		return mConsumer.consumeAndFormat(rec, !DISPLAY_UNITS);
	}

	public int getIconId() {
		return mIconId;
	}
}
