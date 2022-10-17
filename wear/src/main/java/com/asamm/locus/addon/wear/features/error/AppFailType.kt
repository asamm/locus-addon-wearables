/**
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.features.error

import androidx.annotation.StringRes
import com.asamm.locus.addon.wear.R

/**
 * Specification of different types of application failures with reference to err message.
 */
enum class AppFailType(@StringRes val headerTitle: Int, val errorMsgId: Int) {

    UNSUPPORTED_LOCUS_VERSION(
            headerTitle = R.string.app_name,
            errorMsgId = R.string.err_locus_version_not_supported
    ),

    /**
     * General communication fail during normal operation even though device should be connected
     */
    CONNECTION_FAILED(
            headerTitle = R.string.title_activity_error,
            errorMsgId = R.string.err_connection_failed
    ),

    /**
     * No node currently connected to this watch
     */
    CONNECTION_ERROR_NODE_NOT_CONNECTED(
            headerTitle = R.string.title_activity_error,
            errorMsgId = R.string.err_connection_error_node_not_connected
    ),

    /**
     * Application is not installed on the device.
     */
    CONNECTION_ERROR_APP_NOT_INSTALLED_ON_DEVICE(
            headerTitle = R.string.app_name,
            errorMsgId = R.string.err_connection_device_app_not_installed
    ),

    /**
     * Version of phone addon is older than watch addon.
     */
    CONNECTION_ERROR_DEVICE_APP_OUTDATED(
            headerTitle = R.string.app_name,
            errorMsgId = R.string.err_connection_device_app_outdated
    ),

    /**
     * Version of phone addon is newer than watch addon.
     */
    CONNECTION_ERROR_WATCH_APP_OUTDATED(
            headerTitle = R.string.app_name,
            errorMsgId = R.string.err_connection_watch_app_outdated
    );
}