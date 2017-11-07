package com.asamm.locus.addon.wearables.gui.dispatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.gui.CustomActivity;
import com.asamm.locus.addon.wearables.gui.MapActivity;
import com.asamm.locus.addon.wearables.gui.TrackRecordActivity;

/**
 * Helper factory responsible for creating application Activities
 * based on saved state (or after clicking menu buttons)
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class ActivityDispatchFactory {

    private ActivityDispatchFactory() {}

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
                activityNameSimple = MapActivity.class.getSimpleName();
                break;
            case R.id.button_main_menu_02_track:
                activityNameSimple = TrackRecordActivity.class.getSimpleName();
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
        String savedState = p.getString(CustomActivity.PREF_LAST_ACTIVITY, "");
        return createIntentFromActivityName(c, savedState);
    }

    private static Intent createIntentFromActivityName(Context c, String activityNameSimple) {
        final Class<?> activity;
        if (TrackRecordActivity.class.getSimpleName().equals(activityNameSimple)) {
            activity = TrackRecordActivity.class;
        } else if (MapActivity.class.getSimpleName().equals(activityNameSimple)) {
            activity = MapActivity.class;
        } else {
            activity = null;
        }
        return activity == null ? null : new Intent(c, activity);
    }
}
