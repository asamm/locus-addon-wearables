package com.asamm.locus.addon.wear.features.trackRecord;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.wear.widget.CircularProgressLayout;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.WatchDogPredicate;
import com.asamm.locus.addon.wear.features.settings.PreferencesEx;
import com.asamm.locus.addon.wear.utils.FeatureConfigEnum;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.CommandStringExtra;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.communication.WearCommService;
import com.asamm.locus.addon.wear.features.trackRecord.profiles.ProfileListActivity;
import com.asamm.locus.addon.wear.features.trackRecord.profiles.TrackRecordProfileSelectLayout;
import com.asamm.locus.addon.wear.features.trackRecord.recording.MainScreenController;
import com.asamm.locus.addon.wear.features.trackRecord.recording.RecordingScrollLayout;
import com.asamm.locus.addon.wear.features.trackRecord.recording.TrackRecordingControllerUpdatable;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;
import com.asamm.locus.addon.wear.features.map.MapActivity;
import com.asamm.locus.addon.wear.gui.custom.DisableGuiHelper;
import com.asamm.locus.addon.wear.gui.custom.WaypointInputTextActivity;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonActionDescEnum;
import com.asamm.locus.addon.wear.gui.custom.hwcontrols.HwButtonAutoDetectActionEnum;
import com.asamm.locus.addon.wear.features.trackRecord.stats.StatsScreenController;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackRecordActivityConfiguration;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackStatTypeEnum;
import com.asamm.locus.addon.wear.features.trackRecord.stats.model.TrackStatViewId;
import com.asamm.locus.addon.wear.features.trackRecord.stats.view.TrackStatsSelectListActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import locus.api.utils.Logger;

import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.IDLE;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.IDLE_WAITING;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.PAUSED;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.PAUSED_WAITING;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.REC;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.REC_WAITING;
import static com.asamm.locus.addon.wear.features.trackRecord.TrackRecActivityState.UNINITIALIZED;

