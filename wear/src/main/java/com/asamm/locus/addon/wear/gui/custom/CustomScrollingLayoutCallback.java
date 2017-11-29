package com.asamm.locus.addon.wear.gui.custom;

import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.view.View;

/**
 * Custom scrolling for recycler lists
 * Created by Milan Cejnar on 29.11.2017.
 * Asamm Software, s.r.o.
 */


public class CustomScrollingLayoutCallback extends WearableLinearLayoutManager.LayoutCallback {
	/**
	 * How much should we scale the icon at most.
	 */
	private static final float MAX_ICON_PROGRESS = 0.65f;

	private float mProgressToCenter;

	@Override
	public void onLayoutFinished(View child, RecyclerView parent) {
		// Figure out % progress from top to bottom
		float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
		float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

		// Normalize for center
		mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);
		// Adjust to the maximum scale
		mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS);

		child.setScaleX(1 - mProgressToCenter);
		child.setScaleY(1 - mProgressToCenter);
	}
}
