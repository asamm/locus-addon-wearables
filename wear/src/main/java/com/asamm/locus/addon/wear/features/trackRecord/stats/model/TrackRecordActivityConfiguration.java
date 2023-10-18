package com.asamm.locus.addon.wear.features.trackRecord.stats.model;

import android.content.Context;

import com.asamm.locus.addon.wear.features.settings.PreferencesEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import locus.api.objects.Storable;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import com.asamm.logger.Logger;

/**
 * Class for representation of Track recording activity statistics layout configuration
 * Created by Milan Cejnar on 24.11.2017.
 * Asamm Software, s.r.o.
 */
public class TrackRecordActivityConfiguration {
    private final static String TAG = "TrackRecordActivityConfiguration";
    private final CfgContainer config;

    private static TrackRecordActivityConfiguration INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    private TrackRecordActivityConfiguration(Context ctx) {
        CfgContainer tmp = PreferencesEx.getStatsScreenConfiguration();
        config = tmp != null ? tmp : createDefaultConfiguration();
    }

    public TrackStatTypeEnum getStatConfigAtPosition(int screenIdx, int cellIdx) {
        if (screenIdx < 0 || screenIdx >= config.getScreenCount()) {
            return TrackStatTypeEnum.BLANK;
        } else {
            return config.screens.get(screenIdx).getCellTypeAtIdx(cellIdx);
        }
    }

    public void setStatConfigAtPosition(Context ctx, int screenIdx, int cellIdx, TrackStatTypeEnum newStat) {
        if (screenIdx >= 0 && screenIdx < config.getScreenCount()
                && cellIdx >= 0 && cellIdx < config.screens.get(screenIdx).getScreenType().getPositionsCount()) {
            config.screens.get(screenIdx).setCellTypeAtIdx(cellIdx, newStat);
            PreferencesEx.persistStatsScreenConfiguration(this);
        } else {
            Logger.e(TAG, "invalid TrackStat cell index (" + screenIdx + ", " + cellIdx + ")");
        }
    }

    public TrackRecScreenConfigDto getScreenConfigAtPosition(int screenIdx) {
        if (screenIdx < 0 || screenIdx >= config.getScreenCount()) {
            return TrackRecScreenConfigDto.createEmptyScreenConfig();
        } else {
            return config.screens.get(screenIdx);
        }
    }

    public static TrackRecordActivityConfiguration getConfiguration(Context ctx) {
        if (INSTANCE == null) {
            synchronized (INSTANCE_LOCK) {
                if (INSTANCE == null) INSTANCE = new TrackRecordActivityConfiguration(ctx);
            }
        }
        return INSTANCE;
    }

    public int getScreenCount() {
        return config.getScreenCount();
    }

    public byte[] getAsBytes() {
        return config.getAsBytes();
    }

    private CfgContainer createDefaultConfiguration() {
        CfgContainer cont = new CfgContainer();
        // Init statistics for main screen at the 0th index
        List<TrackRecCellConfigDto> mainScreenConfig = new ArrayList<>(2);
        mainScreenConfig.add(new TrackRecCellConfigDto((byte) 0, TrackStatTypeEnum.TRACK_TIME));
        mainScreenConfig.add(new TrackRecCellConfigDto((byte) 1, TrackStatTypeEnum.TOTAL_LENGTH_MOVE));
        cont.screens.add(new TrackRecScreenConfigDto(TrackStatsViewScreenType.STAT_SCREEN_R2_C1, mainScreenConfig));

        // Init second screen with R2C1 configuration
        List<TrackRecCellConfigDto> secondScreenConfig = new ArrayList<>(2);
        secondScreenConfig.add(new TrackRecCellConfigDto((byte) 0, TrackStatTypeEnum.SPEED_AVG_MOVE));
        secondScreenConfig.add(new TrackRecCellConfigDto((byte) 1, TrackStatTypeEnum.BLANK));
        cont.screens.add(new TrackRecScreenConfigDto(TrackStatsViewScreenType.STAT_SCREEN_R2_C1, secondScreenConfig));

        // Init third screen with R2C2 configuration
        List<TrackRecCellConfigDto> thirdScreenConfig = new ArrayList<>(4);
        thirdScreenConfig.add(new TrackRecCellConfigDto((byte) 0, TrackStatTypeEnum.ELEVATION_DOWNHILL));
        thirdScreenConfig.add(new TrackRecCellConfigDto((byte) 1, TrackStatTypeEnum.ELEVATION_UPHILL));
        thirdScreenConfig.add(new TrackRecCellConfigDto((byte) 2, TrackStatTypeEnum.PACE));
        thirdScreenConfig.add(new TrackRecCellConfigDto((byte) 3, TrackStatTypeEnum.BLANK));
        cont.screens.add(new TrackRecScreenConfigDto(TrackStatsViewScreenType.STAT_SCREEN_R2_C2, thirdScreenConfig));

        // Init fourth screen with R2C2 configuration
        List<TrackRecCellConfigDto> fourthScreenConfig = new ArrayList<>(4);
        fourthScreenConfig.add(new TrackRecCellConfigDto((byte) 0, TrackStatTypeEnum.ENERGY));
        fourthScreenConfig.add(new TrackRecCellConfigDto((byte) 1, TrackStatTypeEnum.ALTITUDE));
        fourthScreenConfig.add(new TrackRecCellConfigDto((byte) 2, TrackStatTypeEnum.HEART_RATE));
        fourthScreenConfig.add(new TrackRecCellConfigDto((byte) 3, TrackStatTypeEnum.BLANK));
        cont.screens.add(new TrackRecScreenConfigDto(TrackStatsViewScreenType.STAT_SCREEN_R2_C2, fourthScreenConfig));

        return cont;
    }

    /**
     * Storable container holding statistics configuration for all the screens
     */
    @SuppressWarnings("WeakerAccess")
    public static class CfgContainer extends Storable {

        private ArrayList<TrackRecScreenConfigDto> screens = new ArrayList<>(4);

        public CfgContainer() {
            super();
        }

        public CfgContainer(byte[] data) throws IOException {
            super();
            read(data);
        }

        private int getScreenCount() {
            return screens == null ? 0 : screens.size();
        }

        @Override
        protected int getVersion() {
            return 0;
        }

        @Override
        protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
            int screenCount = dr.readBytes(1)[0];
            for (int i = 0; i < screenCount; i++) {
                try {
                    screens.add(dr.readStorable(TrackRecScreenConfigDto.class));
                } catch (InstantiationException | IllegalAccessException e) {
                    screens.add(new TrackRecScreenConfigDto(TrackStatsViewScreenType.STAT_SCREEN_BLANK, Collections.emptyList()));
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void writeObject(DataWriterBigEndian dw) throws IOException {
            int screenCount = getScreenCount();
            dw.write((byte) screenCount);
            for (int i = 0; i < screenCount; i++) {
                dw.writeStorable(screens.get(i));
            }
        }
    }
}
