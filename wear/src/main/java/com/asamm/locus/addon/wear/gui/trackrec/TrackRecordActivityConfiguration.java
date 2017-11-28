package com.asamm.locus.addon.wear.gui.trackrec;

import com.asamm.locus.addon.wear.gui.custom.TrackStatConsumable;

/**
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */

public class TrackRecordActivityConfiguration {
	public static int MAX_CNT_FIELDS = 6;

	private TrackRecStatTypeEnum[] mStatConfig = new TrackRecStatTypeEnum[MAX_CNT_FIELDS];

	public TrackRecordActivityConfiguration() {
		// TODO cejnar mocked static configuration
		mStatConfig[0] = TrackRecStatTypeEnum.TOTAL_TIME;
		mStatConfig[1] = TrackRecStatTypeEnum.TOTAL_LENGTH_MOVE;
		mStatConfig[2] = TrackRecStatTypeEnum.SPEED_AVG;
		mStatConfig[3] = TrackRecStatTypeEnum.SPEED_MAX;
		mStatConfig[4] = TrackRecStatTypeEnum.ALTITUDE_MAX;
		mStatConfig[5] = TrackRecStatTypeEnum.ALTITUDE_MIN;
	}

	public TrackRecStatTypeEnum getTypeForIdx(int statIdx) {
		TrackRecStatTypeEnum result = TrackRecStatTypeEnum.BLANK;
		if (statIdx < mStatConfig.length && mStatConfig[statIdx] != null) {
			result = mStatConfig[statIdx];
		}
		return result;
	}

	public TrackRecStatTypeEnum[] getStatConfig() {
		return mStatConfig;
	}
}
