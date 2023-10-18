/*
 * Created by milan on 10.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ToggleChipDefaults
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordingService
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.BodySensorsManager
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.compose.*
import com.asamm.locus.addon.wear.gui.compose.theme.AppTheme
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum
import com.asamm.logger.Logger

/**
 * Main app settings activity.
 */
class MainSettingsActivity : LocusWearActivity() {

    // runs from the menu, not independently
    override val isChildLocusWearActivity: Boolean
        get() = true

    override val initialCommandType: DataPayload<EmptyCommand>?
        get() = null

    override val initialCommandResponseType: DataPath?
        get() = null

    override val isMakeHandshakeOnStart: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
    }

    @Composable
    fun MainContent() {
        AppTheme {
            Column {
                Header()
                Content()
            }
        }
    }

    @Composable
    private fun Header() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colors.primary)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LText(
                text = stringResource(R.string.settings),
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.title2
            )
        }
    }

    @Composable
    private fun ColumnScope.Content() {
        val scrollState = rememberScrollState()
        val hrmState by PreferencesEx.hrmFeatureConfigStateLd.observeAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f, fill = true)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // version info
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            LText(
                text = getString(R.string.version_name_X, versionName),
                modifier = Modifier
                    .padding(all = 8.dp)
            )

            // HRM settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .clickable {
                        onHrmClicked()
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LText(
                    text = stringResource(id = R.string.settings_hrm),
                    modifier = Modifier
                        .weight(1.0f)
                )
                val hrmEnabled = hrmState === FeatureConfigEnum.ENABLED
                Icon(
                    imageVector = ToggleChipDefaults.switchIcon(checked = hrmEnabled),
                    contentDescription = "",
                    modifier = Modifier.padding(start = 8.dp),
                    tint = if (hrmEnabled) MaterialTheme.colors.primary else Color.Gray
                )
            }
            if (PreferencesEx.isDebug) {
                LText(text = "State: $hrmState")
            }
        }
    }

    private fun onHrmClicked() {
        when (PreferencesEx.hrmFeatureConfigState) {
            FeatureConfigEnum.ENABLED -> {
                PreferencesEx.hrmFeatureConfigState = FeatureConfigEnum.DISABLED
                if (TrackRecordingService.isRunning()) {
                    val intent = Intent(this, TrackRecordingService::class.java)
                    intent.action = TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE
                    startService(intent)
                }
            }
            else -> {
                if (BodySensorsManager.checkAndRequestBodySensorsPermission(this)) {
                    // enable because we have a valid permission
                    enableHrm()
                }
            }
        }
    }

    private fun enableHrm() {
        runOnUiThread {
            val hrmState = PreferencesEx.hrmFeatureConfigState
            Logger.d(TAG, "enableHrm(), state: $hrmState")

            // check permission
            if (hrmState === FeatureConfigEnum.NO_PERMISSION) {
                if (BodySensorsManager.checkBodySensorsPermission(this)) {
                    // change just to "not available" to process in next step
                    PreferencesEx.hrmFeatureConfigState = FeatureConfigEnum.NOT_AVAILABLE
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.err_no_hrm_permission),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // check availability
            if (hrmState === FeatureConfigEnum.NOT_AVAILABLE) {
                val tmpHrmConfig = BodySensorsManager.recheckSensorAvailability(this)
                // HRM could have been enabled as side effect of calling recheckSensorAvailability()
                if (tmpHrmConfig === FeatureConfigEnum.NOT_AVAILABLE) {
                    // sensor still not available even after recheck
                    Toast.makeText(
                        this,
                        getString(R.string.err_hrm_sensor_not_available),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            if (hrmState === FeatureConfigEnum.DISABLED) {
                // sensor should be accessible and is only disabled, simply enable it
                PreferencesEx.hrmFeatureConfigState = FeatureConfigEnum.ENABLED
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        BodySensorsManager.handlePermissionResult(this, requestCode)
        enableHrm()
    }

    @Preview(
        widthDp = WEAR_PREVIEW_DEVICE_WIDTH_DP,
        heightDp = WEAR_PREVIEW_DEVICE_HEIGHT_DP,
        apiLevel = WEAR_PREVIEW_API_LEVEL,
        uiMode = WEAR_PREVIEW_UI_MODE,
        backgroundColor = WEAR_PREVIEW_BACKGROUND_COLOR_BLACK,
        showBackground = WEAR_PREVIEW_SHOW_BACKGROUND
    )
    @Composable
    fun WearAppPreview() {
        MainContent()
    }

    companion object {

        // tag for logger
        private const val TAG = "MainSettingsActivity"
    }
}