package com.asamm.locus.addon.wear.common.communication.containers.commands;

import com.asamm.locus.addon.wear.common.communication.containers.TimeStampStorable;

import java.io.IOException;

import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;

/*
 * Created by milan on 01.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class CommandFloatExtra extends TimeStampStorable {

    private float value = Float.NaN;

    public CommandFloatExtra() {
        super();
    }

    public CommandFloatExtra(float value) {
        this();
        this.value = value;
    }

    public CommandFloatExtra(byte[] data) throws IOException {
        super(data);
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        value = dr.readFloat();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeFloat(value);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public boolean isValid() {
        return !Float.isNaN(value);
    }
}
