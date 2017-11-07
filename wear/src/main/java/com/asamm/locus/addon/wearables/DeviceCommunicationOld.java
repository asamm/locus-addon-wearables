package com.asamm.locus.addon.wearables;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.assam.locus.addon.wearables.common.Const;
import com.assam.locus.addon.wearables.common.DataContainer;
import com.asamm.locus.addon.wearables.gui.CustomActivityOld;
import com.asamm.locus.addon.wearables.gui.MapActivityOld;
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
public class DeviceCommunicationOld implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // tag for logger
    private static final String TAG = DeviceCommunicationOld.class.getSimpleName();

    // stored singleton
    private static DeviceCommunicationOld mInstance;

    static void initialize(MainApplicationOld app) {
        destroyInstance();
        mInstance = new DeviceCommunicationOld(app);
    }

    /**
     * Get current instance if exists. Do not create a new if do not exists.
     * @return instance of deviceCommunication or 'null' if not exists
     */
    public static DeviceCommunicationOld getInstance() {
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

    /**************************************************/
    // PRIVATE PART
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

    /**
     * Default constructor.
     */
    private DeviceCommunicationOld(MainApplicationOld app) {
        Logger.logD(TAG, "DeviceCommunicationOld(" + app + ")");
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

    /**
     * Start thread that will take care about refreshing of content.
     */
    private void startRefresher() {
        // class for periodic checks.
        final Runnable mChecker = new Runnable() {

            @Override
            public void run() {
                try {
                    // repeat actions till system is running
                    int counter = 0;
                    while (mGoogleApiClient.isConnected() && mRefresher != null) {
                        if (getLastUpdate() == null) {
                            Thread.sleep(1000);
                        } else {
                            Thread.sleep(2500);
                        }

                        // increase counter
                        counter++;

                        // check connection to node
                        if (isApiConnected() && !isNodeConnected()) {
                            connectToNode();
                        }

                        // perform update
                        boolean reloadBase = !isReady() || counter % 5 == 0;
                        performUpdateOfData(reloadBase);

                        // refresh map
                        if (MainApplicationOld.getCurrentActivity() instanceof MapActivityOld) {
                            performGetMapPreview();
                        }
                    }
                } catch (Exception e) {
                    Logger.logE(TAG, "startRefresher()", e);
                }
            }
        };

        // prepare and start refresher
        mRefresher = new Thread(mChecker);
        mRefresher.setPriority(Thread.MIN_PRIORITY);
        mRefresher.start();
    }

    // SCREEN LIFECYCLE

    /**
     * Called when screen is started.
     * @param act started activity
     */
	void checkConnection(CustomActivityOld act) {
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
        CustomActivityOld act = MainApplicationOld.getCurrentActivity();
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
                    performUpdateOfData(false);
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

            // refresh main layout
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
        CustomActivityOld act = MainApplicationOld.getCurrentActivity();
        int width  = act.getWindow().getDecorView().getWidth();
        int height = act.getWindow().getDecorView().getHeight();
        if (width <= 0 || height <= 0) {
            Logger.logW(TAG, "performGetMapPreview(), " +
                    "invalid dimensions:" + width + ", " + height);
            return;
        }

        // generate path (set zero coordinates to get always centered map)
        String path = Const.PATH_GET_MAP_PREVIEW;
        path += 0.0 + "/" + 0.0 + "/";
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
			if (!MainApplicationOld.existCurrentActivity() && !mDisconnectAfterSend) {
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
