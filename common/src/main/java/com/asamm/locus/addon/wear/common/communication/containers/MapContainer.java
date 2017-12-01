package com.asamm.locus.addon.wear.common.communication.containers;

import com.asamm.locus.addon.wear.common.communication.Const;

import java.io.IOException;

import locus.api.android.ActionTools;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusInfo;
import locus.api.objects.enums.PointRteAction;
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

	// result from loaded image
	private ActionTools.BitmapLoadResult mLoadedMap;

	// information about type of active guidance
	private int mGuideType;
	private int mNavPointAction1Id;
	private int mNavPointAction2Id;
	private double mNavPoint1Dist;
	private int mUnitsFormatLength;
	private int mZoomDevice;
	private int mZoomWear;

	public MapContainer() {
		super();
	}

	public MapContainer(byte[] byteArray) throws IOException {
		super(byteArray);
	}

	public MapContainer(ActionTools.BitmapLoadResult loadedMap, UpdateContainer mLastUpdate, LocusInfo li, int zoom) {
		this();
		mLoadedMap = loadedMap;
		if (mLastUpdate != null) {
			mZoomDevice = mLastUpdate.getMapZoomLevel();
			mZoomWear = zoom;
			mGuideType = mLastUpdate.getGuideType();
			UpdateContainer.GuideTypeTrack guide = mLastUpdate.getGuideTypeTrack();
			if (guide != null) {
				mNavPointAction1Id = guide.getNavPoint1Action().getId();
				mNavPointAction2Id = guide.getNavPoint2Action().getId();
				mNavPoint1Dist = guide.getNavPoint1Dist();
				if (li != null) {
					mUnitsFormatLength = li.getUnitsFormatLength();
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
	}

	@Override
	protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
		super.readObject(version, dr);
		mGuideType = dr.readInt();
		mNavPointAction1Id = dr.readInt();
		mNavPointAction2Id = dr.readInt();
		mNavPoint1Dist = dr.readDouble();
		mUnitsFormatLength = dr.readInt();
		mZoomDevice = dr.readInt();
		mZoomWear = dr.readInt();
		boolean isMap = dr.readBoolean();
		try {
			mLoadedMap = isMap ? (ActionTools.BitmapLoadResult) dr.readStorable(ActionTools.BitmapLoadResult.class) : null;
		} catch (Exception e) {
			mLoadedMap = null;
			Logger.logE(TAG, "Could not read map image.", e);
		}
	}

	@Override
	protected void writeObject(DataWriterBigEndian dw) throws IOException {
		super.writeObject(dw);
		dw.writeInt(mGuideType);
		dw.writeInt(mNavPointAction1Id);
		dw.writeInt(mNavPointAction2Id);
		dw.writeDouble(mNavPoint1Dist);
		dw.writeInt(mUnitsFormatLength);
		dw.writeInt(mZoomDevice);
		dw.writeInt(mZoomWear);
		boolean isMap = mLoadedMap != null;
		dw.writeBoolean(isMap);
		if (isMap) {
			dw.writeStorable(mLoadedMap);
		}
	}

	@Override
	protected int getVersion() {
		return 0;
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

	public ActionTools.BitmapLoadResult getLoadedMap() {
		return mLoadedMap;
	}

	public int getZoomDevice() {
		return mZoomDevice;
	}

	public boolean isMapPresent() {
		return mLoadedMap != null && mLoadedMap.getImage() != null;
	}

	public int getZoomWear() {
		return mZoomWear;
	}
}
