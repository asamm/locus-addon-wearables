package com.asamm.locus.addon.wear.common.communication.containers.commands

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.PeriodicCommand.PeriodicExtra
import locus.api.utils.DataReaderBigEndian
import locus.api.utils.DataWriterBigEndian
import java.io.IOException
import kotlin.experimental.and

/**
 * Parameters specifying size and zoom of requested map
 *
 *
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */
class MapPeriodicParams() : TimeStampStorable(), PeriodicExtra {

    // version 0
    var zoom = 0
        private set

    var width = 0
        private set

    var height = 0
        private set

    // version 1
    /**
     * Flags:
     * 0x1 - AutoRotate
     */
    private var bFlags: Byte = 0

    var bearing: Short = 0
        private set

    var diagonal: Int = 0
        private set

    var offsetX = 0
        private set

    var offsetY = 0
        private set

    var densityDpi: Int = 0

    // nonzero when offset is used
    var lastLatitude = 0.0
        private set

    var lastLongitude = 0.0
        private set

    val isAutoRotate: Boolean
        get() = bFlags and FLG_AUTOROTATE != 0.toByte()

    constructor(
            mZoom: Int, mWidth: Int, mHeight: Int,
            offsetX: Int, offsetY: Int, densityDpi: Int,
            isAutoRotate: Boolean, bearing: Short, diagonal: Int,
            lastOffsetLatitude: Double, lastOffsetLongitude: Double) : this() {
        zoom = mZoom
        width = mWidth
        height = mHeight
        bFlags = if (isAutoRotate) FLG_AUTOROTATE else 0
        this.offsetX = offsetX
        this.offsetY = offsetY
        this.densityDpi = densityDpi
        this.diagonal = diagonal
        lastLatitude = lastOffsetLatitude
        lastLongitude = lastOffsetLongitude
        this.bearing = bearing
    }

    //*************************************************
    // STORABLE
    //*************************************************

    override fun getVersion(): Int {
        return 1
    }

    @Throws(IOException::class)
    override fun readObject(version: Int, dr: DataReaderBigEndian) {
        super.readObject(version, dr)
        zoom = dr.readInt()
        width = dr.readInt()
        height = dr.readInt()

        // V1
        if (version >= 1) {
            offsetX = dr.readInt()
            offsetY = dr.readInt()
            densityDpi = dr.readShort().toInt()
            diagonal = dr.readShort().toInt()
            bFlags = dr.readBytes(1)[0]
            lastLatitude = dr.readDouble()
            lastLongitude = dr.readDouble()
            bearing = dr.readShort()
        }
    }

    @Throws(IOException::class)
    override fun writeObject(dw: DataWriterBigEndian) {
        super.writeObject(dw)
        dw.writeInt(zoom)
        dw.writeInt(width)
        dw.writeInt(height)

        // V1
        dw.writeInt(offsetX)
        dw.writeInt(offsetY)
        dw.writeShort(densityDpi)
        dw.writeShort(diagonal)
        dw.write(bFlags.toInt())
        dw.writeDouble(lastLatitude)
        dw.writeDouble(lastLongitude)
        dw.writeShort(bearing.toInt())
    }

    companion object {

        /**
         * code for mBearing property which indicates that device is free to use its last known bearing
         */
        const val APPLY_DEVICE_BEARING = Short.MIN_VALUE

        private const val FLG_AUTOROTATE: Byte = 0x1
    }
}