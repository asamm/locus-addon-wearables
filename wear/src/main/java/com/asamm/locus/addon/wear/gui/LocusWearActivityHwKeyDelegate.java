package com.asamm.locus.addon.wear.gui;

import android.support.wearable.input.RotaryEncoder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.asamm.locus.addon.wear.gui.custom.HwButtonAction;
import com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import locus.api.utils.Logger;

import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_1_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_1_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_2_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_2_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_3_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.HwButtonActionDescEnum.BTN_3_PRESS;

/**
 * Created by Milan Cejnar on 24.01.2018.
 * Asamm Software, s.r.o.
 */

class LocusWearActivityHwKeyDelegate {

	private HashMap<HwButtonActionDescEnum, HwButtonAction> mHwButtonActions = new HashMap<>();
	private List<Integer> hwKeyCodes = Arrays.asList(new Integer[]{
			KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_3});
	private HwButtonActionDescEnum[] shortPressMapping = new HwButtonActionDescEnum[]{BTN_1_PRESS, BTN_2_PRESS, BTN_3_PRESS};
	private HwButtonActionDescEnum[] longPressMapping = new HwButtonActionDescEnum[]{BTN_1_LONG_PRESS, BTN_2_LONG_PRESS, BTN_3_LONG_PRESS};

	boolean onKeyDown(int keyCode, KeyEvent event) {
		if (hwKeyCodes.contains(keyCode)) {
			event.startTracking();
			return true;
		}
		return false;
	}

	boolean onKeyLongPress(int keyCode, KeyEvent event) {
		int idx = hwKeyCodes.indexOf(keyCode);
		if (idx >= 0) {
			HwButtonAction action = mHwButtonActions.get(longPressMapping[idx]);
			Logger.logD("LocusWearActivityHwKeyDelegate", "long press " + longPressMapping[idx]);
			if (action != null) action.doButtonAction(longPressMapping[idx], event, 0);
			return action != null;
		}
		return false;
	}

	boolean onKeyUp(int keyCode, KeyEvent event) {
		int idx = hwKeyCodes.indexOf(keyCode);
		if (idx >= 0 && (event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
			HwButtonAction action = mHwButtonActions.get(shortPressMapping[idx]);
			Logger.logD("LocusWearActivityHwKeyDelegate", "short press " + shortPressMapping[idx]);
			if (action != null) action.doButtonAction(shortPressMapping[idx], event, 0);
			return action != null;
		}
		return false;
	}

	void registerHwButtonListener(HwButtonActionDescEnum button, HwButtonAction action) {
		if (button != null) mHwButtonActions.put(button, action);
	}

	void registerDefaultRotaryMotionListener(View rootView) {
		if (rootView == null)
			throw new IllegalArgumentException("Got null rootVIew in registerDefaultRotaryMotionListener().");

		rootView.setOnGenericMotionListener((View v, MotionEvent ev) -> {
			if (ev.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(ev)) {
				// Don't forget the negation here
//				float delta = -RotaryEncoder.getRotaryAxisValue(ev) * RotaryEncoder.getScaledScrollFactor(
//						LocusWearActivity.this);
//
//				// Swap these axes if you want to do horizontal scrolling instead
//				scrollBy(0, Math.round(delta));
				// TODO cejnar step rotary input
				Logger.logD("LocusWearActivityHwKeyDelegate", "ROTARY RAW " + (-RotaryEncoder.getRotaryAxisValue(ev)));
				return true;
			}

			return false;
		});
	}
}
