package com.asamm.locus.addon.wear.gui.custom;

import android.app.Activity;

import locus.api.android.utils.UtilsFormat;
import locus.api.objects.extra.TrackStats;

import static locus.api.android.utils.UtilsFormat.formatDouble;

/**
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */
@FunctionalInterface
public interface TrackStatConsumable {
	String consumeAndFormat(Activity context, TrackStats stats, boolean displayUnits);

	class TscFactory {
		public static TrackStatConsumable createTotalLengthMoveConsumable() {
			return (context, stats, displayUnits) -> {
				//((MainApplication)context.getApplication()).getCache().get
				// TODO cejnar load units from app cache
				int unitsFormat = 0;
				return stats == null ? "" :
						UtilsFormat.formatDistance(unitsFormat,
								stats.getTotalLengthMove(),
								displayUnits);
			};
		}

		public static TrackStatConsumable createTotalTimeConsumable() {
			return (context, stats, displayUnits) -> {
				return stats == null ? "" :
						formatTime(false, stats.getTotalTime(), displayUnits);
			};
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




