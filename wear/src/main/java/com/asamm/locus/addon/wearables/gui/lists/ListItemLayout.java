package com.asamm.locus.addon.wearables.gui.lists;

import android.content.Context;
import android.graphics.Typeface;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.R;

/**
 * Created by menion on 08/08/15.
 * Asamm Software, s. r. o.
 */
public class ListItemLayout extends LinearLayout implements WearableListView.OnCenterProximityListener {

    // view for icon
    private ImageView mIvIcon;
    // view for name
    private TextView mTvTitle;
    // view for description
    private TextView mTvDesc;

    // alpha value for non-center item
    private final float mFadedTextAlpha;

    public ListItemLayout(Context context) {
        this(context, null);
    }

    public ListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mFadedTextAlpha = 0.60f;
    }

    // Get references to the icon and text in the item layout definition
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // prepare views
        mIvIcon = (ImageView)
                findViewById(R.id.image_view_icon);
        mTvTitle = (TextView)
                findViewById(R.id.text_view_title);
        mTvDesc = (TextView)
                findViewById(R.id.text_view_desc);
    }

    // GETTERS FOR ITEMS

    /**
     * Return image view with left icon.
     * @return image view
     */
    public ImageView getImageViewIcon() {
        return mIvIcon;
    }

    /**
     * Get text view with name.
     * @return text view
     */
    public TextView getTextViewTitle() {
        return mTvTitle;
    }

    /**
     * Get text view with name.
     * @return text view
     */
    public TextView getTextViewDesc() {
        return mTvDesc;
    }

    // ON CENTER PROXIMITY LISTENER

    @Override
    public void onCenterPosition(boolean animate) {
        mIvIcon.setAlpha(1f);
        mTvTitle.setAlpha(1f);
        mTvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTvDesc.setAlpha(1f);
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mIvIcon.setAlpha(mFadedTextAlpha);
        mTvTitle.setAlpha(mFadedTextAlpha);
        mTvTitle.setTypeface(Typeface.DEFAULT);
        mTvDesc.setAlpha(mFadedTextAlpha);
    }
}
