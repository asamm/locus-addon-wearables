package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

/**
 * Blank data place holder for empty "command payloads"
 * Created by Milan Cejnar on 08.11.2017.
 * Asamm Software, s.r.o.
 */

public class EmptyCommand extends TimeStampStorable {

	public EmptyCommand() {
		super();
	}

	@Override
	protected int getVersion() {
		return 0;
	}
}
