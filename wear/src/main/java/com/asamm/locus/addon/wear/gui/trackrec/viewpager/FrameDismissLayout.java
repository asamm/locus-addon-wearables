package com.asamm.locus.addon.wear.gui.trackrec.viewpager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by Milan Cejnar on 28.11.2017.
 * Asamm Software, s.r.o.
 */

public class FrameDismissLayout extends FrameLayout{
	public FrameDismissLayout(@NonNull Context context) {
		super(context);
	}

	public FrameDismissLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public FrameDismissLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public FrameDismissLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		super.onInterceptTouchEvent(ev);
		return true;
	}
}
