package com.asamm.locus.addon.wear.gui.custom.hwcontrols;

import android.view.InputEvent;

/**
 * Created by Milan Cejnar on 24.01.2018.
 * Asamm Software, s.r.o.
 */
@FunctionalInterface
public interface HwButtonAction {
	/**
	 * @param hwButton button that caused this event
	 * @param event source input event
	 * @param amount optional parameter, only has meaning for non-binary actions
	 */
	void doButtonAction(HwButtonActionDescEnum hwButton, InputEvent event, float amount);
}
