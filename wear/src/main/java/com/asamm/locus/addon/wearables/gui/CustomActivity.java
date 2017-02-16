package com.asamm.locus.addon.wearables.gui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asamm.locus.addon.wearables.DeviceCommunication;
import com.asamm.locus.addon.wearables.MainApplication;
import com.asamm.locus.addon.wearables.R;
import com.asamm.locus.addon.wearables.gui.custom.InfoPanel;

import locus.api.utils.Logger;

/**
 * Created by menion on 12/08/15.
 * Asamm Software, s. r. o.
 */
public abstract class CustomActivity extends WearableActivity {

    // tag for logger
    private static final String TAG = CustomActivity.class.getSimpleName();

    // current activity state
    public enum State {
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTROY
    }

    // inflater for fast layout load
    private LayoutInflater mInflater;
    // channel for communication
    private DeviceCommunication mComm;

    // main layout
    public FrameLayout mContainer;
    // main screen title
    //private TextView mTvHeader;

    // state of current activity
    private State mCurrentState = State.ON_CREATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.logD(TAG, "onCreate() " + this.getClass());
        setContentView(R.layout.activity_main);

        // set state
        mCurrentState = State.ON_CREATE;

        // register activity
        MainApplication.activityOnCreate(this);

        // generate main views
        mContainer = (FrameLayout) findViewById(R.id.frame_layout_main);
        /*mTvHeader = (TextView)
                findViewById(R.id.text_view_screen_header);*/

        // prepare parameters
        mInflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // get instance of communicator
        mComm = DeviceCommunication.getInstance();

        // check
        if (checkIfDeviceReady()) {
            finishIfNotReady();
        }

        // enable ambient mode
        setAmbientEnabled();
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.logD(TAG, "onStart() " + this.getClass());

        // set state
        mCurrentState = State.ON_START;

        // register activity
        MainApplication.activityOnStart(this);

        // get instance of communicator (get it once more if instance was not yet
        // ready during onCreate)
        mComm = DeviceCommunication.getInstance();

