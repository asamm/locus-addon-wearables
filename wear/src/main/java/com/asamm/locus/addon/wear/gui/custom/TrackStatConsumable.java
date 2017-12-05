package com.asamm.locus.addon.wear.gui.custom;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackRecordingValue;
import com.asamm.locus.addon.wear.common.utils.Pair;

import locus.api.android.utils.UtilsFormat;

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

	class ValueUnitContainer {
		private final Pair<String, String> mValUnitPair;

		ValueUnitContainer(String value, String units) {
			mValUnitPair = Pair.of(value, units);
		}

		public String getValue() {
			return mValUnitPair.first;
		}

		public String getUnits() {
			return mValUnitPair.second;
		}

		public static ValueUnitContainer empty() {
			return new ValueUnitContainer("", "");
		}
	}

	class TscFactory {
		private static boolean isInvalidInput(TrackRecordingValue rec) {
			return rec == null || rec.getTrackRecStats() == null;
		}

		public static TrackStatConsumable createTotalLengthMoveConsumable() {
			return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
					new ValueUnitContainer(
							UtilsFormat.formatDistance(rec.getUnitsFormatLength(),
									rec.getTrackRecStats().getTotalLengthMove(),
									true),
							UtilsFormat.formatDistanceUnits(rec.getUnitsFormatLength(),
									rec.getTrackRecStats().getTotalLengthMove()));
		}

		public static TrackStatConsumable createAvgSpeedMoveConsumable() {
			return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
					new ValueUnitContainer(
							UtilsFormat.formatSpeed(rec.getUnitsFormatSpeed(),
									rec.getTrackRecStats().getSpeedAverage(true),
									true),
							UtilsFormat.formatSpeedUnits(rec.getUnitsFormatSpeed()));
		}

		public static TrackStatConsumable createSpeedMoveConsumable() {
			return (rec) -> (isInvalidInput(rec)) ? ValueUnitContainer.empty() :
					new ValueUnitContainer(
							UtilsFormat.formatSpeed(rec.getUnitsFormatSpeed(),
									rec.getSpeed(),
									true),
							UtilsFormat.formatSpeedUnits(rec.getUnitsFormatSpeed()));
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

		public static TrackStatConsumable createTotalTimeConsumable() {
			return (rec) -> isInvalidInput(rec) ? ValueUnitContainer.empty() :
					new ValueUnitContainer(
							formatTime(false, rec.getTrackRecStats().getTotalTime(), false),
							"");
		}

		public static TrackStatConsumable createBlankConsumable() {
			return (stats) -> ValueUnitContainer.empty();
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




