/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication.containers

import locus.api.objects.Storable
import locus.api.utils.DataReaderBigEndian
import locus.api.utils.DataWriterBigEndian
import java.io.IOException

/**
 * This Storable class contains cropped 4B long timestamp which ensures that command are always sent when
 * using DataApi DataItem for transfer. (Sending the same dataItem twice doesn't trigger data changed listener)
 */
abstract class TimeStampStorable() : Storable() {

    /**
     * Cropped 4B timestamp.
     */
    var timeStampInt = System.currentTimeMillis().toInt()

    constructor(byteArray: ByteArray) : this() {
        read(byteArray)
    }

    //*************************************************
    // STORABLE
    //*************************************************

    @Throws(IOException::class)
    override fun readObject(version: Int, dr: DataReaderBigEndian) {
        timeStampInt = dr.readInt()
    }

    @Throws(IOException::class)
    override fun writeObject(dw: DataWriterBigEndian) {
        dw.writeInt(timeStampInt)
    }
}