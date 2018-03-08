package com.asamm.locus.addon.wear.common.communication;

import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue;
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer;
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;
import com.asamm.locus.addon.wear.common.communication.containers.commands.EmptyCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand;
import com.asamm.locus.addon.wear.common.communication.containers.commands.ProfileIconGetCommand;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue;
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.google.android.gms.wearable.DataItem;

/**
 * Specifies available Paths for data communications using DataAPI.
 * Also includes other properties
 * - mark as urgent message
 * - support for Asset based communication instead of DataItem for bigger payloads
 * <p>
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public enum DataPath {
	GET_HAND_SHAKE(EmptyCommand.class, (byte) 1),
	PUT_HAND_SHAKE(HandShakeValue.class, (byte) 2),
	GET_TRACK_REC_PROFILES(EmptyCommand.class, (byte) 3),
	PUT_TRACK_REC_PROFILE_INFO(TrackProfileInfoValue.ValueList.class, (byte) 4),
	PUT_TRACK_REC(TrackRecordingValue.class, (byte) 5),
	PUT_TRACK_REC_STATE_CHANGE(TrackRecordingStateChangeValue.class, (byte) 6),
	GET_PROFILE_ICON(ProfileIconGetCommand.class, (byte) 7),
	PUT_PROFILE_ICON(TrackProfileIconValue.class, (byte) 8),
	GET_ADD_WAYPOINT(EmptyCommand.class, (byte) 9),
	PUT_ADD_WAYPOINT(EmptyCommand.class, (byte) 10),
	GET_PERIODIC_DATA(PeriodicCommand.class, (byte) 11),
	GET_KEEP_ALIVE(EmptyCommand.class, (byte) 12),
	PUT_MAP(MapContainer.class, (byte) 13),
	/**
	 * Fake communication data path, used for signalling activity about ON_CONNECTED event inside
	 * strictly the application. Should not be used over network.
	 */
	PUT_ON_CONNECTED_EVENT(EmptyCommand.class, (byte) 14);

	public static final String BASE_PATH = "/locus/wear";

	private static final DataPath[] values = values();

	public static final String DEFAULT_ASSET_KEY = ":";

	private byte mId;
	private String mKey;
	private String mPath;
	private boolean mUrgent;
	private Class<? extends TimeStampStorable> mContainerClass;

	/**
	 * default urgent setting if not explicitly specified
	 */
	private static final boolean URGENT_DEFAULT = true;
	/**
	 * default setting wheter to use Asset or DataItem for data transfaer if not explicitly specified
	 */
	private static final boolean USE_ASSETS_DEFAULT = false;

	DataPath(Class<? extends TimeStampStorable> container, byte id) {
		this(container, URGENT_DEFAULT, id);
	}

	DataPath(Class<? extends TimeStampStorable> container, boolean isUrgent, byte id) {
		mId = id;
		mKey = this.name().toLowerCase();
		mPath = BASE_PATH + '/' + mKey;
		this.mContainerClass = container;
		this.mUrgent = isUrgent;
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

	public static DataPath valueOf(byte id) {
		for (DataPath p : values) {
			if (p.mId == id) {
				return p;
			}
		}
		return null;
	}

	public boolean isUrgent() {
		return mUrgent;
	}

	public Class<? extends TimeStampStorable> getContainerClass() {
		return mContainerClass;
	}

	public byte getId() {
		return mId;
	}
}
