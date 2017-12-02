package com.asamm.locus.addon.wear.gui.trackrec.configuration;

import android.content.Context;

import com.asamm.locus.addon.wear.gui.trackrec.recording.TrackRecStatTypeEnum;

import java.util.Arrays;

/**
 * Class for representation of Track recording activity statistics layout configuration
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordActivityConfiguration {
	public static int MAX_CNT_FIELDS = 6;

	private TrackRecStatTypeEnum[] mStatConfig = new TrackRecStatTypeEnum[MAX_CNT_FIELDS];
	private static final TrackRecStatTypeEnum[] DEFAULT_CONFIG = new TrackRecStatTypeEnum[]{
			TrackRecStatTypeEnum.TOTAL_TIME, TrackRecStatTypeEnum.TOTAL_LENGTH_MOVE,
			TrackRecStatTypeEnum.SPEED, TrackRecStatTypeEnum.SPEED_AVG_MOVE,
			TrackRecStatTypeEnum.ELEVATION_DOWNHILL, TrackRecStatTypeEnum.ELEVATION_UPHILL
	};

	private TrackRecordActivityConfiguration() {
		mStatConfig = Arrays.copyOf(DEFAULT_CONFIG, DEFAULT_CONFIG.length);
	}

	public TrackRecStatTypeEnum getStatConfigAtPosition(int idx) {
		if (idx < 0 || idx >= mStatConfig.length) {
			return TrackRecStatTypeEnum.BLANK;
		} else {
			return mStatConfig[idx];
		}
	}

	public static TrackRecordActivityConfiguration loadConfiguration(Context ctx) {
		return new TrackRecordActivityConfiguration();
	}
}
