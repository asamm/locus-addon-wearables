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

private enum class Target {
    DEVICE,
    WEAR,
    UNDEFINED,
}

/**
 * Specifies available Paths for data communications using DataAPI.
 *
 * Also includes other properties
 * - mark as urgent message
 * - support for Asset based communication instead of DataItem for bigger payloads
 *
 * Items are separated by the target to the
 * - DEVICE > path has prefix "/locus/toDevice" and is registered only on the device module. Such items
 *     has prefix "TD_".
 * - WEAR > path has prefix "/locus/toWear" and is registered only on the wear module. Such items
 *     has prefix "TW_".
 * - UNDEFINED > path has prefix "/locus/wear" and is registered in both modules. This is only system,
 *     where both modules received events. This is not recommended to use if not intended.
 */
enum class DataPath(
        container: Class<out TimeStampStorable>,
        val isUrgent: Boolean = true,
        private val target: Target = Target.UNDEFINED,
        val id: Byte) {

    /**
     * Initial request To Device to "Get hand shake" object.
     */
    TD_GET_HAND_SHAKE(
            container = EmptyCommand::class.java,
            target = Target.DEVICE,
            id = 1.toByte()
    ),

    /**
     * Response to "Get hand shake" To Wear with [HandShakeValue] object.
     */
    TW_PUT_HAND_SHAKE(
            container = HandShakeValue::class.java,
            target = Target.WEAR,
            id = 2.toByte()
    ),

    GET_TRACK_REC_PROFILES(
            EmptyCommand::class.java,
            3.toByte()
    ),

    PUT_TRACK_REC_PROFILE_INFO(
            TrackProfileInfoValue.ValueList::class.java,
            4.toByte()
    ),

    PUT_TRACK_REC(
            TrackRecordingValue::class.java,
            5.toByte()
    ),

    PUT_TRACK_REC_STATE_CHANGE(
            TrackRecordingStateChangeValue::class.java,
            6.toByte()
    ),

    GET_PROFILE_ICON(
            ProfileIconGetCommand::class.java,
            7.toByte()
    ),

    PUT_PROFILE_ICON(
            TrackProfileIconValue::class.java,
            8.toByte()
    ),

    // can be removed in future versions
    GET_ADD_WAYPOINT(
            EmptyCommand::class.java,
            9.toByte()
    ),

    PUT_ADD_WAYPOINT(
            EmptyCommand::class.java,
            10.toByte()
    ),

    /**
     * Request for a new periodic updates container.
     */
    TD_GET_PERIODIC_DATA(
            container = PeriodicCommand::class.java,
            target = Target.DEVICE,
            id = 11.toByte()
    ),

    /**
     * Container with the map & necessary map content send from running app to The Watches.
     */
    TW_PUT_MAP(
            container = MapContainer::class.java,
            target = Target.WEAR,
            id = 13.toByte()
    ),

    /**
     * Fake communication data path, used for signalling activity about ON_CONNECTED event inside
     * strictly the application. Should not be used over network.
     */
    PUT_ON_CONNECTED_EVENT(
            EmptyCommand::class.java,
            14.toByte()
    ),

    POST_ADD_WAYPOINT(
            CommandStringExtra::class.java,
            15.toByte()
    ),

    PUT_HEART_RATE(
            CommandFloatExtra::class.java,
            16.toByte()
    ),

    /**
     * Sent if track recording end detected but device is receiving sensor data
     * from watch track recording service
     */
    STOP_WATCH_TRACK_REC_SERVICE(
            EmptyCommand::class.java,
            17.toByte()
    ),

    /**
     * Special keep alive data packet sent to notify The Device that the watch app is still alive and connected.
     */
    TD_KEEP_ALIVE(
            container = EmptyCommand::class.java,
            target = Target.DEVICE,
            id = 12.toByte()
    ),

    /**
     * Special keep alive data packet sent to notify The Watch, that the phone app is still alive and connected.
     */
    TW_KEEP_ALIVE(
            container = EmptyCommand::class.java,
            target = Target.WEAR,
            id = 18.toByte()
    );

    /**
     * Get unique "path" identifier of target listener.
     */
    val path = when (target) {
        Target.DEVICE -> "/locus/toDevice/${name.lowercase()}"
        Target.WEAR -> "/locus/toWear/${name.lowercase()}"
        Target.UNDEFINED -> "/locus/wear/${name.lowercase()}"
    }

    val containerClass: Class<out TimeStampStorable> = container

    constructor(container: Class<out TimeStampStorable>, id: Byte)
            : this(container, true, Target.UNDEFINED, id)

    override fun toString(): String {
        return "DataPath [$name, " +
                "container: ${containerClass.simpleName}, " +
                "target: $target, " +
                "urgent: $isUrgent, " +
                "id: $id, " +
                "path: $path]"
    }

    companion object {

        private val values = values()

        const val DEFAULT_ASSET_KEY = ":"

        fun valueOf(item: DataItem): DataPath? {
            return fromPath(item.uri.path)
        }

        @JvmStatic
        fun fromPath(path: String?): DataPath? {
            return values.find { it.path == path }
        }

        @JvmStatic
        fun valueOf(id: Byte): DataPath? {
            return values.find { it.id == id }
        }
    }
}