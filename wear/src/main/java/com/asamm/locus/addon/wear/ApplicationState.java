package com.asamm.locus.addon.wear;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;

/**
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public class ApplicationState {
	private boolean connected = false;
	private HandShakeValue handShakeValue = null;
	private Point screenDimension = new Point();


	ApplicationState(Context c) {
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getSize(screenDimension);
	}

	public boolean isConnected() {
		return connected;
	}

	void setConnected(boolean connected) {
		this.connected = connected;
	}

	public HandShakeValue getHandShakeValue() {
		return handShakeValue;
	}

	void setHandShakeValue(HandShakeValue handShakeValue) {
		this.handShakeValue = handShakeValue;
	}

	public boolean isHandShake() {
		return handShakeValue != null;
	}

	public int getScreenWidth() {
		return screenDimension.x;
	}

	public int getScreenHeight() {
		return screenDimension.y;
	}
}
