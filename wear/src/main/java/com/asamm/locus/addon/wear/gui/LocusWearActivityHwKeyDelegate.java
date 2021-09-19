package com.asamm.locus.addon.wear.gui;

import android.app.Activity;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.wearable.input.RotaryEncoderHelper;

import com.asamm.locus.addon.wear.ApplicationMemoryCache;
import com.asamm.locus.addon.wear.application.AppPreferencesManager;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAction;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_1_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_1_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_2_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_2_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_3_LONG_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.BTN_3_PRESS;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.ROTARY_DOWN;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum.ROTARY_UP;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum.BTN_ACTION_DOWN;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum.BTN_ACTION_PRIMARY_OR_UP;
import static com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum.BTN_ACTION_SECONDARY;

/**
 * Delegate object for handling HW buttons and rotatry input
 * Created by Milan Cejnar on 24.01.2018.
 * Asamm Software, s.r.o.
 */

public interface LocusWearActivityHwKeyDelegate {

	boolean onKeyDown(int keyCode, KeyEvent event);

	boolean onKeyLongPress(int keyCode, KeyEvent event);

	boolean onKeyUp(int keyCode, KeyEvent event);

	/**
	 * This method can be used to manually set actions for any HW button press/long press
	 *
	 * @param button
	 * @param action
	 */
	void registerHwButtonListener(HwButtonActionDescEnum button, HwButtonAction action);

