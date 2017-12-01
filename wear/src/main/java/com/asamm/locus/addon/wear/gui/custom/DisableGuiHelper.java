package com.asamm.locus.addon.wear.gui.custom;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.asamm.locus.addon.wear.R;

/**
 * Helper for creating grayed "disabled' drawables for icons
 * Created by Milan Cejnar on 23.11.2017.
 * Asamm Software, s.r.o.
 */

public class DisableGuiHelper {
	public static Drawable getImageWithDisabled(Context ctx, Bitmap img) {
		// prepare drawables
		Resources res = ctx.getResources();

		Drawable dEnabled = new BitmapDrawable(res, img);
		Drawable dDisabled = new BitmapDrawable(res, img.copy(Bitmap.Config.ARGB_8888, true));
		dDisabled = getImageWithTint(dDisabled, ContextCompat.getColor(ctx, R.color.base_disabled));

		// construct drawable image
		return getImageWithDisabled(dEnabled, dDisabled);
	}


	private static Drawable getImageWithDisabled(Drawable dEnabled, Drawable dDisabled) {
		// construct drawable image
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(new int[]{-android.R.attr.state_enabled}, dDisabled);
		drawable.addState(new int[]{android.R.attr.state_enabled}, dEnabled);

		// set drawable
		return drawable;
	}

	private static Drawable getImageWithTint(Drawable draw, int tint) {
		draw = draw.mutate();
		draw = DrawableCompat.wrap(draw);
		DrawableCompat.setTint(draw, tint);
		DrawableCompat.setTintMode(draw, PorterDuff.Mode.SRC_ATOP);
		return draw;
	}
}
