/**
 * Created by Milan Cejnar on 01.12.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.gui.trackrec.stats.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.SpannableTextUtils;
import com.asamm.locus.addon.wear.gui.custom.TrackStatConsumable;
import com.asamm.locus.addon.wear.gui.trackrec.stats.model.TrackRecStatTypeEnum;


/**
 * Componend for displaying various(specified by {@code mType} single-value statistics
 */
public class TrackStatLayout extends ConstraintLayout {

	/**
	 * Type of displayed statistics
	 */
	private TrackRecStatTypeEnum mType;
	// formatted text of measured value/statistics
	private TextView mTextViewValue;

	private ImageView mImageViewIcon;
	private TextView mTextViewDescription;

	public TrackStatLayout(Context context) {
		this(context, null);
	}

	public TrackStatLayout(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TrackStatLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.mType = TrackRecStatTypeEnum.BLANK;
		initView(context, attrs);
	}

	private void initView(Context ctx, AttributeSet attrs) {
		// get parameters from attributes
		final TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.TrackStatLayout);
		boolean isPositionTopScreen = ta.getBoolean(R.styleable.TrackStatLayout_positionTop, true);
		boolean isPositionLeftScreen = ta.getBoolean(R.styleable.TrackStatLayout_positionLeft, true);
		ta.recycle();

		View.inflate(ctx,
				isPositionTopScreen ? R.layout.track_stat_layout_icon_top : R.layout.track_stat_layout_icon_bottom,
				this);
		mTextViewValue = findViewById(R.id.stat_value);
		mImageViewIcon = findViewById(R.id.stat_icon);
		mTextViewDescription = findViewById(R.id.stat_description);

		int gravity = Gravity.CENTER_VERTICAL |
				(isPositionLeftScreen ? Gravity.RIGHT : Gravity.LEFT);
		mTextViewValue.setGravity(gravity);
		mTextViewDescription.setGravity(gravity);
		mImageViewIcon.setScaleType(isPositionLeftScreen ?
				ImageView.ScaleType.FIT_END : ImageView.ScaleType.FIT_START);

		setType(mType);
	}

	public void setType(TrackRecStatTypeEnum statType) {
		this.mType = statType;
		mImageViewIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), mType.getIconId()));
		mTextViewDescription.setText(getResources().getText(mType.getNameStringId()));
		mTextViewValue.setText("");
	}

	public void consumeNewStatistics(TrackRecordingValue trv) {
		TrackStatConsumable.ValueUnitContainer newValue = mType.consumeAndFormat(trv);
		SpannableStringBuilder ssb = new SpannableStringBuilder(newValue.getValue());
		SpannableTextUtils.addStyledText(ssb, " " + newValue.getUnits(), 0.5f, Typeface.NORMAL, 0);
		mTextViewValue.setText(ssb);
	}

	public void setAmbientMode(boolean enabled) {
		mTextViewDescription.setTextColor(enabled ? getContext().getColor(R.color.base_light_primary) : getContext().getColor(R.color.base_dark_primary));
		mTextViewValue.setTextColor(enabled ? getContext().getColor(R.color.base_light_primary) : getContext().getColor(R.color.base_dark_primary));
		if (enabled) {
			mImageViewIcon.setColorFilter(R.color.base_light_primary);
		} else {
			mImageViewIcon.clearColorFilter();
		}
	}
}
