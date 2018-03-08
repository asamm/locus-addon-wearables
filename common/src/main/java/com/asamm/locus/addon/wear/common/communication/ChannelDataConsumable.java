package com.asamm.locus.addon.wear.common.communication;

import com.asamm.locus.addon.wear.common.communication.containers.DataPayloadStorable;

/**
 * Created by Milan Cejnar on 08.03.2018.
 * Asamm Software, s.r.o.
 */

public interface ChannelDataConsumable {
	void consumeData(DataPayloadStorable data);
}
