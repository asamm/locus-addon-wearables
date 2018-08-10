/*
 * Created by milan on 10.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.utils;

import locus.api.android.utils.UtilsFormat;

import static locus.api.android.utils.UtilsFormat.VALUE_UNITS_DISTANCE_IM_F;
import static locus.api.android.utils.UtilsFormat.VALUE_UNITS_DISTANCE_IM_FM;
import static locus.api.android.utils.UtilsFormat.VALUE_UNITS_DISTANCE_IM_Y;
import static locus.api.android.utils.UtilsFormat.VALUE_UNITS_DISTANCE_IM_YM;
import static locus.api.android.utils.UtilsFormat.VALUE_UNITS_DISTANCE_NA_MNMI;
import static locus.api.android.utils.UtilsFormat.formatDouble;

/**
 * Only addded to supply some formatting functions from Android or Locus core to
 * stay dependent only on the API
 */
public class UtilsFormatWear {
    //*************************************************/
    // PACE
    //*************************************************/

    /**
     * Format pace value.
     *
     * @param time               time value for certain distance
     * @param dist               traveled distance
     * @param distanceFormatType user distance format
     * @return formatted value
     */
    public static String formatPace(long time, double dist, int distanceFormatType) {
        String value = formatTime(false, formatPaceValue(time, dist, distanceFormatType), false);
        return value;
    }

    /**
     * Format value for pace.
     *
     * @param time               time value for certain distance [in ms]
     * @param dist               traveled distance [in m]
     * @param distanceFormatType user distance format
     * @return formatted value [in ms / dist]
     */
    public static long formatPaceValue(long time, double dist, int distanceFormatType) {
        // check minimal distance
        if (dist < 1.0) {
            return 0L;
        }
        double distFormatted = doDistanceUnitFormatting(distanceFormatType,
                () -> dist / UtilsFormat.UNIT_KILOMETER_TO_METER,
                () -> dist / UtilsFormat.UNIT_MILE_TO_METER,
                () -> dist / UtilsFormat.UNIT_NMILE_TO_METER);
        return (long) (time / distFormatted);
    }

    /**
     * Get formatted units for pace.
     *
     * @param distanceFormatType user distance format
     * @return formatted units
     */
    public static String formatPaceUnit(int distanceFormatType) {
        return doDistanceUnitFormatting(distanceFormatType,
                () -> "min/km", () -> "min/mile", () -> "min/nmile");
    }

    @FunctionalInterface
    public interface SupplierCompat<T> {
        T get();
    }

    /**
     * @param distanceFormatType user distance format
     * @param formatMetric       metric  formatter strategy
     * @param formatImperial     imperial formatter strategy
     * @param formatNautical     natutical formatter strategy
     * @param <T>                Output type
     * @return formatted output depending on selected imperial/nautical/metric strategy for given distanceFormatType
     */
    private static <T> T doDistanceUnitFormatting(int distanceFormatType, SupplierCompat<T> formatMetric,
                                                  SupplierCompat<T> formatImperial, SupplierCompat<T> formatNautical) {
        switch (distanceFormatType) {
            case VALUE_UNITS_DISTANCE_IM_F:
            case VALUE_UNITS_DISTANCE_IM_FM:
            case VALUE_UNITS_DISTANCE_IM_Y:
            case VALUE_UNITS_DISTANCE_IM_YM:
                return formatImperial.get();
            case VALUE_UNITS_DISTANCE_NA_MNMI:
                return formatNautical.get();
            default:
                return formatMetric.get();
        }
    }

    public static String formatTime(boolean full, long time, boolean withUnits) {
        long hours = time / 3600000;
        long mins = (time - (hours * 3600000)) / 60000;
        double sec = (time - (hours * 3600000) - mins * 60000) / 1000.0;
        if (sec > 59.5) {
            mins++;
            sec = 0.0;
        }
        if (mins > 59.5) {
            hours++;
            mins = 0;
        }

        if (full) {
            if (withUnits) {
                return hours + "h:" +
                        formatDouble(mins, 0, 2) + "m:" +
                        formatDouble(sec, 0, 2) + "s";
            } else {
                return formatDouble(hours, 0, 2) + ":" +
                        formatDouble(mins, 0, 2) + ":" +
                        formatDouble(sec, 0, 2);
            }
        } else {
            if (hours == 0) {
                if (mins == 0) {
                    if (withUnits) {
                        return "00m:" + formatDouble(sec, 0, 2) + "s";
                    } else {
                        return "00:" + formatDouble(sec, 0, 2);
                    }
                } else {
                    if (withUnits) {
                        return formatDouble(mins, 0, 2) + "m:" + formatDouble(sec, 0, 2) + "s";
                    } else {
                        return formatDouble(mins, 0, 2) + ":" + formatDouble(sec, 0, 2);
                    }
                }
            } else {
                if (withUnits) {
                    return hours + "h:" + formatDouble(mins, 0, 2) + "m";
                } else {
                    return formatDouble(hours, 0, 2) + ":" +
                            formatDouble(mins, 0, 2) + ":" +
                            formatDouble(sec, 0, 2);
                }
            }
        }
    }
}
