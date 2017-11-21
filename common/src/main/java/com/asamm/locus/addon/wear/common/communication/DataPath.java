package com.asamm.locus.addon.wear.common.communication;

import com.asamm.locus.addon.wear.common.communication.containers.BasicAppInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.StringCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.google.android.gms.wearable.DataItem;

/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public enum DataPath {
	GET_HAND_SHAKE(EmptyCommand.class),
	PUT_HAND_SHAKE(HandShakeValue.class),
	GET_BASIC_INFO(EmptyCommand.class),
	PUT_BASIC_INFO(BasicAppInfoValue.class),
	GET_TRACK_REC_PROFILES(EmptyCommand.class),
	PUT_TRACK_REC_PROFILE_INFO(TrackProfileInfoValue.ValueList.class),
	PUT_TRACK_REC_ICON_INFO(TrackProfileIconValue.ValueList.class),
	PUT_APP_DESTROYED(EmptyCommand.class),

	GET_TRACK_REC(EmptyCommand.class),
	PUT_TRACK_REC(TrackRecordingValue.class, false),
	PUT_TRACK_REC_STATE_CHANGE(TrackRecordingStateChangeValue.class),
	GET_ADD_WAYPOINT(EmptyCommand.class),
	PUT_ADD_WAYPOINT(EmptyCommand.class),

	GET_PERIODIC_DATA(PeriodicCommand.class),

	GET_KEEP_ALIVE(EmptyCommand.class, false, false),
	PUT_MAP(MapContainer.class, false, true),
	/**
	 * Fake communication data path, used for signalling activity about ON_CONNECTED event inside
	 * strictly the application. Should not be used over network.
	 */
	PUT_ON_CONNECTED_EVENT(EmptyCommand.class);

	public static final String BASE_PATH = "/locus/wear";

	private static final DataPath[] values = values();

	public static final String DEFAULT_ASSET_KEY = ":";

	private String mKey;
	private String mPath;
	private boolean mUrgent;
	private boolean mAsset;
	private Class<? extends TimeStampStorable> mContainerClass;

	/**
	 * default urgent setting if not explicitly specified
	 */
	private static final boolean URGENT_DEFAULT = true;
	/**
	 * default setting wheter to use Asset or DataItem for data transfaer if not explicitly specified
	 */
	private static final boolean USE_ASSETS_DEFAULT = false;

	DataPath(Class<? extends TimeStampStorable> container) {
		this(container, URGENT_DEFAULT);
	}

	DataPath(Class<? extends TimeStampStorable> container, boolean isUrgent) {
		this(container, isUrgent, USE_ASSETS_DEFAULT);
	}

	DataPath(Class<? extends TimeStampStorable> container, boolean isUrgent, boolean isAsset) {
		mKey = this.name().toLowerCase();
		mPath = BASE_PATH + '/' + mKey;
		this.mContainerClass = container;
		this.mUrgent = isUrgent;
		this.mAsset = isAsset;
	}

	public String getKey() {
		return mKey;
	}

	public String getPath() {
		return mPath;
	}

	public static DataPath valueOf(DataItem item) {
		for (DataPath p : values) {
			if (p.getPath().equals(item.getUri().getPath())) {
				return p;
			}
		}
		return null;
	}

	public boolean isUrgent() {
		return mUrgent;
	}

	public boolean isAsset() {
		return mAsset;
	}

	public Class<? extends TimeStampStorable> getContainerClass() {
		return mContainerClass;
	}
}