/**
 * States of Track recording activity depending on current track recording state.
 * "Waiting" states are used when transitioning between track rec. state and waiting on comm.
 * confirmation from the device
 * <p>
 * Created by Milan Cejnar on 22.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordActivity extends LocusWearActivity implements CircularProgressLayout.OnTimerFinishedListener {

    private static final String TAG = TrackRecordActivity.class.getSimpleName();

    private static final int REQUEST_WP_NAME = 2;

    private static final int FLIPPER_START_RECORDING_SCREEN_IDX = 0;
    private static final int FLIPPER_RECORDING_RUNNING_SCREEN_IDX = 1;

    private static final int REFRESH_PERIOD_MS = 1000;

    private static final int WATCHDOG_TIMEOUT = REFRESH_PERIOD_MS * 9;

    private CircularProgressLayout mCircularProgress;

    private ViewGroup rootLayout;
    /**
     * Last received track recording value
     */
    private TrackRecordingValue model;

    /**
     * View flipper for flipping between Start recording/Active recording screens
     */
    private ViewFlipper mRecViewFlipper;

    /**
     * Snapping scroll view with active recording control and statistics
     */
    private RecordingScrollLayout mRecordingScrollScreen;

    private MainScreenController mTrackRecMainScreen;

    // start recording screen fields
    private ImageView mImgStartRecording;

    private TextView mTvStartTrackRecordingHeader;

    private TextView mTvLblAmbientMode;

    /**
     * component for track recording profile display and selection
     */
    private TrackRecordProfileSelectLayout mProfileSelect;

    @Override
    protected DataPayload getInitialCommandType() {
        PeriodicCommand cmd = new PeriodicCommand(PeriodicCommand.IDX_PERIODIC_TRACK_RECORDING,
                REFRESH_PERIOD_MS);
        return new DataPayload(DataPath.TD_GET_PERIODIC_DATA, cmd);
    }

    @Override
    protected DataPath getInitialCommandResponseType() {
        return DataPath.PUT_TRACK_REC;
    }

    @Override
    public boolean getSupportAmbientMode() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.track_record_activity);
        rootLayout = findViewById(R.id.track_rec_activity_layout_root);
        mImgStartRecording = findViewById(R.id.img_track_rec_start);
        mRecViewFlipper = findViewById(R.id.trackRecordViewFlipper);
        mProfileSelect = findViewById(R.id.track_rec_profile_select_layout);
        mProfileSelect.setProfileSelectCallback(this::onOpenProfileListActivityClick);

        mTvStartTrackRecordingHeader = mRecViewFlipper.findViewById(R.id.text_view_screen_header);
        mTvStartTrackRecordingHeader.setText(getText(R.string.title_activity_track_record));
        mTvLblAmbientMode = findViewById(R.id.tv_lbl_ambient_mode);

        initRecordingScrollScreen();
        setDisabledDrawables();
    }

    private void initRecordingScrollScreen() {
        mRecordingScrollScreen = findViewById(R.id.recording_scroll_view);
        mTrackRecMainScreen = new MainScreenController(mRecordingScrollScreen);
        TrackRecordActivityConfiguration cfg = TrackRecordActivityConfiguration.getConfiguration(this);
        ArrayList<TrackRecordingControllerUpdatable> screens = new ArrayList<>(4);
        screens.add(mTrackRecMainScreen);
        // 0 index reserved for mTrackRecMainScreen which must always be present
        for (int i = 1; i < cfg.getScreenCount(); i++) {
            screens.add(new StatsScreenController(mRecordingScrollScreen, i));
        }
        mRecordingScrollScreen.setFeatureItems(screens);
        mCircularProgress = mRecordingScrollScreen.findViewById(R.id.circular_progress);
        setProgression(false);
        mCircularProgress.setOnTimerFinishedListener(this);
    }

    private void setProgression(boolean enableProgression) {
        mTrackRecMainScreen.setProgressionVisible(enableProgression);
    }

    private void setDisabledDrawables() {
        Drawable d = DisableGuiHelper.getImageWithDisabled(this,
                BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_track_record_rec_192));
        mImgStartRecording.setImageDrawable(d);
    }

    private static final WatchDogPredicate<TrackProfileInfoValue.ValueList> isProfileListNotEmpty =
            (TrackProfileInfoValue.ValueList profs) ->
                    profs != null && profs.getStorables() != null && !profs.getStorables().isEmpty();

    @Override
    public void consumeNewData(DataPath path, TimeStampStorable data) {
        super.consumeNewData(path, data);
        switch (path) {
            case PUT_TRACK_REC_PROFILE_INFO:
                TrackProfileInfoValue.ValueList profiles = (TrackProfileInfoValue.ValueList) data;
                if (isProfileListNotEmpty.test(profiles)) {
                    runOnUiThread(() -> onNewProfilesReceived(profiles));
                } else {
                    Logger.INSTANCE.logE(TAG, "Received empty profile list.");
                    getMainApplication().sendDataWithWatchDogConditionable(
                            new DataPayload<>(DataPath.GET_TRACK_REC_PROFILES, new EmptyCommand()),
                            DataPath.PUT_TRACK_REC_PROFILE_INFO, WATCHDOG_TIMEOUT,
                            isProfileListNotEmpty);
                }
                break;
            case PUT_PROFILE_ICON:
                final TrackProfileIconValue icon = (TrackProfileIconValue) data;
                runOnUiThread(() -> onNewIconReceived(icon));
                break;
            case PUT_TRACK_REC:
                TrackRecordingValue trv = (TrackRecordingValue) data;
                onPutTrackRec(trv);
                getMainApplication().addWatchDog(getInitialCommandType(), getInitialCommandResponseType(), WATCHDOG_TIMEOUT);
                break;
            case PUT_ADD_WAYPOINT:
                runOnUiThread(() -> Toast.makeText(this, getResources().getString(R.string.waypoint_added), Toast.LENGTH_SHORT).show());
                break;
        }
    }

    private void onNewProfilesReceived(TrackProfileInfoValue.ValueList profiles) {
        TrackProfileInfoValue selectedProfile = mProfileSelect.getProfile();
        boolean isProfileMissing = selectedProfile == null ||
                !profiles.getStorables().contains(selectedProfile);
        if (isProfileMissing) {
            mProfileSelect.setParameters(profiles.getStorables().get(0));
        }
        mProfileSelect.setProfileList(profiles);
        mProfileSelect.setEnabled(true);
    }

    private void onNewIconReceived(TrackProfileIconValue icon) {
        TrackProfileInfoValue selectedProfile = mProfileSelect.getProfile();
        if (selectedProfile != null && selectedProfile.getId() == icon.getId()) {
            mProfileSelect.setParameters(selectedProfile); // forces icon refresh from cache
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TrackStatsSelectListActivity.REQUEST_CODE_STATS_SELECT_LIST_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                byte b = data.getByteExtra(TrackStatsSelectListActivity.RESULT_STAT_ID, TrackStatTypeEnum.BLANK.getId());
                TrackStatTypeEnum result = TrackStatTypeEnum.getById(b);
                TrackStatViewId viewId = new TrackStatViewId(
                        data.getIntExtra(TrackStatsSelectListActivity.PARAM_SCREEN_IDX, -1),
                        data.getIntExtra(TrackStatsSelectListActivity.PARAM_CELL_IDX, -1));
                TrackRecordActivityConfiguration.getConfiguration(this)
                        .setStatConfigAtPosition(this, viewId.getScreenIdx(),
                                viewId.getCellIdx(), result);
                mRecordingScrollScreen.refreshStatisticsConfiguration(this);
            }
            return;
        }
        if (requestCode == REQUEST_WP_NAME) {
            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra(WaypointInputTextActivity.KEY_RESULT_DATA);
                WearCommService.getInstance().sendDataItem(DataPath.POST_ADD_WAYPOINT, new CommandStringExtra(name));
            }
            return;
        }
        if (requestCode == TrackRecordProfileSelectLayout.PICK_PROFILE_REQUEST && resultCode == Activity.RESULT_OK) {
            byte[] profileBytes = data.getByteArrayExtra(ProfileListActivity.RESULT_PROFILES);
            try {
                TrackProfileInfoValue info = new TrackProfileInfoValue();
                info.read(profileBytes);
                mProfileSelect.setParameters(info);
                PreferencesEx.persistLastTrackRecProfile(mProfileSelect.getProfile());
            } catch (IOException e) {
                Logger.INSTANCE.logE("TAG", "empty profile bytes", e);

            }
        } else {
            // refresh profile select even after canceled -> force refresh icon
            mProfileSelect.setParameters(mProfileSelect.getProfile());
        }
    }

    // handler for delayed open request for profile selection activity
    private Handler mDelayedProfileSelectClickHandler;

    public void onOpenProfileListActivityClick(View v) {
        if (mDelayedStartClickHandler != null) { // pending start recording request
            return;
        }
        if (mProfileSelect.hasProfileList()) {
            Intent i = new Intent(this, ProfileListActivity.class);
            TrackProfileInfoValue.ValueList profiles = mProfileSelect.getProfileList();
            Bundle b = new Bundle();
            b.putByteArray(ProfileListActivity.ARG_PROFILES, profiles.getAsBytes());
            b.putString(ProfileListActivity.ARG_SELECTED_PROFILE_NAME,
                    mProfileSelect.getProfile() == null ? "" : mProfileSelect.getProfile().getName());
            i.putExtras(b);
            startActivityForResult(i, TrackRecordProfileSelectLayout.PICK_PROFILE_REQUEST);
        } else { // postpone activity start until conditions met
            synchronized (this) {
                if (mDelayedProfileSelectClickHandler == null && isIdleScreenAlive()) {
                    mDelayedProfileSelectClickHandler = new Handler();
                    mDelayedProfileSelectClickHandler.postDelayed(() -> {
                        mDelayedProfileSelectClickHandler = null;
                        onOpenProfileListActivityClick(v);
                    }, 333);
                }
            }
        }
    }

    private void onPutTrackRec(final TrackRecordingValue trv) {
        if (trv == null || !trv.isInfoAvailable()) {
            DataPayload p = getInitialCommandType();
            getMainApplication().sendDataWithWatchDogConditionable(getInitialCommandType(),
                    getInitialCommandResponseType(),
                    WATCHDOG_TIMEOUT, (d) -> d != null);
            return;
        }
        runOnUiThread(() -> {
            synchronized (mStateMachine) {
                mStateMachine.update(trv);
                if (mStateMachine.getCurrentState() == IDLE && isRecScreenVisible()) {
                    enableIdleScreen();
                }
            }
            mRecordingScrollScreen.onNewTrackRecordingData(this, trv);
            if (isRecScreenVisible()) {
                refreshStatistics(trv);
            }
        });
    }

    private void refreshStatistics(TrackRecordingValue trv) {

    }

    private boolean isRecScreenVisible() {
        return mRecViewFlipper.getDisplayedChild() == FLIPPER_RECORDING_RUNNING_SCREEN_IDX;
    }

    @Override
    protected void onResume() {
        super.onResume();
        TrackProfileInfoValue profileInfo = PreferencesEx.getLastTrackRecProfile();
        TrackRecordingStateEnum lastState = PreferencesEx.getLastTrackRecProfileState();
        if (profileInfo.getName() != null) {
            mProfileSelect.setParameters(profileInfo);
        } else {
            mProfileSelect.setPlaceHolder(getText(R.string.loading_profiles));
        }
        // initialize starting model from persistence
        model = new TrackRecordingValue(true,
                lastState != TrackRecordingStateEnum.NOT_RECORDING,
                lastState == TrackRecordingStateEnum.PAUSED, profileInfo.getName(), null, null, null);
        if (lastState != TrackRecordingStateEnum.NOT_RECORDING) {
            transitionToRecState();
        } else {
            transitionToIdlestate();
        }
        setIgnoreNextDrawerPeek();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferencesEx.persistLastRecState(model);
        PreferencesEx.persistLastTrackRecProfile(mProfileSelect.getProfile());
    }

    private volatile Handler mDelayedStartClickHandler;

    public void handleStartClick(final View v) {
        if (isIdleScreenAlive()) {
            mImgStartRecording.setEnabled(false);
        }
        synchronized (this) {
            if (mStateMachine.getCurrentState() == IDLE && mProfileSelect.hasProfileList()) {
                // start recoding
                sendStateChangeRequest(TrackRecordingStateEnum.RECORDING);
                mStateMachine.transitionTo(REC_WAITING);
                mImgStartRecording.setEnabled(true);
            } else {
                if (mDelayedStartClickHandler == null && isIdleScreenAlive()) {
                    mDelayedStartClickHandler = new Handler();
                    mDelayedStartClickHandler.postDelayed(() -> {
                        mDelayedStartClickHandler = null;
                        handleStartClick(v);
                    }, 333);
                }
            }
        }
    }

    private boolean isIdleScreenAlive() {
        final TrackRecActivityState state = mStateMachine.getCurrentState();
        return ((MainApplication) getApplication()).getCurrentActivity() == this
                && (state == IDLE || state == IDLE_WAITING || state == UNINITIALIZED)
                && this.getState() != WearActivityState.ON_STOP
                && this.getState() != WearActivityState.ON_DESTROY;
    }


    public boolean isStoppingProgressionActive() {
        return Boolean.TRUE.equals(mTrackRecMainScreen.isProgressionVisible());
    }

    public void handleStopClick(View v) {
        boolean isProgressionActive = isStoppingProgressionActive();
        if (isProgressionActive) {
            mCircularProgress.stopTimer();
        } else {
            mCircularProgress.setTotalTime(2000);
            mCircularProgress.startTimer();
        }
        setProgression(!isProgressionActive);
    }

    public void handlePauseClick(View v) {
        if (mStateMachine.getCurrentState() == REC) {
            sendStateChangeRequest(TrackRecordingStateEnum.PAUSED);
            mStateMachine.transitionTo(PAUSED_WAITING);
        } else if (mStateMachine.getCurrentState() == PAUSED) {
            sendStateChangeRequest(TrackRecordingStateEnum.RECORDING);
            mStateMachine.transitionTo(REC_WAITING);
        }
    }

    public void handleAddWaypointClick(View v) {
        Intent i = new Intent(this, WaypointInputTextActivity.class);
        startActivityForResult(i, REQUEST_WP_NAME);
    }


    private TrackProfileInfoValue getProfileInfo() {
        return mProfileSelect.getProfile();
    }

    @Override
    protected void onHandShakeFinished() {
        super.onHandShakeFinished();
        getMainApplication().sendDataWithWatchDog(DataPath.GET_TRACK_REC_PROFILES, new EmptyCommand(),
                DataPath.PUT_TRACK_REC_PROFILE_INFO, WATCHDOG_TIMEOUT);
    }

    @Override
    public boolean isUsePeriodicData() {
        return true;
    }

    private void sendStateChangeRequest(final TrackRecordingStateEnum newState) {
        WearCommService wcs = WearCommService.getInstance();
        TrackProfileInfoValue v = getProfileInfo();
        TrackRecordingStateChangeValue command =
                new TrackRecordingStateChangeValue(newState, v != null ? v.getName() : null);
        wcs.sendDataItem(DataPath.PUT_TRACK_REC_STATE_CHANGE, command);
        DataPayload p = getInitialCommandType();
        getMainApplication().sendDataWithWatchDogConditionable(p, DataPath.PUT_TRACK_REC, WATCHDOG_TIMEOUT,
                (response) -> {
                    if (response instanceof TrackRecordingValue) {
                        TrackRecordingStateEnum state =
                                ((TrackRecordingValue) response).getTrackRecordingState();
                        return state == newState;
                    }
                    return false;
                });
    }

    private void setIdleScreenEnabled(boolean isEnabled) {

    }

    /**
     * Called when transition back to the start screen from the active tracking screen
     */
    private void transitionToIdlestate() {
        setIdleScreenEnabled(false);
        mRecViewFlipper.setDisplayedChild(FLIPPER_START_RECORDING_SCREEN_IDX);
        Logger.INSTANCE.logD(TAG, "setting idle screen");
    }

    /**
     * Called when NOT_RECORDING state is confirmed
     */
    private void enableIdleScreen() {
        stopTrackRecService();
        setIdleScreenEnabled(true);
        mRecViewFlipper.setDisplayedChild(FLIPPER_START_RECORDING_SCREEN_IDX);
        Logger.INSTANCE.logD(TAG, "Enabling idle screen");
    }

    /**
     * Called when transitioning from start screen to recording screen
     */
    private void transitionToRecState() {
        mRecordingScrollScreen.onTrackActivityStateChange(this, mStateMachine.getCurrentState());
        mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);
        Logger.INSTANCE.logD(TAG, "setting rec screen");
    }

    /**
     * Called when new recording or paused state is confirmed by the first data
     */
    private void enableRecScreen() {
        startTrackRecService();
        mRecordingScrollScreen.onTrackActivityStateChange(this, mStateMachine.getCurrentState());
        mRecViewFlipper.setDisplayedChild(FLIPPER_RECORDING_RUNNING_SCREEN_IDX);
        Logger.INSTANCE.logD(TAG, "Enabling rec screen");
    }

    private void transitionToPauseState() {
        transitionToRecState();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        rootLayout.setBackgroundColor(Color.BLACK);
        mTvStartTrackRecordingHeader.setBackgroundColor(Color.BLACK);
        mProfileSelect.setVisibility(View.INVISIBLE);
        mRecordingScrollScreen.setAmbient(true);
        mImgStartRecording.setVisibility(View.INVISIBLE);
        mTvLblAmbientMode.setVisibility(View.VISIBLE);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        rootLayout.setBackgroundColor(getColor(R.color.color_background));
        mTvStartTrackRecordingHeader.setBackgroundColor(getColor(R.color.base_primary));
        mProfileSelect.setVisibility(View.VISIBLE);
        mRecordingScrollScreen.setAmbient(false);
        mImgStartRecording.setVisibility(View.VISIBLE);
        mTvLblAmbientMode.setVisibility(View.GONE);

    }

    @Override
    public void onTimerFinished(CircularProgressLayout layout) {
        setProgression(false);
        mCircularProgress.stopTimer();
        sendStateChangeRequest(TrackRecordingStateEnum.NOT_RECORDING);
        mStateMachine.transitionTo(IDLE_WAITING);
    }

    private void startTrackRecService() {
        if (!TrackRecordingService.isRunning()
                && PreferencesEx.getHrmFeatureConfig() == FeatureConfigEnum.ENABLED) {
            Intent i = new Intent(this, TrackRecordingService.class);
            i.setAction(TrackRecordingService.ACTION_START_FOREGROUND_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        }
    }

    private void stopTrackRecService() {
        if (TrackRecordingService.isRunning()) {
            Intent intent = new Intent(this, TrackRecordingService.class);
            intent.setAction(TrackRecordingService.ACTION_STOP_FOREGROUND_SERVICE);
            startService(intent);
        }
    }

    @Override
    public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {
        delegate.registerDefaultRotaryMotionListener(mRecordingScrollScreen);
        delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_DOWN, () -> mRecordingScrollScreen.scrollToNextPage());
        delegate.registerHwButtonListener(HwButtonActionDescEnum.ROTARY_UP, () -> mRecordingScrollScreen.scrollToPreviousPage());

        HwButtonActionDescEnum upPrimaryBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_PRIMARY_OR_UP);
        HwButtonActionDescEnum downBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_DOWN);
        HwButtonActionDescEnum secondaryActionBtn =
                delegate.getHwButtonForAutoDetectAction(HwButtonAutoDetectActionEnum.BTN_ACTION_SECONDARY);

        delegate.registerHwButtonListener(HwButtonActionDescEnum.BTN_2_LONG_PRESS,
                () -> {
                    delegate.setUseHwButtons(false);
                    startLocusWearActivity(MapActivity.class);
                });

        delegate.registerHwButtonListener(upPrimaryBtn, () -> {
            if (isRecScreenVisible()) {
                if (isStoppingProgressionActive()) {
                    // temporary allows to cancel stop request by short press
                    handleStopClick(null);
                } else {
                    handlePauseClick(null);
                }
            } else {
                handleStartClick(null);
            }
        });
        delegate.registerHwButtonListener(downBtn, () -> {
            if (isRecScreenVisible()) {
                mRecordingScrollScreen.scrollToNextPage();
            }
        });
        // this watch has third button, use it as a scroll up, otherwise it would be unused anyway
        if (secondaryActionBtn == HwButtonActionDescEnum.BTN_3_PRESS) {
            delegate.registerHwButtonListener(HwButtonActionDescEnum.BTN_3_PRESS, () -> {
                if (isRecScreenVisible()) {
                    mRecordingScrollScreen.scrollToPreviousPage();
                }
            });
        }
        delegate.registerHwButtonListener(HwButtonActionDescEnum.BTN_1_LONG_PRESS, () -> {
            if (isRecScreenVisible()) {
                handleStopClick(null);
            }
        });
    }

    /**
     * Interface for activity state machine
     *
     * @param <STATE>
     * @param <MODEL>
     */
    private interface StateMachine<STATE extends Enum<STATE>, MODEL> {
        STATE getCurrentState();

        void update(MODEL model);

        void transitionTo(STATE newState);

    }

    /**
     * state machine implementation for this activity
     */
    private StateMachine<TrackRecActivityState, TrackRecordingValue> mStateMachine =
            new StateMachine<TrackRecActivityState, TrackRecordingValue>() {
                private TrackRecActivityState mCurrentState = UNINITIALIZED;
                /**
                 * Map for handling routines triggered by state transition
                 * Map: state_from -> target_state -> trigger
                 */
                private HashMap<TrackRecActivityState, HashMap<TrackRecActivityState, Runnable>> mTransitionsFunctions;

                { /* initializer block for specifying handling functions triggered by
						state transition */
                    TrackRecActivityState[] values = TrackRecActivityState.values();
                    mTransitionsFunctions = new HashMap<>(values.length);
                    for (TrackRecActivityState s : values) {
                        mTransitionsFunctions.put(s, new HashMap<>());
                    }

                    // transitions for state uninitialized
                    HashMap<TrackRecActivityState, Runnable> uninitializedTransitions
                            = mTransitionsFunctions.get(UNINITIALIZED);
                    uninitializedTransitions.put(IDLE_WAITING,
                            TrackRecordActivity.this::transitionToIdlestate);
                    uninitializedTransitions.put(REC_WAITING,
                            TrackRecordActivity.this::transitionToRecState);
                    uninitializedTransitions.put(PAUSED_WAITING,
                            TrackRecordActivity.this::transitionToPauseState);

                    // transitions to waiting states from main states
                    for (TrackRecActivityState toDual : Arrays.asList(IDLE, PAUSED, REC)) {
                        final TrackRecActivityState to = toDual.getOtherWaitingState();
                        for (TrackRecActivityState from : Arrays.asList(IDLE, PAUSED, REC)) {
                            if (from == toDual)
                                continue; // cant go back from enabled state to its wating state
                            HashMap<TrackRecActivityState, Runnable> m = mTransitionsFunctions.get(from);
                            if (to == IDLE_WAITING)
                                m.put(to, TrackRecordActivity.this::transitionToIdlestate);
                            else if (to == PAUSED_WAITING)
                                m.put(to, TrackRecordActivity.this::transitionToPauseState);
                            else if (to == REC_WAITING)
                                m.put(to, TrackRecordActivity.this::transitionToRecState);
                        }
                    }

                    // transitions to main state from their respecive waiting state
                    mTransitionsFunctions.get(IDLE_WAITING).put(IDLE, TrackRecordActivity.this::enableIdleScreen);
                    mTransitionsFunctions.get(REC_WAITING).put(REC, TrackRecordActivity.this::enableRecScreen);
                    mTransitionsFunctions.get(PAUSED_WAITING).put(PAUSED, TrackRecordActivity.this::enableRecScreen);
                }

                @Override
                public TrackRecActivityState getCurrentState() {
                    return mCurrentState;
                }

                /**
                 * Transitions state machine to new state and calls trigger if available
                 * @param newState
                 */
                public void transitionTo(TrackRecActivityState newState) {
                    Runnable action = mTransitionsFunctions.get(mCurrentState).get(newState);
                    mCurrentState = newState;
                    if (action != null) {
                        action.run();
                    }
                }

                /**
                 * Called on model update. Implements hard-coded transition logic
                 *
                 * @param trv new model value
                 */
                @Override
                public void update(TrackRecordingValue trv) {
                    model = trv;

                    TrackRecordingStateEnum newRecordingState = trv.getTrackRecordingState();

                    // State machine transition conditions
                    if (mCurrentState == UNINITIALIZED) {
                        transitionTo(newRecordingState == TrackRecordingStateEnum.NOT_RECORDING ?
                                IDLE_WAITING : newRecordingState == TrackRecordingStateEnum.PAUSED ?
                                PAUSED_WAITING : REC_WAITING);
                        // immediately perform one more state machine step to skip current waiting state
                        if (mCurrentState != UNINITIALIZED) {
                            update(trv);
                        }
                    } else if (mCurrentState.isWaitingState() && newRecordingState == mCurrentState.getExpectedState()) {
                        // received data I was waiting for, transition to enabled state
                        transitionTo(mCurrentState.getOtherWaitingState());
                    } else if (!mCurrentState.isWaitingState()) {
                        TrackRecordingStateEnum expectedState =
                                mCurrentState.getOtherWaitingState().getExpectedState();
                        if (newRecordingState != expectedState) {
                            switch (newRecordingState) {
                                case PAUSED:
                                    transitionTo(PAUSED_WAITING);
                                    break;
                                case RECORDING:
                                    transitionTo(REC_WAITING);
                                    break;
                                case NOT_RECORDING:
                                    transitionTo(IDLE_WAITING);
                            }
                        }
                    }
                }
            };
}
