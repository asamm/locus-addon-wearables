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
	/** General communication fail during normal operationg (after handshake) */
	CONNECTION_FAILED(R.string.err_connection_failed),
	/** No node currently connected to this watch */
	CONNECTION_ERROR_NODE_NOT_CONNECTED(R.string.err_connection_error_node_not_connected),
	/** Initial communication failed */
	CONNECTION_ERROR_HANDSHAKE_FAILED(R.string.err_connection_error_node_not_connected);

	private final int mErrorMsgId;

	AppFailType(int rStringErrorMessage) {
		this.mErrorMsgId = rStringErrorMessage;
	}

	public int getErrorMsgId() {
		return mErrorMsgId;
	}
}
