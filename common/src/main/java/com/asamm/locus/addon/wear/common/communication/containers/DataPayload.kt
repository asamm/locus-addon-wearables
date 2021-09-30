/**
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear.common.communication.containers

import com.asamm.locus.addon.wear.common.communication.DataPath

/**
 * Container wrapping both DataPath and Data to be sent
 */
class DataPayload<E : TimeStampStorable>(val path: DataPath, val storable: E)