package com.asamm.locus.addon.wear.communication;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.asamm.locus.addon.wear.MainApplication;
import com.asamm.locus.addon.wear.WatchDog;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.LocusWearCommService;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.utils.TriStateLogicEnum;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Singleton instance handling wear-device communication
 * <p>
 * Created by Milan Cejnar on 07.11.2017.
 * Asamm Software, s.r.o.
 */

public class WearCommService extends LocusWearCommService implements CapabilityApi.CapabilityListener {
	private static String TAG = WearCommService.class.getSimpleName();
	private static WearCommService mDeviceCommunicationService;

	private volatile long mLastSentDataTimestamp = 0L;

	private MainApplication mApp;

	private static final String CAPABILITY_PHONE_APP = "verify_remote_wear_for_locus_map_phone_app";

	private TriStateLogicEnum mCapableClientDetected = TriStateLogicEnum.UNKNOWN;

	private WearCommService(MainApplication c) {
		super(c);
		this.mApp = c;
	}

	public static WearCommService getInstance() {
		return mDeviceCommunicationService;
	}

	public static WearCommService initialize(final MainApplication c) {
		if (mDeviceCommunicationService == null) {
			synchronized (TAG) {
				if (mDeviceCommunicationService == null) {
					mDeviceCommunicationService = new WearCommService(c);
				}
			}
		}
		return mDeviceCommunicationService;
	}

	@Override
	protected void destroy() {
		if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
			Wearable.CapabilityApi.removeCapabilityListener(
					mGoogleApiClient,
					this,
					CAPABILITY_PHONE_APP);
		}
		mApp = null;
		super.destroy();
	}

	public static void destroyInstance() {
		if (mDeviceCommunicationService != null) {
			mDeviceCommunicationService.destroy();
		}
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		super.onConnected(bundle);
		// Set up listeners for capability changes (install/uninstall of remote app).
		Wearable.CapabilityApi.addCapabilityListener(
				mGoogleApiClient,
				this,
				CAPABILITY_PHONE_APP);

		checkIfPhoneHasApp();

		final MainApplication app = this.mApp;
		if (app != null) {
			app.onConnected();
		}
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		super.onConnectionFailed(connectionResult);
		if (mApp != null) {
			mApp.onConnectionSuspened();
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		super.onConnectionSuspended(i);
		if (mApp != null) {
			mApp.onConnectionSuspened();
		}
	}


	public void getConnectedNodes(ResultCallback<NodeApi.GetConnectedNodesResult> resultCallback) {
		Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(resultCallback, 1000, TimeUnit.MILLISECONDS);
	}


	@Override
	public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
		Set<Node> capableNodes = capabilityInfo != null ? capabilityInfo.getNodes() : null;
		mCapableClientDetected = capableNodes != null && capableNodes.size() > 0 ?
				TriStateLogicEnum.TRUE : TriStateLogicEnum.FALSE;
		if (mCapableClientDetected == TriStateLogicEnum.TRUE) {
			Iterator<Node> nodeIt = capableNodes.iterator();
			mNodeId = null;
			while (mNodeId == null && nodeIt.hasNext()) {
				Node n = nodeIt.next();
				mNodeId = n.isNearby() ? n.getId() : null;
			}
		}
		if (mApp != null) {
			mApp.onCapableClientConnected();
		}
	}

	/**
	 * @return UNKNOWN if not known yet, TRUE if cabable client present, FALSE if capable device not found
	 */
	public TriStateLogicEnum isAppInstalledOnDevice() {
		return mCapableClientDetected;
	}

	@Override
	protected void sendDataItemWithoutConnectionCheck(DataPath path, TimeStampStorable data) {
		long currentTime = System.currentTimeMillis();
		// if keep alive command but some other command was sent recently then ignore this
		// command to save bandwidth
		if (path == DataPath.GET_KEEP_ALIVE &&
				currentTime - mLastSentDataTimestamp <= WatchDog.WD_PERIOD_TRANSMIT_KEEP_ALIVE_MS) {
			return;
		}
		// if sending other data than keep alive command, write current time to
		// postpone keep alive thread
		if (path != DataPath.GET_KEEP_ALIVE) {
			mLastSentDataTimestamp = currentTime;
		}
		super.sendDataItemWithoutConnectionCheck(path, data);
	}

	private void checkIfPhoneHasApp() {
		PendingResult<CapabilityApi.GetCapabilityResult> pendingResult =
				Wearable.CapabilityApi.getCapability(
						mGoogleApiClient,
						CAPABILITY_PHONE_APP,
						CapabilityApi.FILTER_ALL);

		pendingResult.setResultCallback((getCapabilityResult) -> {
			if (getCapabilityResult.getStatus().isSuccess()) {
				onCapabilityChanged(getCapabilityResult.getCapability());
			}
		});
	}
}