        // check
        if (checkIfDeviceReady()) {
            finishIfNotReady();
        }
    }

    public void onResume() {
        super.onResume();
        Logger.logD(TAG, "onResume() "+ this.getClass());

        // set state
        mCurrentState = State.ON_RESUME;

        // register activity
        MainApplication.activityOnResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() " + this.getClass());
        // set state
        mCurrentState = State.ON_PAUSE;
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.logD(TAG, "onStop() " + this.getClass());

        // set state
        mCurrentState = State.ON_STOP;

        // register activity
        MainApplication.activityOnStop(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.logD(TAG, "onDestroy() " + this.getClass());

        // set state
        mCurrentState = State.ON_DESTROY;
    }

    // AMBIENT MODE

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
//        if (isAmbient()) {
//            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
//            mClockView.setVisibility(View.VISIBLE);
//
//            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
//        } else {
//            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
//            mClockView.setVisibility(View.GONE);
//        }
    }

    // TOOLS

    /**
     * Get current activity state.
     * @return current state
     */
    public State getCurrentState() {
        return mCurrentState;
    }

    /**
     * Get instance of device communication class.
     * @return device communication class
     */
    public DeviceCommunication getDeviceComm() {
        return mComm;
    }

    /**
     * Define screen title.
     * @param title visible text
     */
    /*protected void setScreenHeader(CharSequence title) {
        mTvHeader.setText(title);
    }*/

    /**
     * Check if system is ready, if not return to main menu.
     * @return <code>true</code> if finished
     */
    protected boolean finishIfNotReady() {
        //
        if (!getDeviceComm().isReady()) {
            Logger.logW(TAG, "onStart(), " +
                    "connection not ready " + this.getClass());
            finish();
            return true;
        }

        // nothing to do
        return false;
    }

    // WORK WITH FRAGMENTS SYSTEM

    /**
     * Display fragment in main screen container.
     * @param fragment fragment to display
     */
    public void displayFragment(AFragmentBase fragment) {
        // get fragment manager
        FragmentManager fm = getFragmentManager();

        // execute transaction
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(mContainer.getId(), fragment);
        ft.commit();
    }

    /**
     * Get current attached fragment.
     * @return attached fragment
     */
    public AFragmentBase getDisplayedFragment() {
        // get fragment manager
        FragmentManager fm = getFragmentManager();

        // return attached fragment
        return (AFragmentBase) fm.findFragmentById(mContainer.getId());
    }

    /**
     * Base fragment.
     */
    public static abstract class AFragmentBase extends Fragment {

        // current parent activity
        CustomActivity act;

        @Override
        public void onAttach(Activity act) {
            super.onAttach(act);
            this.act = (CustomActivity) act;
        }
    }

    // WORK WITH BASIC LAYOUTS

    private int mLastInflatedView = 0;

    /**
     * Clear current view container and insert new.
     * @param newLayout new layout resource IS
     * @return inflated layout
     */
    protected View clearContainer(int newLayout) {
        // check previous layout
        if (isContainerVisible(newLayout)) {
            return mContainer.getChildAt(0);
        }

        // clear existing layouts
        mContainer.removeAllViews();

        // inflate new layout and insert ite
        mInflater.inflate(newLayout, mContainer, true);
        // return layout
        mLastInflatedView = newLayout;
        return mContainer.getChildAt(0);
    }

    /**
     * Check if container is currently visible.
     * @param layoutId ID of layout
     * @return <code>true</code> if container is visible
     */
    protected boolean isContainerVisible(int layoutId) {
        return mLastInflatedView == layoutId;
    }

    // ABSTRACT PART

    /**
     * Check if device is ready, otherwise finish current activity.
     * @return <code>true</code> to perform check
     */
    protected boolean checkIfDeviceReady() {
        return false;
    }

    /**
     * Refresh state of current layout.
     */
    public abstract void refreshLayout();

    /**************************************************/
    // INFO SCREEN
    /**************************************************/

    /**
     * Display basic information screen with content.
     * @param title title of screen
     * @param desc optional small description
     */
    protected void displayScreenInfo(CharSequence title, CharSequence desc) {
        getNewInfoPanel().displayInfo(title, desc);
    }
    /**
     * Display basic progress screen with content.
     * @param title title of screen
     * @param desc optional small description
     */
    protected void displayScreenProgress(CharSequence title, CharSequence desc) {
        getNewInfoPanel().displayProgress(title, desc);
    }

    private InfoPanel getNewInfoPanel() {
        // get view and items
        View view = clearContainer(R.layout.layout_info_panel);

        // get panel for content
        return new InfoPanel((LinearLayout) view);
    }

    /**************************************************/
    // CONFIRMATION SCREEN
    /**************************************************/

    /**
     * Display confirmation view in current layout.
     * @param delay delay for confirmation
     * @param title title next to button
     * @param listener listener for events
     */
    public void displayConfirmationAction(long delay, int title,
            final DelayedConfirmationView.DelayedConfirmationListener listener) {
        // get view and items
        View view = clearContainer(R.layout.layout_confirmation_panel);

        // get and set delayed view
        final DelayedConfirmationView delayedView = (DelayedConfirmationView)
                view.findViewById(R.id.delayed_view_confirm);
        delayedView.setListener(new DelayedConfirmationView.DelayedConfirmationListener() {

            @Override
            public void onTimerFinished(View view) {
                delayedView.setListener(null);
                listener.onTimerFinished(view);
            }

            @Override
            public void onTimerSelected(View view) {
                delayedView.setListener(null);
                listener.onTimerSelected(view);
            }
        });

        // Two seconds to cancel the action
        delayedView.setTotalTimeMs(delay);
        // Start the timer
        delayedView.start();

        // set title
        TextView tvTitle = (TextView)
                view.findViewById(R.id.text_view_title);
        tvTitle.setText(title);
    }

    /**
     * Check if any confirmation action is currently visible.
     * @return <code>true</code> if is visible
     */
    public boolean isConfirmationActionVisible() {
        return isContainerVisible(R.layout.layout_confirmation_panel);
    }
}