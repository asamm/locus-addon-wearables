/*
 * Created by milan on 09.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.application;

/**
 * Enum for representing state of configuration of some feature/sensor etc.
 */
public enum FeatureConfigEnum {
    ENABLED((byte) 0),
    DISABLED((byte) 1),
    NOT_AVAILABLE((byte) 2);

    private byte id;

    FeatureConfigEnum(byte id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static FeatureConfigEnum getById(int id) {
        for (FeatureConfigEnum it : values()) {
            if (it.id == id) return it;
        }
        return NOT_AVAILABLE;
    }

}
