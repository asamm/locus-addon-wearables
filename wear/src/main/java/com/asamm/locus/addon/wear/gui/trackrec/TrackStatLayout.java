package com.asamm.locus.addon.wear.gui.trackrec;


import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

public class TrackStatLayout extends LinearLayout {

	private static final String ARG_TYPE = "ARG_TYPE";
	private static final String ARG_ICON_BOTTOM = "ARG_ICON_BOTTOM";

	private TrackRecStatTypeEnum mType;
	private boolean mIsIconBottom;
	private TextView mTextViewValue;
	private ImageView mImageViewIcon;
	private boolean mIsIconTop;

	public TrackStatLayout(Context context) {
		this(context, null);
	}

	public TrackStatLayout(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TrackStatLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public TrackStatLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.mType = TrackRecStatTypeEnum.BLANK;
		initView(context, attrs);
	}

	private void initView(Context ctx, AttributeSet attrs) {
		// get parameters from attributes
		final TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.TrackStatLayout);
		mIsIconTop = ta.getBoolean(R.styleable.TrackStatLayout_isIconTop, true);
		ta.recycle();

		View.inflate(ctx,
				mIsIconTop ? R.layout.track_stat_layout_icon_top : R.layout.track_stat_layout_icon_bottom,
				this);
		mTextViewValue = findViewById(R.id.statText);
		mImageViewIcon = findViewById(R.id.statIcon);
		setType(mType);
	}

	public void setType(TrackRecStatTypeEnum statType) {
		this.mType = statType;
		mImageViewIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), mType.getIconId()));
		mTextViewValue.setText("");
	}
	
	public void consumeNewStatistics(TrackRecordingValue trv) {
		String newValue = mType.consumeAndFormat(trv.getTrackRecStats());
		mTextViewValue.setText(newValue);

	}

}
