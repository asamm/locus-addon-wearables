/**
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication

import com.asamm.locus.addon.wear.common.communication.containers.HandShakeValue
import com.asamm.locus.addon.wear.common.communication.containers.MapContainer
import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable
import com.asamm.locus.addon.wear.common.communication.containers.commands.*
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileInfoValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingStateChangeValue
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue
import com.google.android.gms.wearable.DataItem
import java.util.*

/**
 * Specifies available Paths for data communications using DataAPI.
 *
 * Also includes other properties
 * - mark as urgent message
 * - support for Asset based communication instead of DataItem for bigger payloads
 */
enum class DataPath(
        container: Class<out TimeStampStorable>,
        val isUrgent: Boolean,
        val id: Byte) {

    GET_HAND_SHAKE(
            EmptyCommand::class.java,
            1.toByte()),

    PUT_HAND_SHAKE(
            HandShakeValue::class.java,
            2.toByte()),

    GET_TRACK_REC_PROFILES(
            EmptyCommand::class.java,
            3.toByte()),

    PUT_TRACK_REC_PROFILE_INFO(
            TrackProfileInfoValue.ValueList::class.java,
            4.toByte()),

    PUT_TRACK_REC(
            TrackRecordingValue::class.java,
            5.toByte()),

    PUT_TRACK_REC_STATE_CHANGE(
            TrackRecordingStateChangeValue::class.java,
            6.toByte()),

    GET_PROFILE_ICON(
            ProfileIconGetCommand::class.java,
            7.toByte()),

    PUT_PROFILE_ICON(
            TrackProfileIconValue::class.java,
            8.toByte()),

    // can be removed in future versions
    GET_ADD_WAYPOINT(
            EmptyCommand::class.java,
            9.toByte()),

    PUT_ADD_WAYPOINT(
            EmptyCommand::class.java,
            10.toByte()),

    GET_PERIODIC_DATA(
            PeriodicCommand::class.java,
            11.toByte()),

    GET_KEEP_ALIVE(
            EmptyCommand::class.java,
            12.toByte()),

    PUT_MAP(
            MapContainer::class.java,
            13.toByte()),

    /**
     * Fake communication data path, used for signalling activity about ON_CONNECTED event inside
     * strictly the application. Should not be used over network.
     */
    PUT_ON_CONNECTED_EVENT(
            EmptyCommand::class.java,
            14.toByte()),

    POST_ADD_WAYPOINT(
            CommandStringExtra::class.java,
            15.toByte()),

    PUT_HEART_RATE(
            CommandFloatExtra::class.java,
            16.toByte()),

    /**
     * Sent if track recording end detected but device is receiving sensor data
     * from watch track recording service
     */
    STOP_WATCH_TRACK_REC_SERVICE(
            EmptyCommand::class.java,
            17.toByte()),

    /**
     * Special keep alive data packet sent to notify the watch running foreground track rec service
     * that the phone app is still alive and connected .
     */
    DEVICE_KEEP_ALIVE(
            EmptyCommand::class.java,
            18.toByte());

    val key = name.toLowerCase(Locale.ROOT)
    val path = "/locus/wear/$key"
    val containerClass: Class<out TimeStampStorable> = container

    constructor(container: Class<out TimeStampStorable>, id: Byte)
            : this(container, URGENT_DEFAULT, id)

    companion object {

        private val values = values()

        const val DEFAULT_ASSET_KEY = ":"

        /**
         * default urgent setting if not explicitly specified
         */
        private const val URGENT_DEFAULT = true

        fun valueOf(item: DataItem): DataPath? {
            return fromPath(item.uri.path)
        }

        @JvmStatic
        fun fromPath(path: String?): DataPath? {
            for (p in values) {
                if (p.path == path) {
                    return p
                }
            }
            return null
        }

        @JvmStatic
        fun valueOf(id: Byte): DataPath? {
            for (p in values) {
                if (p.id == id) {
                    return p
                }
            }
            return null
        }
    }
}