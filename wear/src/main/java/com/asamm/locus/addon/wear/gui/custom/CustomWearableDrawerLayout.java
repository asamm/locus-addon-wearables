package com.asamm.locus.addon.wear.gui.custom;

import android.content.Context;
import android.os.Build;
import androidx.wear.widget.drawer.WearableDrawerLayout;
import android.util.AttributeSet;

import java.lang.reflect.Field;

/**
 * Created by Milan Cejnar on 15.12.2017.
 * Asamm Software, s.r.o.
 */

public class CustomWearableDrawerLayout extends WearableDrawerLayout {

	private static final float NAV_DRAWER_PULL_DOWN_AREA_SCALE = 1.2f;

	public CustomWearableDrawerLayout(Context context) {
		this(context, null);
	}

	public CustomWearableDrawerLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CustomWearableDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public CustomWearableDrawerLayout(
			Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		// Area now seems to work correctly even without enlarging it manually,
		// possibly fixed in OS update, because last time I check it was quite hard to open up the drawer

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
			// try to make top navigation drawer trigger area bigger
			try {
				Field topDragger = getClass().getSuperclass().getDeclaredField("mBottomDrawerDragger");
				topDragger.setAccessible(true);
				Object bottomDraggerInstance = topDragger.get(this);
				Field edgeSize = bottomDraggerInstance.getClass().getDeclaredField("mEdgeSize");
				edgeSize.setAccessible(true);
				int edgeSizePx = edgeSize.getInt(bottomDraggerInstance);
				edgeSize.set(bottomDraggerInstance, (int) (edgeSizePx * NAV_DRAWER_PULL_DOWN_AREA_SCALE + 0.5f));
			} catch (Exception e) {
			}
		}
	}
}
