package com.asamm.locus.addon.wear.common.communication;

import locus.api.android.utils.LocusUtils;

/**
 * Common constants to use for both device and wear modules
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public interface Const {
	int ZOOM_UNKONWN = Integer.MIN_VALUE;
	int ZOOM_MIN = 0;
	int ZOOM_MAX = 22;

	// minimal version code of Locus
	// TODO cejnar fill latest locus version before production
	LocusUtils.VersionCode LOCUS_MIN_VERSION_CODE = LocusUtils.VersionCode.UPDATE_13;
}
