package com.asamm.locus.addon.wear.common.communication;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import locus.api.objects.Storable;
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
    protected GoogleApiClient googleApiClient;

    protected volatile String mNodeId;
    /**
     * List of unsent data consisting of pairs of <PATH, DATA>
     */
    private ConcurrentLinkedQueue<Pair<DataPath, TimeStampStorable>> unsentData;

    /**
     * This map can be used by the inheriting class to store and read information (depending on context)
     * about last time of receive or send event of selected Data type
     */
    private final HashMap<DataPath, Long> mLastDataTransmitTime = new HashMap<>();

    protected LocusWearCommService(Context context) {
        this.context = context;
        unsentData = new ConcurrentLinkedQueue<>();

        // connect the GoogleApiClient
        googleApiClient = new GoogleApiClient.Builder(context.getApplicationContext()).
                addApi(Wearable.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                build();
        googleApiClient.connect();
    }

    protected void destroy() {
        // destroy GoogleAPIClient class
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        while (!unsentData.isEmpty()) {
            Pair<DataPath, TimeStampStorable> toSend = unsentData.poll();
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
     * Sends {@link EmptyCommand} to given [path].
     */
    public void sendCommand(DataPath path) {
        sendDataItem(path, new EmptyCommand());
    }

    /**
     * Sends given [data] to given [path].
     */
    public void sendDataItem(DataPath path, TimeStampStorable data) {
        Logger.INSTANCE.logD(LocusWearCommService.class.getSimpleName(),
                "sendDataItem(" + path + ", " + data + "), " +
                        "connected: " + googleApiClient.isConnected());
        if (!googleApiClient.isConnected()) {
            unsentData.offer(new Pair<>(path, data));
            reconnectIfNeeded();
        } else {
            sendDataItemWithoutConnectionCheck(path, data);
        }
    }

    // message api not used currently
//	public void sendMessage(DataPath path, TimeStampStorable data) {
//		Logger.logD(TAG, "Sending message "+path.getPath());
//		Wearable.MessageApi.sendMessage(mGoogleApiClient, mNodeId, path.getPath(), data.getAsBytes());
//
//		// You can add success and/or failure listeners,
//		// Or you can call Tasks.await() and catch ExecutionException
//		//sendTask.addOnSuccessListener(...);
//		//sendTask.addOnFailureListener(...);
//	}

    /**
     * Sends payload, should be only called from this class and its subclasses
     */
    protected void sendDataItemWithoutConnectionCheck(DataPath path, TimeStampStorable data) {
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
        Wearable.DataApi.putDataItem(googleApiClient, request);
    }

    public boolean isConnected() {
        return googleApiClient.isConnected();
    }

    public boolean isConnecting() {
        return googleApiClient.isConnecting();
    }

    public void reconnectIfNeeded() {
        if (!isConnected() && !isConnecting()) {
            googleApiClient.connect();
        }
    }

    /**
     * Transforms DataItem object to custom data container which type depends on given path type
     */
    public <E extends TimeStampStorable> E createStorableForPath(DataPath p, DataItem item) {
        Class<? extends TimeStampStorable> clazz = p.getContainerClass();
        if (clazz.getSimpleName().equals(EmptyCommand.class.getSimpleName())) {
            return null;
        }
        try {
            Map<String, DataItemAsset> assets = item.getAssets();
            DataItemAsset asset = assets.get(DataPath.DEFAULT_ASSET_KEY);
            if (asset == null) {
                Storable storable = clazz.newInstance();
                storable.read(item.getData());
                return (E) storable;
            } else {
                // blocking call - asset receive
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        googleApiClient, asset).await().getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int nRead;
                byte[] buffer = new byte[0x1000];
                while ((nRead = assetInputStream.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, nRead);
                }
                Storable storable = clazz.newInstance();
                storable.read(baos.toByteArray());
                return (E) storable;
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE("DataPath", "Constructor failed for " + p.name(), e);
            return null;
        }
    }

    public String getNodeId() {
        return mNodeId;
    }

    public void setNodeId(String nodeId) {
        mNodeId = nodeId;
    }

    /**
     * Sets NOW as last trasmit time for data of type [dataPath]
     */
    public void pushLastTransmitTimeFor(DataPath dataPath) {
        mLastDataTransmitTime.put(dataPath, System.currentTimeMillis());
    }

    /**
     * @return last transmit time for the given data type. Will result in 0L if
     * no transmit time was given yet for this data type.
     */
    public long getLastTransmitTimeFor(DataPath dataPath) {
        Long result = mLastDataTransmitTime.get(dataPath);
        return result != null ? result : 0L;
    }
}
