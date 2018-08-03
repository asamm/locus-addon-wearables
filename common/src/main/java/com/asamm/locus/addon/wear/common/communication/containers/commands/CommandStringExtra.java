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
public class CommandStringExtra extends TimeStampStorable {
    private String value;

    public CommandStringExtra() {
        super();
    }

    public CommandStringExtra(String value) {
        this();
        this.value = value;
    }

    public CommandStringExtra(byte[] data) throws IOException {
        super(data);
    }

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        super.reset();
        value = "";
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        super.readObject(version, dr);
        value = dr.readString();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        super.writeObject(dw);
        dw.writeString(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
