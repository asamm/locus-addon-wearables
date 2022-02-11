package com.asamm.locus.addon.wear.features.launcher

import android.content.Intent
import android.os.Bundle
import com.asamm.locus.addon.wear.application.PreferencesEx
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate
import locus.api.utils.Logger

/**
 * Main start activity.
 */
class LauncherActivity : LocusWearActivity() {

    override val initialCommandType: DataPayload<*>
        get() = TODO("Not yet implemented")

    override val initialCommandResponseType: DataPath
        get() = TODO("Not yet implemented")

    override fun registerHwKeyActions(delegate: LocusWearActivityHwKeyDelegate?) {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // detect that application was started from main application menu
        if (intent != null
                && intent.action != null
                && "android.intent.action.MAIN" == intent.action
                && (intent.categories == null || intent.categories.contains("android.intent.category.LAUNCHER"))) {
            Logger.logD(TAG, "onCreate($savedInstanceState), new start")
            PreferencesEx.mapOffsetX = 0
            PreferencesEx.mapOffsetY = 0
            PreferencesEx.mapBearing = 0.toShort()
        }

        // set state
        super.onCreate(savedInstanceState)

        // dispatch to correct activity - if activities are different, then application was restored
        // do not apply for child activities, which are not persisted when opened
        val i = Intent(this, PreferencesEx.lastActivity)
        startActivity(i)
        finish()
    }

    companion object {

        // tag for logger
        private const val TAG = "LauncherActivity"
    }
}