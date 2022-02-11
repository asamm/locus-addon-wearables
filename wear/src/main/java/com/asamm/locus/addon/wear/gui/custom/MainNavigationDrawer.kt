/**
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.gui.custom

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import androidx.drawerlayout.widget.DrawerLayout
import androidx.wear.widget.drawer.WearableDrawerView

class MainNavigationDrawer @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WearableDrawerView(context, attrs, defStyleAttr, defStyleRes) {

    override fun onDrawerStateChanged(state: Int) {
        super.onDrawerStateChanged(state)

        // manual hiding of peeking drawer, gets stuck otherwise
        if (state == DrawerLayout.STATE_IDLE && isPeeking) {
            postDelayed(
                { if (isPeeking) controller.closeDrawer() },
                DEFAULT_PEEK_DELAY.toLong()
            )
        }
    }

    companion object {

        private const val DEFAULT_PEEK_DELAY = 1500
    }
}