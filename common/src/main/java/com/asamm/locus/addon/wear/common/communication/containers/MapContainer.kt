/**
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication.containers

import com.asamm.locus.addon.wear.common.communication.Const
import com.asamm.loggerV2.logE
import locus.api.android.MapPreviewResult
import locus.api.android.features.periodicUpdates.UpdateContainer
import locus.api.android.objects.LocusInfo
import locus.api.objects.extra.Location
import locus.api.objects.extra.PointRteAction
import locus.api.utils.DataReaderBigEndian
import locus.api.utils.DataWriterBigEndian
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Container for transfer of map screen and navigation information
 */
class MapContainer() : TimeStampStorable() {

    // result from loaded image
    var loadedMap: MapPreviewResult? = null
        private set

    // information about type of active guidance
    var guideType = UpdateContainer.GUIDE_TYPE_DISABLED

    var navPointAction1Id = PointRteAction.UNDEFINED.id

    var navPointAction2Id = PointRteAction.UNDEFINED.id

    var navPoint1Dist = 0.0
        private set

    var unitsFormatLength: Int = 0

    var zoomDevice = Const.ZOOM_UNKOWN

    var zoomRequest = Const.ZOOM_UNKOWN

    private var status: Byte = 0

    // V1

    // last applied offset in pixels used for rendering the included map
    var offsetX = 0
        private set

    var offsetY = 0
        private set

    var lastLocation = ZERO_LOCATION
        private set

    var bearing: Short = 0
        private set

    constructor(
            loadedMap: MapPreviewResult?, lastUpdate: UpdateContainer?, li: LocusInfo?,
            zoom: Int, offsetX: Int, offsetY: Int, lastLocation: Location?, bearing: Short)
            : this() {
        this.loadedMap = loadedMap
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.lastLocation = lastLocation ?: ZERO_LOCATION
        this.bearing = bearing
        if (lastUpdate != null) {
            zoomDevice = lastUpdate.mapZoomLevel.toByte()
            zoomRequest = zoom.toByte()
            guideType = lastUpdate.guideType
            val guide = lastUpdate.contentGuideTrack
            if (guide != null) {
                status = status or (if (guide.isValid) NAV_VALID_FLAG else 0).toByte()
                navPointAction1Id = guide.navPointFirst?.action?.id ?: navPointAction1Id
                navPointAction2Id = guide.navPointSecond?.action?.id ?: navPointAction2Id
                navPoint1Dist = guide.navPointFirst?.distance ?: navPoint1Dist
                if (li != null) {
                    unitsFormatLength = li.unitsFormatLength
                }
            }
        }
    }

    val isMapPresent: Boolean
        get() = loadedMap?.isValid() == true

    val isNavValid: Boolean
        get() = status and NAV_VALID_FLAG != 0.toByte()

    //*************************************************
    // STORABLE
    //*************************************************

    override fun getVersion(): Int {
        return 1
    }

    override fun readObject(version: Int, dr: DataReaderBigEndian) {
        super.readObject(version, dr)
        guideType = dr.readByte().toInt()
        navPointAction1Id = dr.readInt()
        navPointAction2Id = dr.readInt()
        navPoint1Dist = dr.readDouble()
        unitsFormatLength = dr.readByte().toInt()
        zoomDevice = dr.readByte()
        zoomRequest = dr.readByte()
        status = dr.readByte()
        val isMap = dr.readBoolean()
        try {
            loadedMap = if (isMap) dr.readStorable(MapPreviewResult::class.java) else null
        } catch (e: Exception) {
            loadedMap = null
            logE(ex = e) { "Could not read map image." }
        }

        // V1
        if (version >= 1) {
            offsetX = dr.readInt()
            offsetY = dr.readInt()
            bearing = dr.readShort()
            lastLocation = try {
                dr.readStorable(Location::class.java)
            } catch (e: Exception) {
                ZERO_LOCATION
            }
        }
    }

    override fun writeObject(dw: DataWriterBigEndian) {
        super.writeObject(dw)
        dw.write(guideType)
        dw.writeInt(navPointAction1Id)
        dw.writeInt(navPointAction2Id)
        dw.writeDouble(navPoint1Dist)
        dw.write(unitsFormatLength)
        dw.write(zoomDevice.toInt())
        dw.write(zoomRequest.toInt())
        dw.write(status.toInt())
        val isMap = loadedMap != null
        dw.writeBoolean(isMap)
        if (isMap) {
            dw.writeStorable(loadedMap)
        }

        // V1
        dw.writeInt(offsetX)
        dw.writeInt(offsetY)
        dw.writeShort(bearing.toInt())
        dw.writeStorable(lastLocation)
    }

    companion object {

        private const val NAV_VALID_FLAG: Byte = 0x1

        private val ZERO_LOCATION = Location(0.0, 0.0)
    }
}