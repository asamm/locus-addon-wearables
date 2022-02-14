/**
 * Created by Milan Cejnar on 02.12.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable

/**
 * Predicate interface used by watchdog for parametrizing specific condition on received data.
 */
fun interface WatchDogPredicate<RESPONSE : TimeStampStorable> {
    fun test(r: RESPONSE): Boolean
}