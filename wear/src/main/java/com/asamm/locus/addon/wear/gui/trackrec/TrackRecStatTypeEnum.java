package com.asamm.locus.addon.wear.gui.trackrec;

import android.app.Activity;

import com.asamm.locus.addon.wear.R;
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
	BLANK(R.drawable.ic_symbol_time,
			TrackStatConsumable.TscFactory.createTotalLengthMoveConsumable());

	private final int mIconId;
	private final TrackStatConsumable mConsumer;

	private static final boolean DISPLAY_UNITS = true;

	TrackRecStatTypeEnum(int iconId, TrackStatConsumable consumer) {
		this.mIconId = iconId;
		this.mConsumer = consumer;
	}

	public String consumeAndFormat(Activity context, TrackStats stats) {
		return mConsumer.consumeAndFormat(context, stats, DISPLAY_UNITS);
	}

	public int getIconId() {
		return mIconId;
	}
}
