package com.asamm.locus.addon.wear.gui.custom;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;

import locus.api.android.utils.UtilsFormat;

import static locus.api.android.utils.UtilsFormat.formatDouble;

/**
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */
@FunctionalInterface
public interface TrackStatConsumable {
	String consumeAndFormat(TrackRecordingValue rec, boolean withoutUnits);

	class TscFactory {
		private static boolean isInvalidInput(TrackRecordingValue rec) {
			return rec == null || rec.getTrackRecStats() == null;
		}

		public static TrackStatConsumable createTotalLengthMoveConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatDistance(rec.getUnitsFormatLength(),
							rec.getTrackRecStats().getTotalLengthMove(),
							UtilsFormat.UnitsPrecision.HIGH,
							!withoutUnits);
		}

		public static TrackStatConsumable createAvgSpeedMoveConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatSpeed(rec.getUnitsFormatSpeed(),
							rec.getTrackRecStats().getSpeedAverage(true),
							withoutUnits);
		}

		public static TrackStatConsumable createSpeedMoveConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatSpeed(rec.getUnitsFormatSpeed(),
							rec.getSpeed(),
							withoutUnits);
		}

		public static TrackStatConsumable createMaxAltitudeConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
							rec.getTrackRecStats().getAltitudeMax(),
							!withoutUnits);
		}

		public static TrackStatConsumable createMinAltitudeConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
							rec.getTrackRecStats().getAltitudeMin(),
							!withoutUnits);
		}

		public static TrackStatConsumable createElevationUpConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
							rec.getTrackRecStats().getElePositiveHeight(),
							!withoutUnits);
		}

		public static TrackStatConsumable createElevationDownConsumable() {
			return (rec, withoutUnits) -> (isInvalidInput(rec)) ? "" :
					UtilsFormat.formatAltitude(rec.getUnitsFormatAltitude(),
							rec.getTrackRecStats().getEleNegativeHeight(),
							!withoutUnits);
		}

		public static TrackStatConsumable createTotalTimeConsumable() {
			return (rec, withoutUnits) -> isInvalidInput(rec) ? "" :
					formatTime(false, rec.getTrackRecStats().getTotalTime(), !withoutUnits);
		}

		public static TrackStatConsumable createBlankConsumable() {
			return (stats, withoutUnits) -> "";
		}


		private static String formatTime(boolean full, long time, boolean withUnits) {
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
}




