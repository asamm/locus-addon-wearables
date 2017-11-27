package com.asamm.locus.addon.wear.gui.trackrec;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateEnum;

/**
 * Created by Milan Cejnar on 22.11.2017.
 * Asamm Software, s.r.o.
 */

public enum TrackRecActivityState {
	UNINITIALIZED,
	IDLE_WAITING(TrackRecordingStateEnum.NOT_RECORDING),
	IDLE,
	REC_WAITING(TrackRecordingStateEnum.RECORDING),
	REC,
	PAUSED_WAITING(TrackRecordingStateEnum.PAUSED),
	PAUSED;

	private boolean mWaitingState;
	/** defined which state to expect if this is a waiting state */
	private TrackRecordingStateEnum mExpectedState;

	TrackRecActivityState() {
		mWaitingState = false;
		mExpectedState = null;
	}
	TrackRecActivityState(TrackRecordingStateEnum expectedState) {
		mWaitingState = true;
		this.mExpectedState = expectedState;
	}

	public boolean isWaitingState() {
		return mWaitingState;
	}

	public TrackRecordingStateEnum getExpectedState() {
		return mExpectedState;
	}
	/**
	 * @return the other waiting state e.g. for REC returns REC_WAITING and vice versa
	 */
	TrackRecActivityState getOtherWaitingState() {
		switch (this) {
			case PAUSED: return TrackRecActivityState.PAUSED_WAITING;
			case PAUSED_WAITING: return TrackRecActivityState.PAUSED;
			case REC: return TrackRecActivityState.REC_WAITING;
			case REC_WAITING: return TrackRecActivityState.REC;
			case IDLE: return TrackRecActivityState.IDLE_WAITING;
			case IDLE_WAITING: return TrackRecActivityState.IDLE;
			default: return this; // identity
		}
	}
}