	/**
	 * This function returns recommended HW button mapping for abstract autodetect actions.
	 *
	 * @param adAction
	 * @return
	 */
	public HwButtonActionDescEnum getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum adAction);

	boolean isUseHwButtons();

	/**
	 * Currently only used to temporarily disable  delegate right before screen switch.
	 */
	public void setUseHwButtons(boolean enabled);

	void registerDefaultRotaryMotionListener(View rootView);

	/**
	 * @return number of available multifunction buttons ranging from 0 to 3
	 */
	int getNumMultifunctionButtons();

	/**
	 * Delegate factory. If Hw button support is disabled, returns dummy delegate with no sideeffects
	 */
	class Factory {
		public static LocusWearActivityHwKeyDelegate createDelegate(LocusWearActivity ctx) {
			if (AppPreferencesManager.isUseHwButtons(ctx)) {
				return new LocusWearActivityHwKeyDelegateImpl(ctx);
			} else {
				// return dummy key delegate if hw keys are disabled
				return new LocusWearActivityHwKeyDelegate() {
					@Override
					public boolean onKeyDown(int keyCode, KeyEvent event) {
						return false;
					}

					@Override
					public boolean onKeyLongPress(int keyCode, KeyEvent event) {
						return false;
					}

					@Override
					public boolean onKeyUp(int keyCode, KeyEvent event) {
						return false;
					}

					@Override
					public void registerHwButtonListener(HwButtonActionDescEnum button, HwButtonAction action) {

					}

					@Override
					public void registerDefaultRotaryMotionListener(View rootView) {

					}

					@Override
					public HwButtonActionDescEnum getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum adAction) {
						return HwButtonActionDescEnum.BTN_1_PRESS; // dummy return
					}

					@Override
					public boolean isUseHwButtons() {
						return false;
					}

					@Override
					public void setUseHwButtons(boolean enabled) {
					}

					@Override
					public int getNumMultifunctionButtons() {
						return 0;
					}
				};
			}
		}
	}

	class LocusWearActivityHwKeyDelegateImpl implements LocusWearActivityHwKeyDelegate {
		private Activity mContext;
		private HashMap<HwButtonActionDescEnum, HwButtonAction> mHwButtonActions =
				new HashMap<>(HwButtonActionDescEnum.values().length);
		private List<Integer> hwKeyCodes = Arrays.asList(new Integer[]{
				KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_3});
		private HwButtonActionDescEnum[] shortPressMapping =
				new HwButtonActionDescEnum[]{BTN_1_PRESS, BTN_2_PRESS, BTN_3_PRESS};
		private HwButtonActionDescEnum[] longPressMapping =
				new HwButtonActionDescEnum[]{BTN_1_LONG_PRESS, BTN_2_LONG_PRESS, BTN_3_LONG_PRESS};
		// maps AutoDetect Action to HwButtonACtion
		private HashMap<HwButtonAutoDetectActionEnum, HwButtonActionDescEnum> autoDetectActionMapping =
				new HashMap<>(HwButtonAutoDetectActionEnum.values().length);

		private int mNumMultifunctionButtons = 0;
		private Double mRotaryAccumulator = 0.0;
		private final int mScreenHeight;
		// used only to temporarily disable while switching screens
		private boolean mUseHwButtons = true;
		// used to avoid fake short press detections when holding down a button and switching screens
		private boolean firstKeyDownReceived = false;

		private LocusWearActivityHwKeyDelegateImpl(LocusWearActivity ctx) {
			this.mContext = ctx;
			mScreenHeight = ctx.getMainApplication().getCache().getScreenHeight();
			doAutoDetectHwActions(ctx);
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			if (hwKeyCodes.contains(keyCode)) {
				firstKeyDownReceived = true;
				event.startTracking();
				return true;
			}
			return false;
		}

		@Override
		public boolean onKeyLongPress(int keyCode, KeyEvent event) {
			int idx = hwKeyCodes.indexOf(keyCode);
			if (idx >= 0) {
				HwButtonAction action = mHwButtonActions.get(longPressMapping[idx]);
//				Logger.logD("LocusWearActivityHwKeyDelegate", "long press " + longPressMapping[idx]);
				if (action != null) action.doButtonAction();
				return action != null;
			}
			return false;
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			int idx = hwKeyCodes.indexOf(keyCode);
			if (isUseHwButtons() && firstKeyDownReceived &&
					idx >= 0 && (event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
				HwButtonAction action = mHwButtonActions.get(shortPressMapping[idx]);
//				Logger.logD("LocusWearActivityHwKeyDelegate", "short press " + shortPressMapping[idx]);
				if (action != null) action.doButtonAction();
				return action != null;
			}
			return false;
		}

		@Override
		public void registerHwButtonListener(HwButtonActionDescEnum button, HwButtonAction action) {
			if (button != null) mHwButtonActions.put(button, action);
		}

		@Override
		public void registerDefaultRotaryMotionListener(View rootView) {
			if (rootView == null)
				throw new IllegalArgumentException("Got null rootVIew in registerDefaultRotaryMotionListener().");

			rootView.setOnGenericMotionListener((View v, MotionEvent ev) -> {
				if (ev.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoderHelper.isFromRotaryEncoder(ev)) {
					mRotaryAccumulator += -RotaryEncoderHelper.getRotaryAxisValue(ev) * RotaryEncoderHelper.getScaledScrollFactor(mContext);
					float triggerLimit = mScreenHeight / 3.0f;
					if (Math.abs(mRotaryAccumulator) > triggerLimit) {
						HwButtonActionDescEnum actionDesc =
								mRotaryAccumulator < 0 ? ROTARY_UP : ROTARY_DOWN;
						HwButtonAction action = mHwButtonActions.get(actionDesc);
						mRotaryAccumulator %= triggerLimit;
						if (action != null) action.doButtonAction();
					}
					return true;
				}

				return false;
			});
		}

		@Override
		public HwButtonActionDescEnum getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum adAction) {
			return autoDetectActionMapping.get(adAction);
		}

		@Override
		public boolean isUseHwButtons() {
			return mUseHwButtons;
		}

		@Override
		public void setUseHwButtons(boolean enabled) {
			mUseHwButtons = enabled;
		}

		/**
		 * Perform autodetection of default actions to HW button actions
		 */
		private void doAutoDetectHwActions(LocusWearActivity ctx) {
			WearableButtons.ButtonInfo stem1 = WearableButtons.getButtonInfo(ctx, KeyEvent.KEYCODE_STEM_1);
			WearableButtons.ButtonInfo stem2 = WearableButtons.getButtonInfo(ctx, KeyEvent.KEYCODE_STEM_2);
			WearableButtons.ButtonInfo stem3 = WearableButtons.getButtonInfo(ctx, KeyEvent.KEYCODE_STEM_3);
			// no multifunction buttons available
			mNumMultifunctionButtons = 0;
			if (stem1 == null) {
				return;
			}
			// only single button available
			if (stem2 == null) {
				mNumMultifunctionButtons = 1;
				autoDetectActionMapping.put(BTN_ACTION_PRIMARY_OR_UP, BTN_1_PRESS);
				autoDetectActionMapping.put(BTN_ACTION_SECONDARY, BTN_1_LONG_PRESS);
				autoDetectActionMapping.put(BTN_ACTION_DOWN, null);
				return;
			}
			// two buttons available
			if (stem3 == null) {
				mNumMultifunctionButtons = 2;
				autoDetectActionMapping.put(BTN_ACTION_PRIMARY_OR_UP, BTN_1_PRESS);
				autoDetectActionMapping.put(BTN_ACTION_SECONDARY, BTN_1_LONG_PRESS);
				autoDetectActionMapping.put(BTN_ACTION_DOWN, BTN_2_PRESS);
				return;
			}
			mNumMultifunctionButtons = 3;
			ApplicationMemoryCache appMem = ctx.getMainApplication().getCache();
			int w = appMem.getScreenWidth();
			int h = appMem.getScreenHeight();

			WearableButtons.ButtonInfo up;
			WearableButtons.ButtonInfo down;
			WearableButtons.ButtonInfo third;

			if (checkYSymmetry(stem2, stem3, h)) {
				up = stem2;
				down = stem3;
				third = stem1;
			} else if (checkYSymmetry(stem1, stem3, h)) {
				up = stem1;
				down = stem3;
				third = stem2;
			} else {
				up = stem1;
				down = stem2;
				third = stem3;
			}
			if (up.getY() > down.getY()) {
				WearableButtons.ButtonInfo tmp = up;
				up = down;
				down = tmp;
			}

			autoDetectActionMapping.put(BTN_ACTION_PRIMARY_OR_UP, HwButtonActionDescEnum.findByProperties(up.getKeycode(), false));
			autoDetectActionMapping.put(BTN_ACTION_DOWN, HwButtonActionDescEnum.findByProperties(down.getKeycode(), false));
			autoDetectActionMapping.put(BTN_ACTION_SECONDARY, HwButtonActionDescEnum.findByProperties(third.getKeycode(), false));
		}

		private boolean checkYSymmetry(WearableButtons.ButtonInfo b1, WearableButtons.ButtonInfo b2, int screenHeight) {
			// arbitrary tolerance in case buttons are not perfectly symmetric
			int tolerancePx = 10;
			if (Math.abs(b1.getX() - b2.getX()) > tolerancePx)
				return false;
			int center = screenHeight / 2; //+-1px error
			return Math.abs(Math.abs(b1.getY() - center) - Math.abs(b2.getY() - center)) <= tolerancePx;
		}

		@Override
		public int getNumMultifunctionButtons() {
			return mNumMultifunctionButtons;
		}
	}
}
