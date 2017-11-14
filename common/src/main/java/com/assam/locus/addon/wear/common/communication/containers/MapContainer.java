package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Created by Milan Cejnar on 14.11.2017.
 * Asamm Software, s.r.o.
 */

public class MapContainer extends TimeStampStorable {
// TODO cejnar
    @Override
    public void reset() {
        super.reset();
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
    }

    @Override
    protected int getVersion() {
        return 0;
    }
}
