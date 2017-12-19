package com.asamm.locus.addon.wear.common.communication;

import locus.api.android.utils.LocusUtils;

/**
 * Common constants to use for both device and wear modules
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public interface Const {
	byte ZOOM_UNKOWN = Byte.MIN_VALUE;
	int ZOOM_MIN = 0;
	int ZOOM_MAX = 22;
	int ZOOM_DEFAULT = 15;

	// minimal version code of Locus
	LocusUtils.VersionCode LOCUS_MIN_VERSION_CODE = LocusUtils.VersionCode.UPDATE_13;

	String ANDROID_MARKET_APP_URI = "market://details?id=com.asamm.locus.addon.wear";

}
