package com.asamm.locus.addon.wearables.gui.custom;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.R;

/**
 * Created by menion on 15/08/15.
 * Asamm Software, s. r. o.
 */
public class InfoPanel {

    // main container
    private LinearLayout mMain;
    // text view for title
    private TextView mTvTitle;
    // title with description
    private TextView mTvDesc;
    // progress bar view
    private ProgressBar mProgressBar;

    public InfoPanel(LinearLayout view) {
        this.mMain = view;

        // get views
        mTvTitle = (TextView)
                view.findViewById(R.id.text_view_initializing);
        mTvDesc = (TextView)
                view.findViewById(R.id.text_view_desc);
        mProgressBar = (ProgressBar)
                view.findViewById(R.id.progress_bar_small);
    }

    /**
     * Set layout at a "progress"
     * @param title message title
     * @param desc extra description
     */
    public void displayProgress(CharSequence title, CharSequence desc) {
        mTvTitle.setText(title);
        mTvDesc.setText(desc);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Set layout at a "error"
     * @param msg message for user
     */
    public void displayError(int msg) {
        mTvTitle.setText(R.string.warning);
        mTvDesc.setText(msg);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Display optional information in panel.
     * @param title message title
     * @param desc extra description
     */
    public void displayInfo(CharSequence title, CharSequence desc) {
        mTvTitle.setText(title);
        if (desc != null && desc.length() > 0) {
            mTvDesc.setText(desc);
        } else {
            mTvDesc.setText("");
        }

        // hide progress
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}
