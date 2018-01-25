package com.asamm.locus.addon.wear.gui.custom.hwcontrols;

import android.view.KeyEvent;

/**
 * Created by Milan Cejnar on 24.01.2018.
 * Asamm Software, s.r.o.
 */

public enum HwButtonActionDescEnum {
	ROTARY_UP(-2, false),
	ROTARY_DOWN(-1, false),
	BTN_1_PRESS(KeyEvent.KEYCODE_STEM_1, false),
	BTN_2_PRESS(KeyEvent.KEYCODE_STEM_2, false),
	BTN_3_PRESS(KeyEvent.KEYCODE_STEM_3, false),
	BTN_1_LONG_PRESS(KeyEvent.KEYCODE_STEM_1, true),
	BTN_2_LONG_PRESS(KeyEvent.KEYCODE_STEM_2, true),
	BTN_3_LONG_PRESS(KeyEvent.KEYCODE_STEM_3, true);

	private static final HwButtonActionDescEnum[] VALUES = values();// to avoid recreating array

	private final boolean longPress;
	private final int keyCode;

	HwButtonActionDescEnum(int keyCode, boolean isLongPress) {

		this.keyCode = keyCode;
		this.longPress = isLongPress;
	}

	public static HwButtonActionDescEnum findByProperties(int keyCode, boolean longPress) {
		for (HwButtonActionDescEnum hb : VALUES) {
			if (hb.keyCode == keyCode && hb.longPress == longPress) {
				return hb;
			}
		}
		return null;
	}
}
