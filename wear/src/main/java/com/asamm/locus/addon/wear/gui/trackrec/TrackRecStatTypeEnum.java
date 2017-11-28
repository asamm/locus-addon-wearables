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
	TOTAL_LENGTH_MOVE(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createTotalLengthMoveConsumable()),
	TOTAL_TIME(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createTotalTimeConsumable()),
	SPEED_AVG(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createAvgSpeedMoveConsumable()),
	SPEED_MAX(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createMaxSpeedMoveConsumable()),
	ALTITUDE_MAX(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createMaxAltitudeConsumable()),
	ALTITUDE_MIN(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createMinAltitudeConsumable()),
	BLANK(R.drawable.ic_symbol_time, // TODO cejnar blank symbol
			TrackStatConsumable.TscFactory.createBlankConsumable());

	private final int mIconId;
	private final TrackStatConsumable mConsumer;

	private static final boolean DISPLAY_UNITS = true;

	TrackRecStatTypeEnum(int iconId, TrackStatConsumable consumer) {
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
