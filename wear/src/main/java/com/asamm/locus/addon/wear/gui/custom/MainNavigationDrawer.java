package com.asamm.locus.addon.wear.gui.custom;

import android.content.Context;
import android.support.wear.widget.drawer.WearableDrawerView;
import android.util.AttributeSet;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */

public class MainNavigationDrawer extends WearableDrawerView {

	private static final String TAG = "MainNavigationDrawer";

	public MainNavigationDrawer(Context context) {
		this(context, (AttributeSet) null);
	}

	public MainNavigationDrawer(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MainNavigationDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public MainNavigationDrawer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void onDrawerStateChanged(int state) {
		super.onDrawerStateChanged(state);
		Logger.logD(TAG, "Drawer state changed " + state);
	}
}
