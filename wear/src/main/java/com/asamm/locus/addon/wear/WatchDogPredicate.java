package com.asamm.locus.addon.wear;

/**
 * Predicate interface used by watchdog for parametrizing specific condition on received data.
 *
 * Created by Milan Cejnar on 02.12.2017.
 * Asamm Software, s.r.o.
 */

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

@FunctionalInterface
public interface WatchDogPredicate<RESPONSE extends TimeStampStorable>  {
	boolean test (RESPONSE r);
}
