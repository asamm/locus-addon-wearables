/**
 * Created by Milan Cejnar on 01.12.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.features.trackRecord.stats.view;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Typeface;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.gui.custom.SpannableTextUtils;
import com.asamm.locus.addon.wear.gui.custom.TrackStatConsumable;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackStatTypeEnum;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackStatViewId;


/**
 * Componend for displaying various(specified by {@code mType} single-value statistics
 */
public class TrackStatLayout extends ConstraintLayout {

    /**
     * Type of displayed statistics
     */
    private TrackStatTypeEnum mType;
    private int mGravity;
    private boolean isPositionTopScreen;
    // formatted text of measured value/statistics
    private TextView textViewValue;

    private ImageView imageViewIcon;
    private TextView textViewDescription;
    private TrackStatViewId trackStatViewPositionId = new TrackStatViewId(-1, -1);
    private LinearLayout blankInfo = null;

    public TrackStatLayout(Context context) {
        this(context, null);
    }

    public TrackStatLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackStatLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context ctx, AttributeSet attrs) {
        // get parameters from attributes
        final TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.TrackStatLayout);
        isPositionTopScreen = ta.getBoolean(R.styleable.TrackStatLayout_positionTop, true);
        mGravity = ta.getInteger(R.styleable.TrackStatLayout_android_gravity, Gravity.CENTER);
        ta.recycle();

        this.setOnLongClickListener(v -> {
            Intent i = new Intent(ctx, TrackStatsSelectListActivity.class);
            i.putExtra(TrackStatsSelectListActivity.PARAM_STAT_ID, mType.getId());
            i.putExtra(TrackStatsSelectListActivity.PARAM_SCREEN_IDX, trackStatViewPositionId.getScreenIdx());
            i.putExtra(TrackStatsSelectListActivity.PARAM_CELL_IDX, trackStatViewPositionId.getCellIdx());
            ((Activity) ctx).startActivityForResult(i, TrackStatsSelectListActivity.REQUEST_CODE_STATS_SELECT_LIST_ACTIVITY);
            return true;
        });
        initView(ctx);
    }

    private boolean isPositionCentered() {
        return mGravity == Gravity.CENTER;
    }

    private boolean isAlignRight() {
        return (mGravity & Gravity.RIGHT) == Gravity.RIGHT;
    }

    private void initView(Context ctx) {
        final int layoutId;
        if (isPositionCentered() && isPositionTopScreen) {
            layoutId = R.layout.track_stat_layout_icon_centered_top;
        } else if (isPositionCentered()) {
            layoutId = R.layout.track_stat_layout_icon_centered_bottom;
        } else if (isPositionTopScreen) {
            layoutId = R.layout.track_stat_layout_icon_top;
        } else {
            layoutId = R.layout.track_stat_layout_icon_bottom;
        }
        View v = View.inflate(ctx, layoutId, this);
        textViewValue = v.findViewById(R.id.stat_value);
        imageViewIcon = v.findViewById(R.id.stat_icon);
        textViewDescription = v.findViewById(R.id.stat_description);

        // apply a bit of margin to the description text on round screens
        if (!isPositionCentered() && getContext().getResources().getConfiguration().isScreenRound()) {
            int widthMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    4, getContext().getResources().getDisplayMetrics());
            if (isAlignRight()) {
                ((LayoutParams) textViewDescription.getLayoutParams()).leftMargin = widthMargin;
            } else {
                ((LayoutParams) textViewDescription.getLayoutParams()).rightMargin = widthMargin;
            }
        }

        textViewValue.setGravity(mGravity);
        textViewDescription.setGravity(mGravity);
        imageViewIcon.setScaleType(isPositionCentered() ? ImageView.ScaleType.FIT_CENTER :
                isAlignRight() ? ImageView.ScaleType.FIT_END : ImageView.ScaleType.FIT_START);
    }

    /**
     * used during controller initialization to setup screen and cell id that identifies this view
     */
    public void setTrackStatViewPositionId(int screenIdx, int cellIdx) {
        this.trackStatViewPositionId = new TrackStatViewId(screenIdx, cellIdx);
    }

    public void setType(TrackStatTypeEnum statType) {
        this.mType = statType;
        imageViewIcon.setImageResource(mType.getIconId());
        textViewDescription.setText(mType.getNameStringId());
        textViewValue.setText("");

        // initiate blank value overlay and set its layout properties based on position of the cell
        if (mType == TrackStatTypeEnum.BLANK && blankInfo == null) {
            View v = View.inflate(getContext(), R.layout.track_stat_layout_empty_info_overlay, this);
            blankInfo = v.findViewById(R.id.linear_layout);
            if (!isPositionCentered()) {
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                ConstraintLayout.LayoutParams layoutParams = ((ConstraintLayout.LayoutParams)blankInfo.getLayoutParams());
                if (isPositionTopScreen) {
                    layoutParams.topMargin = height / 11;
                } else {
                    layoutParams.bottomMargin = height / 11;
                }
                if(isAlignRight()) {
                    layoutParams.leftMargin = width / 11;
                } else {
                    layoutParams.rightMargin = width / 11;
                }
            }
        } else if (mType != TrackStatTypeEnum.BLANK && blankInfo != null) {
            removeView(blankInfo);
            blankInfo = null;
        }
    }

    public void consumeNewStatistics(TrackRecordingValue trv) {
        if (mType == null) return;
        TrackStatConsumable.ValueUnitContainer newValue = mType.consumeAndFormat(trv);
        SpannableStringBuilder ssb = new SpannableStringBuilder(newValue.getValue());
        SpannableTextUtils.addStyledText(ssb, " " + newValue.getUnits(), 0.5f, Typeface.NORMAL, 0);
        textViewValue.setText(ssb);
    }

    public void setAmbientMode(boolean enabled) {
        textViewDescription.setTextColor(getContext().getColor(R.color.color_on_background));
        textViewValue.setTextColor(getContext().getColor(R.color.color_on_background));
        if (enabled) {
            imageViewIcon.setColorFilter(getContext().getColor(R.color.color_on_background));
        } else {
            imageViewIcon.clearColorFilter();
        }
    }
}
