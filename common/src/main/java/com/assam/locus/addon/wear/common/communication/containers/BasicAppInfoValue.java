package com.assam.locus.addon.wear.common.communication.containers;

import java.io.IOException;
import java.util.List;

import locus.api.android.ActionTools;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

/**
 * Created by menion on 07/08/15.
 * Asamm Software, s. r. o.
 */
public class BasicAppInfoValue extends TimeStampStorable {

    // tag for logger
    private static final String TAG = BasicAppInfoValue.class.getSimpleName();

    // currently selected units for 'Altitude'
    private int mUnitsFormatAltitude;
    // currently selected units for 'Angles'
    private int mUnitsFormatAngle;
    // currently selected units for 'Areas'
    private int mUnitsFormatArea;
    // currently selected units for 'Energy'
    private int mUnitsFormatEnergy;
    // currently selected units for 'Length'
    private int mUnitsFormatLength;
    // currently selected units for 'Slope'
    private int mUnitsFormatSlope;
    // currently selected units for 'Speed'
    private int mUnitsFormatSpeed;
    // currently selected units for 'Temperature'
    private int mUnitsFormatTemperature;
    // currently selected units for 'Weight'
    private int mUnitsFormatWeight;

    /**
     * Base constructor mainly for a Storable class.
     */
    @SuppressWarnings("unused")
    public BasicAppInfoValue() {
        super();
    }

    /**
     * Constructor based on raw byte array.
     * @param data packed data
     * @throws IOException
     */
    public BasicAppInfoValue(byte[] data) throws IOException {
        super(data);
    }


    /**************************************************/
    // STORABLE PART
    /**************************************************/

    @Override
    protected int getVersion() {
        return 0;
    }

    @Override
    public void reset() {
        mUnitsFormatAltitude = 0;
        mUnitsFormatAngle = 0;
        mUnitsFormatArea = 0;
        mUnitsFormatEnergy = 0;
        mUnitsFormatLength = 0;
        mUnitsFormatSlope = 0;
        mUnitsFormatSpeed = 0;
        mUnitsFormatTemperature = 0;
        mUnitsFormatWeight = 0;
    }

    @Override
    protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
        mUnitsFormatAltitude = dr.readInt();
        mUnitsFormatAngle = dr.readInt();
        mUnitsFormatArea = dr.readInt();
        mUnitsFormatEnergy = dr.readInt();
        mUnitsFormatLength = dr.readInt();
        mUnitsFormatSlope = dr.readInt();
        mUnitsFormatSpeed = dr.readInt();
        mUnitsFormatTemperature = dr.readInt();
        mUnitsFormatWeight = dr.readInt();
    }

    @Override
    protected void writeObject(DataWriterBigEndian dw) throws IOException {
        dw.writeInt(mUnitsFormatAltitude);
        dw.writeInt(mUnitsFormatAngle);
        dw.writeInt(mUnitsFormatArea);
        dw.writeInt(mUnitsFormatEnergy);
        dw.writeInt(mUnitsFormatLength);
        dw.writeInt(mUnitsFormatSlope);
        dw.writeInt(mUnitsFormatSpeed);
        dw.writeInt(mUnitsFormatTemperature);
        dw.writeInt(mUnitsFormatWeight);
    }
}
