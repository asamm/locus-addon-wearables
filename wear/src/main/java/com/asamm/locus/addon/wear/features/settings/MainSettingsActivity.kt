/*
 * Created by milan on 10.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.features.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand
import com.asamm.locus.addon.wear.features.trackRecord.TrackRecordingService
import com.asamm.locus.addon.wear.features.trackRecord.recording.sensors.RecordingSensorManager
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum
import com.google.android.material.switchmaterial.SwitchMaterial

open class MainSettingsActivity : LocusWearActivity() {

    // runs from the menu, not independently
    override val isChildLocusWearActivity: Boolean
        get() = true

    override val initialCommandType: DataPayload<EmptyCommand>?
        get() = null

    override val initialCommandResponseType: DataPath?
        get() = null

    override val isMakeHandshakeOnStart: Boolean
        get() = false

    // switcher for HRM
    private lateinit var switchHrm: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_settings)

        // setup header
        findViewById<TextView>(R.id.text_view_screen_header)
                .text = getText(R.string.settings)

        // set version text
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        findViewById<TextView>(R.id.text_view_version)
                .text = getString(R.string.version_name_X, versionName)

        // set HRM toggle
        switchHrm = findViewById(R.id.switch_hrm)
        updateSwitchState()
    }

    private fun updateSwitchState() {
        val hrmConfig = PreferencesEx.getHrmFeatureConfig()
        switchHrm.isChecked = hrmConfig === FeatureConfigEnum.ENABLED
        switchHrm.setTextColor(
                if (hrmConfig !== FeatureConfigEnum.NOT_AVAILABLE) {
                    getColor(R.color.base_dark_primary)
                } else {
                    getColor(R.color.base_light_disabled)
                }
        )
    }

    private val isHrmEnabled: Boolean
        get() {
            val hrmConfig = PreferencesEx.getHrmFeatureConfig()
            return hrmConfig === FeatureConfigEnum.ENABLED
        }

    @Suppress("UNUSED_PARAMETER")
    fun onHrmClicked(view: View) {
        if (isHrmEnabled) {
            PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.DISABLED)
            if (TrackRecordingService.isRunning()) {
                val intent = Intent(this, TrackRecordingService::class.java)
                intent.action = TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE
                startService(intent)
            }
        } else {
            if (RecordingSensorManager.checkAndRequestBodySensorPermission(this)) {
                enableHrm()
            }
        }
        updateSwitchState()
    }

    private fun enableHrm() {
        runOnUiThread {
            val hrmConfig = PreferencesEx.getHrmFeatureConfig()
            if (hrmConfig === FeatureConfigEnum.NO_PERMISSION) {
                if (RecordingSensorManager.checkBodySensorPermission(this)) {
                    PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.NOT_AVAILABLE)
                } else {
                    Toast.makeText(
                            this,
                            getString(R.string.err_no_hrm_permission),
                            Toast.LENGTH_LONG
                    ).show()
                }
            }
            if (hrmConfig === FeatureConfigEnum.NOT_AVAILABLE) {
                val tmpHrmConfig = RecordingSensorManager.recheckSensorAvailability(this)
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
            if (hrmConfig === FeatureConfigEnum.DISABLED) {
                // sensor should be accessible and is only disabled, simply enable it
                PreferencesEx.persistHrmFeatureConfig(FeatureConfigEnum.ENABLED)
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        RecordingSensorManager.handlePermissionResult(this, requestCode, permissions, grantResults)
        enableHrm()
        updateSwitchState()
    }
}