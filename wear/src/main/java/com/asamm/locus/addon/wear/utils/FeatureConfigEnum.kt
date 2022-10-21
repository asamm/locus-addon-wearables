/*
 * Created by milan on 09.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.utils

/**
 * Enum for representing state of configuration of some feature/sensor etc.
 */
enum class FeatureConfigEnum(val id: Int) {

    ENABLED(0),

    DISABLED(1),

    NOT_AVAILABLE(2),

    NO_PERMISSION(3);

    companion object {

        fun getById(id: Int): FeatureConfigEnum {
            for (it in values()) {
                if (it.id == id) return it
            }
            return NOT_AVAILABLE
        }
    }
}