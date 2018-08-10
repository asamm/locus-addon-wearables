package com.asamm.locus.addon.wear.gui.custom;

import android.text.Html;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.common.utils.Pair;
import com.asamm.locus.addon.wear.gui.trackrec.recording.sensors.RecordingSensorStore;
import com.asamm.locus.addon.wear.utils.UtilsFormatWear;

import java.text.DateFormat;
import java.util.Date;

import locus.api.android.utils.UtilsFormat;

import static com.asamm.locus.addon.wear.utils.UtilsFormatWear.formatTime;
import static locus.api.android.utils.UtilsFormat.formatDouble;

/**
 * Interface which takes in Recording value and units settings
 * and depending on implementation returns formatted value of implementation
 * specific statistics/measured value
 * <p>
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */
@FunctionalInterface
public interface TrackStatConsumable {
    ValueUnitContainer consumeAndFormat(TrackRecordingValue rec);

    /** Value container used as a result value container for statistics dashboard */
    class ValueUnitContainer {
        private final Pair<String, CharSequence> mValUnitPair;

        ValueUnitContainer(String value, CharSequence units) {
            mValUnitPair = Pair.of(value, units);
        }

        public String getValue() {
            return mValUnitPair.first;
        }

        public CharSequence getUnits() {
            return mValUnitPair.second;
        }

        public static ValueUnitContainer empty() {
            return new ValueUnitContainer("", "");
        }
    }

    /** Factory for different strategies how to get result value and unit for various statistics */
    class TscFactory {
        public static enum TrackStatConsumableModifierEnum {
            CURRENT, AVG, MAX, MIN
        }

        private static boolean isInvalidInput(TrackRecordingValue rec) {
            return rec == null || rec.getTrackRecStats() == null;
        }

        public static TrackStatConsumable createTotalLengthMoveConsumable() {
            return (rec) -> createDistanceConsumable(rec, () -> rec.getTrackRecStats().getTotalLengthMove());
        }

        public static TrackStatConsumable createAvgSpeedConsumable(boolean moveOnly) {
            return (rec) -> createSpeedConsumable(rec, () -> rec.getTrackRecStats().getSpeedAverage(moveOnly));
        }

        public static TrackStatConsumable createSpeedMaxConsumable() {
            return (rec) -> createSpeedConsumable(rec, () -> rec.getTrackRecStats().getSpeedMax());
        }

        public static TrackStatConsumable createSpeedMoveConsumable() {
            return (rec) -> createSpeedConsumable(rec, () -> rec.getSpeed());
        }

        public static TrackStatConsumable createHrmConsumable(TrackStatConsumableModifierEnum type) {
            return (rec) -> {
                if (isInvalidInput(rec)) return ValueUnitContainer.empty();
                final int hrm;

                switch (type) {
                    case AVG:
                        hrm = rec.getTrackRecStats().getHrmAverage();
                        break;
                    case MAX:
                        hrm = rec.getTrackRecStats().getHrmMax();
                        break;
                    default:
                        hrm = rec.getHrm();
                        break;
                }
                return new ValueUnitContainer(String.valueOf(hrm), "BPM");
            };
        }

        public static TrackStatConsumable createDistanceUphillConsumable() {
            return (rec) -> createDistanceConsumable(rec, () -> rec.getTrackRecStats().getElePositiveDistance());
        }
        public static TrackStatConsumable createDistanceDownhillConsumable() {
            return (rec) -> createDistanceConsumable(rec, () -> rec.getTrackRecStats().getEleNegativeDistance());
        }

