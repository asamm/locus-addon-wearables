package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.support.v4.view.ViewPager;
import android.view.View;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 27.11.2017.
 * Asamm Software, s.r.o.
 */


public class VerticalViewPagerTransition implements ViewPager.PageTransformer {
	private static final float MIN_SCALE = 0.75f;

	@Override
	public void transformPage(View view, float position) {
//		float alpha = 0;
//		if (0 <= position && position <= 1) {
//			alpha = 1 - position;
//		} else if (-1 < position && position < 0) {
//			alpha = position + 1;
//		}
//		view.setAlpha(alpha);
		view.setTranslationX(view.getWidth() * -position);
		float yPosition = position * view.getHeight();
		view.setTranslationY(yPosition);
	}
}
