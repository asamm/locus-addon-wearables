/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication.containers

import locus.api.utils.DataReaderBigEndian
import locus.api.utils.DataWriterBigEndian
import java.io.IOException
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class HandShakeValue() : TimeStampStorable() {

    var locusVersion = -1
        private set

    var addOnVersion = -1

    /**
     * Flag if Locus app is running.
     */
    var isLocusRunning: Boolean
        get() = statusFlag and STATUS_LOCUS_RUNNING != 0.toByte()
        set(b) {
            setFlag(b, STATUS_LOCUS_RUNNING)
        }

    private var statusFlag: Byte = 0

    constructor(locusVersion: Int, addonVersion: Int, locusRunning: Boolean) : this() {
        this.locusVersion = locusVersion
        addOnVersion = addonVersion
        isLocusRunning = locusRunning
    }

    val isEmpty: Boolean
        get() = locusVersion < 0

    private fun setFlag(b: Boolean, flag: Byte) {
        statusFlag = if (b) {
            statusFlag or flag
        } else {
            statusFlag and flag.inv()
        }
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
        locusVersion = dr.readInt()
        statusFlag = dr.readBytes(1)[0]
        if (version >= 1) {
            addOnVersion = dr.readInt()
        }
    }

    @Throws(IOException::class)
    override fun writeObject(dw: DataWriterBigEndian) {
        super.writeObject(dw)
        dw.writeInt(locusVersion)
        dw.write(statusFlag.toInt())
        dw.writeInt(addOnVersion)
    }

    companion object {

        private const val STATUS_LOCUS_RUNNING: Byte = 1
    }
}