package com.asamm.locus.addon.wear.gui.trackrec.recording;


import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.trackrec.recording.TrackRecStatTypeEnum;

/**
 * Componend for displaying various(specified by {@code mType} single-value statistics
 * <p>
 * Created by Milan Cejnar on 01.12.2017.
 * Asamm Software, s.r.o.
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
		boolean mIsIconTop = ta.getBoolean(R.styleable.TrackStatLayout_isIconTop, true);
		ta.recycle();

		View.inflate(ctx,
				mIsIconTop ? R.layout.track_stat_layout_icon_top : R.layout.track_stat_layout_icon_bottom,
				this);
		mTextViewValue = findViewById(R.id.stat_text);
		mImageViewIcon = findViewById(R.id.stat_icon);
		mTextViewDescription = findViewById(R.id.stat_description);
		setType(mType);
	}

	public void setType(TrackRecStatTypeEnum statType) {
		this.mType = statType;
		mImageViewIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), mType.getIconId()));
		mTextViewDescription.setText(getResources().getText(mType.getNameStringId()));
		mTextViewValue.setText("");
	}

	public void consumeNewStatistics(TrackRecordingValue trv) {
		String newValue = mType.consumeAndFormat(trv);
		mTextViewValue.setText(newValue);

	}

}
