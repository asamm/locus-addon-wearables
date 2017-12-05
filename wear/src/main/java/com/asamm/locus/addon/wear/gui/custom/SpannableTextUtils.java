package com.asamm.locus.addon.wear.gui.custom;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;

/**
 * Created by Milan Cejnar on 05.12.2017.
 * Asamm Software, s.r.o.
 */

public class SpannableTextUtils {
	/**************************************************/
	// SPANS - TEXTS
	/**************************************************/

	public static final float TEXT_DEF_SIZE = 1.0f;
	public static final int TEXT_DEF_TYPEFACE = Typeface.NORMAL;
	public static final int TEXT_DEF_COLOR = 0;

	public static void addStyledText(SpannableStringBuilder ssb, CharSequence textToAdd, int color) {
		addStyledText(ssb, textToAdd, TEXT_DEF_SIZE, TEXT_DEF_TYPEFACE, color);
	}

	public static void addStyledText(SpannableStringBuilder ssb, CharSequence textToAdd,
									 int typeFace, int color) {
		addStyledText(ssb, textToAdd, TEXT_DEF_SIZE, typeFace, color);
	}

	public static void addStyledText(SpannableStringBuilder ssb, CharSequence textToAdd,
									 float relativeSize, int typeFace, int color) {
		addStyledText(ssb, textToAdd, relativeSize, typeFace, color, false);
	}

	public static void addStyledText(SpannableStringBuilder ssb, CharSequence textToAdd,
									 float relativeSize, int typeFace, int color, boolean strike) {
		addStyledText(ssb, textToAdd, relativeSize, typeFace, color, strike, false);
	}

	public static void addStyledText(SpannableStringBuilder ssb, CharSequence textToAdd,
									 float relativeSize, int typeFace, int color, boolean strike, boolean superScript) {
		// add text to builder
		int startIndex = ssb.length();
		ssb.append(textToAdd);

		// set size
		if (relativeSize != TEXT_DEF_SIZE) {
			ssb.setSpan(new RelativeSizeSpan(relativeSize),
					startIndex, ssb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// set color
		if (color != TEXT_DEF_COLOR) {
			ssb.setSpan(new ForegroundColorSpan(color),
					startIndex, ssb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// set typeFace
		if (typeFace != TEXT_DEF_TYPEFACE) {
			ssb.setSpan(new StyleSpan(typeFace),
					startIndex, ssb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// stroke text also
		if (strike) {
			ssb.setSpan(new StrikethroughSpan(),
					startIndex, ssb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		// subScript text also
		if (superScript) {
			ssb.setSpan(new SuperscriptSpan(),
					startIndex, ssb.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}
}
