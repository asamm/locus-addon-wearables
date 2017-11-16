package com.asamm.locus.addon.wear.common.communication.containers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/**
 * Simple bundle container for simple serialization of String Key-Value pairs;
 *
 * Not thread safe!!! Other threads must not mutate the map during de-/serialization!
 *
 *   TODO cejnar not used yet - delete?
 * Created by Milan Cejnar on 16.11.2017.
 * Asamm Software, s.r.o.
 */

public class StringBundleStorable extends TimeStampStorable {
    private HashMap<String, String> map;

    public StringBundleStorable() {
        super();
    }

    public StringBundleStorable(byte[] arr) throws IOException {
        super(arr);
    }

    @Override
    protected int getVersion() {
        return 0;

    }

    @Override
    public void reset() {
        super.reset();
        map = new HashMap<>(0);
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        map = new HashMap<>();
        int entryCnt = dr.readInt();
        for (int i = 0; i < entryCnt; i++) {
            String key = dr.readString();
            String value = dr.readString();
            map.put(key, value);
        }
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        int entryCnt = map.size();
        dw.writeInt(entryCnt);
        for (Map.Entry<String, String> e : map.entrySet()) {
            dw.writeString(e.getKey());
            dw.writeString(e.getValue());
        }
    }

    public void put(String key, String value) {
        String valueErr = key == null ? "Key" : value == null ? "Value" : null;
        if (valueErr != null) {
            throw new IllegalArgumentException(valueErr + " must not be null!");
        }
        map.put(key, value);
    }

    public String get(String key) {
        return map.get(key);
    }
}
