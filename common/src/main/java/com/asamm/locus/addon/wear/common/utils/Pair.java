package com.asamm.locus.addon.wear.common.utils;

/**
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public class Pair<FIRST, SECOND> {
    public final FIRST first;
    public final SECOND second;

    public Pair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    public static <FIRST, SECOND> Pair<FIRST, SECOND> of(FIRST first, SECOND second) {
        return new Pair<>(first, second);
    }
}
