package com.asamm.locus.addon.wear.common.communication;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import locus.api.utils.Logger;

/**
 * Ancestor for device and wear communication services.
 * Derived classes are meant to be implemented as singleton.
 * <p>
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */
public class LocusWearCommService implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	protected final int MAX_DATA_ITEM_SIZE_B = 99 * 1024;

	protected Context context;

	// Google API client
	protected GoogleApiClient mGoogleApiClient;

	/**
	 * List of unsent data consisting of pairs of <PATH, DATA>
	 */
	private ConcurrentLinkedQueue<Pair<DataPath, TimeStampStorable>> mUnsentData;

	protected LocusWearCommService(Context context) {
		this.context = context;
		mUnsentData = new ConcurrentLinkedQueue<>();
		// connect the GoogleApiClient
		mGoogleApiClient = new GoogleApiClient.Builder(context.getApplicationContext()).
				addApi(Wearable.API).
				addConnectionCallbacks(this).
				addOnConnectionFailedListener(this).
				build();

		mGoogleApiClient.connect();
	}

	protected void destroy() {
		// destroy GoogleAPIClient class
		mGoogleApiClient.disconnect();
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		while (!mUnsentData.isEmpty()) {
			Pair<DataPath, TimeStampStorable> toSend = mUnsentData.poll();
			sendDataItem(toSend.first, toSend.second);
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		// no handling required
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		// no handling required
	}

	/**
	 * Sends {@link EmptyCommand} to given {@path}
	 *
	 * @param path
	 */
	public void sendCommand(DataPath path) {
		sendDataItem(path, new EmptyCommand());
	}

	/**
	 * Sends given {@data} to given {@path}
	 *
	 * @param path
	 * @param data
	 */
	public void sendDataItem(DataPath path, TimeStampStorable data) {
		if (!mGoogleApiClient.isConnected()) {
			mUnsentData.offer(new Pair<>(path, data));
			reconnectIfNeeded();
		} else {
			sendDataItemWithoutConnectionCheck(path, data);
		}
	}

	/**
	 * Sends payload, should be only called from this class and its subclasses
	 *
	 * @param path
	 * @param data
	 */
	protected void sendDataItemWithoutConnectionCheck(DataPath path, TimeStampStorable data) {
		Logger.logD(getClass().getSimpleName(), "Sending " + path);
		PutDataRequest request = PutDataRequest.create(path.getPath());
		final byte[] dataToSend = data.getAsBytes();
		// check data size whether to send as and asset or plain data item
		if (dataToSend.length >= MAX_DATA_ITEM_SIZE_B) {
			request.putAsset(DataPath.DEFAULT_ASSET_KEY, Asset.createFromBytes(dataToSend));
		} else {
			request.setData(dataToSend);
		}
		if (path.isUrgent()) {
			request.setUrgent();
		}
		PendingResult<DataApi.DataItemResult> pendingResult =
				Wearable.DataApi.putDataItem(mGoogleApiClient, request);
	}

	public boolean isConnected() {
		return mGoogleApiClient.isConnected();
	}

	public boolean isConnecting() {
		return mGoogleApiClient.isConnecting();
	}

	public void reconnectIfNeeded() {
		if (!isConnected() && !isConnecting()) {
			mGoogleApiClient.connect();
		}
	}

	/**
	 * Transforms DataItem object to custom data container which type depends on given path type
	 *
	 * @param p
	 * @param item
	 * @param <E>
	 * @return
	 */
	public <E extends TimeStampStorable> E createStorableForPath(DataPath p, DataItem item) {
		Class<? extends TimeStampStorable> clazz = p.getContainerClass();
		if (clazz.getSimpleName().equals(EmptyCommand.class.getSimpleName())) {
			return null;
		}
		try {
			Map<String, DataItemAsset> assets = item.getAssets();
			DataItemAsset asset = assets == null ? null : assets.get(DataPath.DEFAULT_ASSET_KEY);
			if (asset == null) {
				return (E) clazz.getConstructor(byte[].class).newInstance(item.getData());
			} else {
				// blocking call - asset receive
				InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
						mGoogleApiClient, asset).await().getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int nRead;
				byte[] buffer = new byte[0x1000];
				while ((nRead = assetInputStream.read(buffer, 0, buffer.length)) != -1) {
					baos.write(buffer, 0, nRead);
				}
				return (E) clazz.getConstructor(byte[].class).newInstance(baos.toByteArray());
			}
		} catch (Exception e) {
			Logger.logE("DataPath", "Constructor failed for " + p.name(), e);
			return null;
		}
	}
}
