package com.asamm.locus.addon.wear.gui.custom;


import android.view.View;

import locus.api.utils.Logger;

/**
 * Created by menion on 01/10/2017.
 * This code is part of Locus project from Asamm Software, s. r. o.
 */

public class WearMapActionMoveFling {

	@FunctionalInterface
	public interface OffsetUpdatable {
		public void updateOffset(int deltaX, int deltaY, boolean lastUpdate);
	}

	private final OffsetUpdatable mOffsetUpdatable;
	// tag for logger
	private static final String TAG = "WearMapActionMoveFling";

	// duration between animation steps  (2 frames to get 30 fps)
	private static final long ANIMATION_STEP = 32L;

	// running thread
	private volatile Thread mThread;

	// last move change in X dimension
	private float lastDX;
	// last move change in Y dimension
	private float lastDY;

	/**
	 * Create move-fling action.
	 *
	 * @param velocityX velocity along X axis
	 * @param velocityY velocity along Y axis
	 */
	public WearMapActionMoveFling(float velocityX, float velocityY, OffsetUpdatable onUpdate) {
		lastDX = velocityX / -50.0f;
		lastDY = velocityY / -50.0f;
		this.mOffsetUpdatable = onUpdate != null ? onUpdate : (x, y, l) -> {};
	}

	public void cancel() {
		mThread = null;
	}

	public void start(View map) {
		mThread = new Thread(() -> {
			try {
				boolean lastMove = false;
				while (mThread == Thread.currentThread() && !lastMove) {
					float newDx = lastDX * 63.0f / 100.0f;
					float newDy = lastDY * 63.0f / 100.0f;
					lastMove = !(Math.abs(lastDX) > 1 || Math.abs(lastDY) > 1);
					mOffsetUpdatable.updateOffset((int)(lastDX + 0.5f), (int)(lastDY + 0.5f), lastMove);
					lastDX = newDx;
					lastDY = newDy;

					// sleep a while
					Thread.sleep(ANIMATION_STEP);
				}
			} catch (Exception e) {
				Logger.logE(TAG, "startPrivate(" + map + ")", e);
			}
		});
		mThread.start();
	}
}

