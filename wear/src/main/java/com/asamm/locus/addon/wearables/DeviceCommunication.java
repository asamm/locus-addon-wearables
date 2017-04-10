package com.asamm.locus.addon.wearables;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.asamm.locus.addon.wearables.gui.CustomActivity;
import com.asamm.locus.addon.wearables.gui.MapActivity;
import com.asamm.locus.addon.wearables.utils.Const;
import com.asamm.locus.addon.wearables.utils.DataContainer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.utils.Logger;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class DeviceCommunication implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // tag for logger
    private static final String TAG = DeviceCommunication.class.getSimpleName();
    //sleep in ms between two refresh
    private static final int INTERVAL_MINUTE = 60000;
    private static final int INTERVAL_10SECS = 10000;
    private static final int sleepOnActive  = 2500;
    private static final int sleepOnEnteringAmbient  = 5000;
    private static final int sleepOnExitingAmbient  = 500;

    /**************************************************/
    // PRIVATE PART
    // stored singleton
    private static DeviceCommunication mInstance;
    private static int sleepOnAmbient  = 10000;
    /**************************************************/

    // Google API client
    private GoogleApiClient mGoogleApiClient;
    // refresher for updates
    private Thread mRefresher;
    // the connected node to send the message to
    private Node mNode;
    // current fresh data container
    private DataContainer mDataContainer;
    // last container with data from periodic updates
    private UpdateContainer mLastData;
	// handler for messages
	private MessageSender mSender;
    //ambient mode
    private boolean mAmbientMode=false;
    private int refreshSleep = sleepOnActive;
    private boolean mJustExitedFromAmbient = false;
    private boolean mJustEnteredToAmbient = false;
    /**
     * Default constructor.
     */
    private DeviceCommunication(MainApplication app) {
        Logger.logD(TAG, "DeviceCommunication(" + app + ")");
        mGoogleApiClient = new GoogleApiClient.Builder(app).
                addApi(Wearable.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).
                build();
        mGoogleApiClient.connect();
        mNode = null;
        mDataContainer = new DataContainer();
        mRefresher = null;
		mSender = new MessageSender();
    }

    static void initialize(MainApplication app) {
        destroyInstance();
        mInstance = new DeviceCommunication(app);
    }

    /**
     * Get current instance if exists. Do not create a new if do not exists.
     * @return instance of deviceCommunication or 'null' if not exists
     */
    public static DeviceCommunication getInstance() {
        return mInstance;
    }

    /**
     * Destroy existing singleton instance.
     */
    static void destroyInstance() {
        if (mInstance != null) {
            mInstance.destroy();
        }
        mInstance = null;
    }

    public boolean isAmbientMode() {
        return mAmbientMode;
    }

    public void onEnterAmbient()
    {
        this.mAmbientMode = true;
        this.mJustEnteredToAmbient = true;
    }

    public void onExitAmbient()
    {
        this.mAmbientMode = false;
        this.mJustExitedFromAmbient = true;
        requestDataImmediatelly();
    }

    /**
     * Destroy communication object.
     */
    private void destroy() {
		Logger.logW(TAG, "destroy(), " +
				"connected:" + mGoogleApiClient.isConnected());
        // disconnect Google client
        if (mGoogleApiClient.isConnected()) {
            // send message and destroy instance
            mSender.addMessage(Const.PATH_STATE_APP_DESTROYED, true);

            // stop refresher
            mRefresher = null;
        }
    }

    public void requestDataImmediatelly() {

        if (mRefresher != null) {
            synchronized (mRefresher) {
                mRefresher.notify();
            }
        }
    }


    private int calculateRefreshSleep()
    {
        int ret;

        if (mAmbientMode) {
            if (mJustEnteredToAmbient) {
                ret = sleepOnEnteringAmbient;
                mJustEnteredToAmbient = false;
            } else
                ret = sleepOnAmbient;
        }else {
            if (mJustExitedFromAmbient ) {
                if (MainApplication.getCurrentActivity() instanceof MapActivity)
                    ret = sleepOnExitingAmbient; //request data again
                else
                    ret = sleepOnActive;
                mJustExitedFromAmbient = false;
            } else
                ret = sleepOnActive;
        }

        return ret;
    }

    /**
     * Start thread that will take care about refreshing of content.
     */

    private void startRefresher() {
        // class for periodic checks.
        final class ThreadChecker extends Thread{
            @Override
            public void run(){
                try {
                    // repeat actions till system is running
                    int counter = 0;
                    while (mGoogleApiClient.isConnected() && mRefresher != null) {
                        synchronized (this) {
                            if (getLastUpdate() == null) {
                                this.wait(1000);
                            } else {
                                Log.d(TAG, "refreshSleep: " + refreshSleep);
                                this.wait(refreshSleep);
                            }
                        }
                        // increase counter
                        counter++;

                        // check connection to node
                        if (isApiConnected() && !isNodeConnected()) {
                            connectToNode();
                        }

                        // perform update
                        boolean reloadBase = !isReady() || counter % 5 == 0;
                        if (reloadBase) {
                            performUpdateOfData(true);
                            performUpdateOfData(false);
                        }else
                            performUpdateOfData(false);


                        // refresh map
                        if (!mAmbientMode || mJustEnteredToAmbient)
                            if (MainApplication.getCurrentActivity() instanceof MapActivity) {
                                performGetMapPreview();
                            }

                        refreshSleep = calculateRefreshSleep();

                    }
                } catch (Exception e) {
                    Logger.logE(TAG, "startRefresher()", e);
                }
            }

            }

        // prepare and start refresher
        mRefresher = new ThreadChecker();
        mRefresher.setPriority(Thread.MIN_PRIORITY);
        mRefresher.start();
    }

    // SCREEN LIFECYCLE

    /**
     * Called when screen is started.
     * @param act started activity
     */
	void checkConnection(CustomActivity act) {
        Logger.logD(TAG, "checkConnection(" + act + "), " +
                "connected:" + mGoogleApiClient.isConnected() + ", " +
                "connecting:" + mGoogleApiClient.isConnecting() + ", " +
                "node:" + mNode);

        // connect to Google client
        if (mGoogleApiClient.isConnected()) {
            // already connected
        } else if (mGoogleApiClient.isConnecting()) {
            // currently connecting
        } else {
            // try to connect now
            mGoogleApiClient.connect();
        }
    }

    // TESTING

    /**
     * Check if current device is correct connected.
     * @return <code>true</code> if connected
     */
    public boolean isApiConnected() {
        return mGoogleApiClient != null &&
                mGoogleApiClient.isConnected();
    }

    /**
     * Check if current device is correct connected.
     * @return <code>true</code> if connected
     */
    public boolean isNodeConnected() {
        return isApiConnected() && mNode != null;
    }

    /**
     * Check if system is fully ready for work.
     * @return <code>true</code> if system is ready for work
     */
    public boolean isReady() {
        // check if connected and data are loaded
        if (!isNodeConnected() ) {
            return false;
        }

        // check data
        if (getDataContainer().getLocusVersion() == null) {
            // unable to loaded LocusVersion
            return false;
        }

        // check version
        return getDataContainer().getLocusVersion().
                getVersionCode() >= Const.LOCUS_VERSION_CODE;
    }

    /**
     * Refresh layout of activity.
     */
    private void refreshLayout() {
        CustomActivity act = MainApplication.getCurrentActivity();
        if (act != null && !act.isFinishing()) {
            act.refreshLayout();
        }
    }

    /**
     * Get current container with Locus data.
     * @return loaded container
     */
    public DataContainer getDataContainer() {
        return mDataContainer;
    }

    /**
     * Get last received periodic updates container.
     * @return last fresh container
     */
    public UpdateContainer getLastUpdate() {
        return mLastData;
    }

    // PERFORM LOCUS CHECKS

    /**
     * Send request to check base Locus data.
     */
    private void performUpdateOfData(boolean reloadBase) {
        // preform refresh
        if (reloadBase) {
            // reload base data. Periodic updates will be refresh once,
            // base data are received
            sendMessage(Const.PATH_GET_BASE_DATA);
        } else if (isReady()) {
            // reload update container if system is ready
            sendMessage(Const.PATH_GET_PERIODIC_UPDATE);
        }
    }

    /**
     * Handle new received data.
     * @param event received data
     */
	void handleNewData(MessageEvent event) {
        try {
            Logger.logD(TAG, "handleNewData(" + event.getPath() + ")");
            switch (event.getPath()) {
                case Const.PATH_LOADED_DATA_CONTAINER:
                    // load base data
                    mDataContainer.mergeContainer(new DataContainer(event.getData()));

                    // request new periodic updates now
                    //performUpdateOfData(false);
                    break;
                case Const.PATH_LOADED_PERIODIC_UPDATE:
                    // get loaded data
                    byte[] data = event.getData();
                    if (data == null || data.length == 0) {
                        Logger.logD(TAG, "handleNewData(), " +
                                "received container is empty, data not yet ready");
                        mLastData = null;
                    } else {
                        mLastData = new UpdateContainer();
                        mLastData.read(event.getData());
                    }
                    break;
                default:
                    return;
            }

            refreshLayout();
        } catch (IOException e) {
            Logger.logE(TAG, "handleNewData(" + event + ")", e);
        }
    }

    // MAP PREVIEW

    /**
     * Send request on map preview.
     */
    private void performGetMapPreview() {
        // check update container
        if (getLastUpdate() == null) {
            return;
        }

        // check existence of activity
        CustomActivity act = MainApplication.getCurrentActivity();
        int width  = act.getWindow().getDecorView().getWidth();
        int height = act.getWindow().getDecorView().getHeight();
        if (width <= 0 || height <= 0) {
            Logger.logW(TAG, "performGetMapPreview(), " +
                    "invalid dimensions:" + width + ", " + height);
            return;
        }

        Log.d(TAG, "performGetMapPreview last MapZoomLevel: " + getLastUpdate().getMapZoomLevel());

        // generate path (set zero coordinates to get always centered map)
        String path = Const.PATH_GET_MAP_PREVIEW;
        path += 0.0 + "/" + 0.0 + "/";
        if (act != null)
            path += ((MapActivity) act).zoomLevel + "/";
        else
            path += getLastUpdate().getMapZoomLevel() + "/";

        path += (width * 2) + "/" + (height * 2) + "/";

        // finally send message with request
        sendMessage(path);
    }

    // TRACK RECORDING EVENTS

    public void performTrackRecordingGetProfiles() {
        sendMessage(Const.PATH_GET_TRACK_RECORD_PROFILES);
    }

    public void performTrackRecordStart(String profileName) {
        sendMessage(Const.PATH_TRACK_REC_START + profileName);
    }

    public void performTrackRecordStop() {
        sendMessage(Const.PATH_TRACK_REC_STOP);
    }

    public void performTrackRecordPause() {
        if (getLastUpdate().isTrackRecPaused()) {
            sendMessage(Const.PATH_TRACK_REC_START);
        } else {
            sendMessage(Const.PATH_TRACK_REC_PAUSE);
        }
    }

    public void performTrackRecordAddWpt() {
        sendMessage(Const.PATH_TRACK_REC_ADD_WPT);
    }

    // COMMUNICATION

    /**
     * Send simple message to parent device.
     * @param msg message to send
     */
    private void sendMessage(final String msg) {
		mSender.addMessage(msg, false);
    }

    // GOOGLE API CLIENT - CONNECTION CALLBACK

    @Override
    public void onConnected(Bundle bundle) {
        Logger.logW(TAG, "onConnected(" + bundle + ")");

        // finally connect to node
        connectToNode();
    }

    /**
     * Try to connect to best possible node.
     */
    private void connectToNode() {
        // prepare callback handler
        ResultCallback<NodeApi.GetConnectedNodesResult> callback =
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {

                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        // get best node
                        Node nodeBest = null;
                        for (Node node : nodes.getNodes()) {
                            nodeBest = node;
                            if (nodeBest.isNearby()) {
                                break;
                            }
                        }

                        // store node and refresh layout
                        mNode = nodeBest;
                        Logger.logD(TAG, "registerBestNode(), " +
                                "node:" + mNode + ", " +
                                "connected:" + mGoogleApiClient.isConnected() + ", " +
                                "connecting:" + mGoogleApiClient.isConnecting());

                        // if node is not connected, refresh layout, otherwise try to load new data
                        if (mNode != null) {
                            // check for a new data
                            performUpdateOfData(true);
                        }

                        // refresh layout and start refresher
                        refreshLayout();
                        startRefresher();
                    }
                };

        // obtain all notes
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).
                setResultCallback(callback);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.logW(TAG, "onConnectionSuspended(" + i + ")");
    }

    // GOOGLE API CLIENT - ON CONNECTION FAILED LISTENER

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logger.logW(TAG, "onConnectionFailed(" + connectionResult + ")");
    }

	// MESSAGE SENDER

	public void setLongRefreshPeriod( boolean value )
    {
        if (value)
            sleepOnAmbient = INTERVAL_MINUTE;
        else
            sleepOnAmbient = INTERVAL_10SECS;

    }

	private class MessageSender {

		// queue of messages to send
		private List<String> mMessagesQueue;
		// flag if we are currently sending message
		private boolean mSending;
		// flag if we are currently terminating this instance
		private boolean mDisconnectAfterSend;

		MessageSender() {
			mMessagesQueue = new ArrayList<>();
			mDisconnectAfterSend = false;
			mSending = false;
		}

		/**
		 * Add new message to queue.
		 * @param msg message to send
		 * @param last {@code true} if this is last message and instance should be destroyed then
		 */
		private void addMessage(String msg, boolean last) {
			// handle "last message"
			if (mDisconnectAfterSend) {
				Logger.logD(TAG, "sendMessage(" + msg + "), " +
						"instance is already destroying");
				return;
			} else if (last) {
				mDisconnectAfterSend = true;
				mMessagesQueue.clear();
			}

			// add message to queue if not yet exists
			if (!mMessagesQueue.contains(msg)) {
				mMessagesQueue.add(msg);
			}

			// handle queue
			handleMessagesQueue();
		}

		/**
		 * Handle current message queue.
		 */
		private synchronized void handleMessagesQueue() {
			// check messages
			if (mSending || mMessagesQueue.size() == 0) {
				return;
			}

			// check activity
			if (!MainApplication.existCurrentActivity() && !mDisconnectAfterSend) {
				Logger.logD(TAG, "handleMessagesQueue(), " +
						"activity not attached");
				return;
			}

			// check connected state
			if (!isNodeConnected()) {
				Logger.logD(TAG, "handleMessagesQueue(), " +
						"node not connected");
				refreshLayout();
				return;
			}

			// prepare callback for result
			mSending = true;
			final String msg = mMessagesQueue.remove(0);
			ResultCallback<MessageApi.SendMessageResult> callback =
					new ResultCallback<MessageApi.SendMessageResult>() {

						@Override
						public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
							Logger.logD(TAG, "sendMessage(" + msg + "), " +
									"result:" + sendMessageResult.getStatus().toString());

							// sending is done
							mSending = false;

							// destroy instance
							if (mDisconnectAfterSend) {
								// disconnect client
								if (isApiConnected()) {
									mGoogleApiClient.disconnect();
								}
							} else if (sendMessageResult.getStatus().getStatusCode() == 4000) {
								// handle "TARGET_NODE_NOT_CONNECTED" status
								refreshLayout();

								// handle next message
								handleMessagesQueue();
							}
						}
					};

			// finally send message
			Logger.logD(TAG, "handleMessagesQueue(), " +
					"sending message:" + msg);
			Wearable.MessageApi.
					sendMessage(mGoogleApiClient, mNode.getId(), msg, null).
					setResultCallback(callback);
		}
	}
}
