package com.asamm.locus.addon.wear.common.communication.containers;

import com.asamm.locus.addon.wear.common.communication.Const;

import java.io.IOException;

import locus.api.android.MapPreviewResult;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.objects.enums.PointRteAction;
import locus.api.objects.extra.Location;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Container for transfer of map screen and navigation information
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */

public class MapContainer extends TimeStampStorable {

	private static final String TAG = "MapContainer";

	private static final int NAV_VALID_FLAG = 0x1;

	// result from loaded image
	private MapPreviewResult mLoadedMap;

	private static final Location ZERO_LOCATION = new Location(0, 0);
	// information about type of active guidance
	private byte mGuideType;
	private int mNavPointAction1Id;
	private int mNavPointAction2Id;
	private double mNavPoint1Dist;
	private byte mUnitsFormatLength;
	private byte mZoomDevice;
	private byte mZoomWear;
	private byte mStatus;

	//v1
	// last applied offset in pixels used for rendering the included map
	private int mOffsetX;
	private int mOffsetY;
	private Location mLastLocation;
	private short mBearing;

	public MapContainer() {
		super();
	}

	public MapContainer(byte[] byteArray) throws IOException {
		super(byteArray);
	}

	public MapContainer(MapPreviewResult loadedMap, UpdateContainer mLastUpdate, LocusInfo li,
						int zoom, int offsetX, int offsetY, Location lastLocation, short bearing) {
		this();
		mLoadedMap = loadedMap;
		this.mOffsetX = offsetX;
		this.mOffsetY = offsetY;
		mLastLocation = lastLocation == null ? ZERO_LOCATION : lastLocation;
		mBearing = bearing;
		if (mLastUpdate != null) {
			mZoomDevice = (byte) mLastUpdate.getMapZoomLevel();
			mZoomWear = (byte) zoom;
			mGuideType = (byte) mLastUpdate.getGuideType();
			UpdateContainer.GuideTypeTrack guide = mLastUpdate.getGuideTypeTrack();
			if (guide != null) {
				mStatus |= guide.isValid() ? NAV_VALID_FLAG : 0;
				mNavPointAction1Id = guide.getNavPoint1Action().getId();
				mNavPointAction2Id = guide.getNavPoint2Action().getId();
				mNavPoint1Dist = guide.getNavPoint1Dist();
				if (li != null) {
					mUnitsFormatLength = (byte) li.getUnitsFormatLength();
				}
			}
		}
	}

	public void reset() {
		super.reset();
		mLoadedMap = null;
		mGuideType = UpdateContainer.GUIDE_TYPE_DISABLED;
		mNavPointAction1Id = PointRteAction.UNDEFINED.getId();
		mNavPointAction2Id = PointRteAction.UNDEFINED.getId();
		mNavPoint1Dist = 0;
		mUnitsFormatLength = 0;
		mZoomDevice = Const.ZOOM_UNKOWN;
		mZoomWear = Const.ZOOM_UNKOWN;
		mStatus = 0;
		mOffsetY = 0;
		mOffsetX = 0;
		mBearing = 0;
		mLastLocation = ZERO_LOCATION;
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mGuideType = dr.readBytes(1)[0];
		mNavPointAction1Id = dr.readInt();
		mNavPointAction2Id = dr.readInt();
		mNavPoint1Dist = dr.readDouble();
		mUnitsFormatLength = dr.readBytes(1)[0];
		mZoomDevice = dr.readBytes(1)[0];
		mZoomWear = dr.readBytes(1)[0];
		mStatus = dr.readBytes(1)[0];
		boolean isMap = dr.readBoolean();
		try {
			mLoadedMap = isMap ? dr.readStorable(MapPreviewResult.class) : null;
		} catch (Exception e) {
			mLoadedMap = null;
			Logger.logE(TAG, "Could not read map image.", e);
		}
		if (version >= 1) {
			mOffsetX = dr.readInt();
			mOffsetY = dr.readInt();
			mBearing = dr.readShort();
			try {
				mLastLocation = dr.readStorable(Location.class);
			} catch (Exception e) {
				mLastLocation = ZERO_LOCATION;
			}
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.write(mGuideType);
		dw.writeInt(mNavPointAction1Id);
		dw.writeInt(mNavPointAction2Id);
		dw.writeDouble(mNavPoint1Dist);
		dw.write(mUnitsFormatLength);
		dw.write(mZoomDevice);
		dw.write(mZoomWear);
		dw.write(mStatus);
		boolean isMap = mLoadedMap != null;
		dw.writeBoolean(isMap);
		if (isMap) {
			dw.writeStorable(mLoadedMap);
		}

		// v1
		dw.writeInt(mOffsetX);
		dw.writeInt(mOffsetY);
		dw.writeShort(mBearing);
		dw.writeStorable(mLastLocation);
	}

	@Override
	protected int getVersion() {
		return 1;
	}

	public int getmGuideType() {
		return mGuideType;
	}

	public int getNavPointAction1Id() {
		return mNavPointAction1Id;
	}

	public void setNavPointAction1Id(int navPointAction1Id) {
		this.mNavPointAction1Id = navPointAction1Id;
	}

	public int getNavPointAction2Id() {
		return mNavPointAction2Id;
	}

	public void setNavPointAction2Id(int navPointAction2Id) {
		this.mNavPointAction2Id = navPointAction2Id;
	}

	public double getNavPoint1Dist() {
		return mNavPoint1Dist;
	}

	public int getUnitsFormatLength() {
		return mUnitsFormatLength;
	}

	public MapPreviewResult getLoadedMap() {
		return mLoadedMap;
	}

	public int getZoomDevice() {
		return mZoomDevice;
	}

	public boolean isMapPresent() {
		return mLoadedMap != null && mLoadedMap.isValid();
	}

	public int getZoomWear() {
		return mZoomWear;
	}

	public boolean isNavValid() {
		return (mStatus & NAV_VALID_FLAG) != 0;
	}

	public int getOffsetX() {
		return mOffsetX;
	}

	public int getOffsetY() {
		return mOffsetY;
	}

	public Location getLastLocation() {
		return mLastLocation;
	}

	public short getBearing() {
		return mBearing;
	}
}
