package com.asamm.locus.addon.wear.features.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.features.settings.PreferencesEx
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.logger.Logger
import com.asamm.loggerV2.logW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main start activity.
 */
class LauncherActivity : LocusWearActivity() {

    override val initialCommandType: DataPayload<*>?
        get() = null

    override val initialCommandResponseType: DataPath?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // setup splash screen
        installSplashScreen()

        // forward to parent
        super.onCreate(savedInstanceState)

        // detect that application was started from main application menu
        if (intent != null
                && intent.action != null
                && "android.intent.action.MAIN" == intent.action
                && (intent.categories == null || intent.categories.contains("android.intent.category.LAUNCHER"))) {
            logW { "onCreate($savedInstanceState), new start" }
            PreferencesEx.mapOffsetX = 0
            PreferencesEx.mapOffsetY = 0
            PreferencesEx.mapBearing = 0.toShort()
        }

        // dispatch to correct activity - if activities are different, then application was restored
        // do not apply for child activities, which are not persisted when opened
        val i = Intent(this, PreferencesEx.lastActivity)
        startActivity(i)
        finish()
    }
}