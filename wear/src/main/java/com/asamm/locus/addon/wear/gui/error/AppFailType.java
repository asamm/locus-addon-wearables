package com.asamm.locus.addon.wear.gui.error;

import com.asamm.locus.addon.wear.R;

/**
 * Specificaiton of different types of application failures with reference to err message.
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */

public enum AppFailType {
	UNSUPPORTED_LOCUS_VERSION(R.string.err_locus_version_not_suppoerted),
	PERIODIC_UPDATES_DISABLED(R.string.err_periodic_updates_disabled),
	CONNECTION_FAILED(R.string.err_connection_failed);

	private final int mErrorMsgId;

	AppFailType(int rStringErrorMessage) {
		this.mErrorMsgId = rStringErrorMessage;
	}

	public int getErrorMsgId() {
		return mErrorMsgId;
	}
}