        public static TrackStatConsumable createElevationUpConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
                                    rec.getTrackRecStats().getElePositiveHeight(),
                                    false),
                            UtilsFormat.formatAltitudeUnits(rec.getUnitsFormatAltitude()));
        }

        public static TrackStatConsumable createElevationDownConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
                                    rec.getTrackRecStats().getEleNegativeHeight(),
                                    false),
                            UtilsFormat.formatAltitudeUnits(rec.getUnitsFormatAltitude()));
        }

        public static TrackStatConsumable createAltitudeConsumable(TrackStatConsumableModifierEnum type) {
            return (rec) -> {
                if (isInvalidInput(rec)) return ValueUnitContainer.empty();

                final double altitude;
                switch (type) {
                    case MAX:
                        altitude = rec.getTrackRecStats().getAltitudeMax();
                        break;
                    case MIN:
                        altitude = rec.getTrackRecStats().getAltitudeMin();
                        break;
                    default:
                        altitude = Float.isNaN(rec.getAltitude()) ? 0f : rec.getAltitude();
                        break;
                }
                return new ValueUnitContainer(
                        UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
                                altitude, false),
                        UtilsFormat.formatAltitudeUnits(rec.getUnitsFormatAltitude()));
            };
        }

        public static TrackStatConsumable createTotalTimeConsumable() {
            return (rec) -> isInvalidInput(rec) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            formatTime(false, rec.getTrackRecStats().getTotalTime(), false),
                            "");
        }

        public static TrackStatConsumable createTotalTimeMoveConsumable() {
            return (rec) -> isInvalidInput(rec) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            formatTime(false, rec.getTrackRecStats().getTotalTimeMove(), false),
                            "");
        }

        public static TrackStatConsumable createEnergyConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            UtilsFormat.formatEnergy(rec.getUnitsFormatEnergy(),
                                    rec.getTrackRecStats().getEnergy(), false),
                            UtilsFormat.formatEnergyUnit(rec.getUnitsFormatEnergy()));
        }

        private static CharSequence getCadenceUnit() {
            return Html.fromHtml("min<sup>-1</sup>");
        }

        public static TrackStatConsumable createCadenceAvgConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            String.valueOf(rec.getTrackRecStats().getCadenceAverage()),
                            getCadenceUnit());
        }

        public static TrackStatConsumable createCadenceMaxConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            String.valueOf(rec.getTrackRecStats().getCadenceMax()),
                            getCadenceUnit());
        }

        public static TrackStatConsumable createPaceConsumable() {
            return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
                    new ValueUnitContainer(
                            UtilsFormatWear.formatPace(
                                    rec.getTrackRecStats().getTotalTime(),
                                    rec.getTrackRecStats().getTotalLength(),
                                    rec.getUnitsFormatLength()),
                            UtilsFormatWear.formatPaceUnit(rec.getUnitsFormatLength()));
        }


        public static TrackStatConsumable createCurrentTimeConsumable() {
            return (rec) ->
                    new ValueUnitContainer(DateFormat.getTimeInstance().format(new Date()), "");
        }

        public static TrackStatConsumable createBatteryConsumable() {
            return (rec) -> {
                // TODO cejnar
                String value = RecordingSensorStore.getBatteryValue().isValid() ?
                        String.valueOf(RecordingSensorStore.getBatteryValue().getValue()) : "???";
                return new ValueUnitContainer(value, "%");
            };
        }

        public static TrackStatConsumable createBlankConsumable() {
            return (stats) -> ValueUnitContainer.empty();
        }

        /** Interface is used to provide lazy loaded floats which are safely available only after
         * their parents container null checks */
        @FunctionalInterface
        private interface FloatProducer {
            float produce();
        }
        private static ValueUnitContainer createSpeedConsumable(TrackRecordingValue rec, FloatProducer speedProducer) {
            if (isInvalidInput(rec)) return ValueUnitContainer.empty();
            return new ValueUnitContainer(
                    UtilsFormat.formatSpeed(rec.getUnitsFormatSpeed(), speedProducer.produce(), true),
                    UtilsFormat.formatSpeedUnits(rec.getUnitsFormatSpeed()));
        }

        private static ValueUnitContainer createDistanceConsumable(TrackRecordingValue rec, FloatProducer distanceProducer) {
            if (isInvalidInput(rec)) return ValueUnitContainer.empty();
            float distance = distanceProducer.produce();
            return new ValueUnitContainer(
                    UtilsFormat.formatDistance(rec.getUnitsFormatLength(),
                            distance, true),
                    UtilsFormat.formatDistanceUnits(rec.getUnitsFormatLength(), distance));
        }
    }
}




