package com.asamm.locus.addon.wear.gui.dispatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.gui.CustomActivityOld;
import com.asamm.locus.addon.wear.gui.MapActivityOld;
import com.asamm.locus.addon.wear.gui.TrackRecordActivityOld;

/**
 * Helper factory responsible for creating application Activities
 * based on saved state (or after clicking menu buttons)
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class ActivityDispatchFactory {

    private ActivityDispatchFactory() {
    }

    ;

    /**
     * Create activity start intent for main menu button id
     *
     * @param c context
     * @return Intent to start selected activity or null if selected button is unknown
     */
    public static Intent createForMainMenuButton(Context c, int buttonId) {
        final String activityNameSimple;
        switch (buttonId) {
            case R.id.button_main_menu_01_map:
                activityNameSimple = MapActivityOld.class.getSimpleName();
                break;
            case R.id.button_main_menu_02_track:
                activityNameSimple = TrackRecordActivityOld.class.getSimpleName();
                break;
            default:
                activityNameSimple = "";
                break;
        }
        return createIntentFromActivityName(c, activityNameSimple);
    }

    /**
     * Create activity start intent based on saved state.
     *
     * @param c context
     * @return Intent to start last opened activity or null if
     * MainMenu was opened last or no saved state is available.
     */
    public static Intent createFromSavedState(Context c) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
        String savedState = p.getString(CustomActivityOld.PREF_LAST_ACTIVITY, "");
        return createIntentFromActivityName(c, savedState);
    }

    private static Intent createIntentFromActivityName(Context c, String activityNameSimple) {
        final Class<?> activity;
        if (TrackRecordActivityOld.class.getSimpleName().equals(activityNameSimple)) {
            activity = TrackRecordActivityOld.class;
        } else if (MapActivityOld.class.getSimpleName().equals(activityNameSimple)) {
            activity = MapActivityOld.class;
        } else {
            activity = null;
        }
        return activity == null ? null : new Intent(c, activity);
    }
}
