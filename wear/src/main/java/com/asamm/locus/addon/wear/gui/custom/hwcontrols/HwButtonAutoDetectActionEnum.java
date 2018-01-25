package com.asamm.locus.addon.wear.gui.custom.hwcontrols;

/**
 * Created by Milan Cejnar on 25.01.2018.
 * Asamm Software, s.r.o.
 */

/**
 * Enum to abstract from physical button indexes to abstract logical actions used
 * by HW buttons autodetect
 */
public enum HwButtonAutoDetectActionEnum {
	BTN_ACTION_PRIMARY_OR_UP, // this is usually mapped to the first button
	BTN_ACTION_DOWN, // this should be mapped to the button symmetrical to the first button
	BTN_ACTION_SECONDARY, // this is mapped to the third button or as PRIMARY button long press
}
