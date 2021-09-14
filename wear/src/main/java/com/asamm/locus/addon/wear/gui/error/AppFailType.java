package com.asamm.locus.addon.wear.gui.error;

import com.asamm.locus.addon.wear.R;

/**
 * Specificaiton of different types of application failures with reference to err message.
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */

public enum AppFailType {

	UNSUPPORTED_LOCUS_VERSION(R.string.err_locus_version_not_supported),
	/**
	 * General communication fail during normal operation even though device should be connected
	 */
	CONNECTION_FAILED(R.string.err_connection_failed),
	/**
	 * No node currently connected to this watch
	 */
	CONNECTION_ERROR_NODE_NOT_CONNECTED(R.string.err_connection_error_node_not_connected),
	/**
	 * Application is not installed on the device
	 */
	CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE(R.string.err_connection_device_app_not_installed),
	/** Version of phone addon is older than watch addon */
	CONNECTION_ERROR_DEVICE_APP_OUTDATED(R.string.err_connection_device_app_outdated),
	/** Version of phone addon is newer than watch addon */
	CONNECTION_ERROR_WATCH_APP_OUTDATED(R.string.err_connection_watch_app_outdated);


	private final int mErrorMsgId;

	AppFailType(int rStringErrorMessage) {
		this.mErrorMsgId = rStringErrorMessage;
	}

	public int getErrorMsgId() {
		return mErrorMsgId;
	}
}
