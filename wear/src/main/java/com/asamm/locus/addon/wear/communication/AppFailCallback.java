package com.asamm.locus.addon.wear.communication;

import com.asamm.locus.addon.wear.gui.error.AppFailType;

/**
 * Definition of callback interface for application wide failure handling
 * <p>
 * Created by Milan Cejnar on 29.11.2017.
 * Asamm Software, s.r.o.
 */
@FunctionalInterface
public interface AppFailCallback {
	void onAppFail(AppFailType failType);
}
