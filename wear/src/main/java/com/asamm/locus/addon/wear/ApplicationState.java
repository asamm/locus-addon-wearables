package com.asamm.locus.addon.wear;

import com.assam.locus.addon.wear.common.communication.containers.HandShakeValue;

/**
 * Created by Milan Cejnar on 09.11.2017.
 * Asamm Software, s.r.o.
 */

public class ApplicationState {
    private boolean connected = false;
    private HandShakeValue handShakeValue = null;
    ApplicationState(){};

    public boolean isConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    public HandShakeValue getHandShakeValue() {
        return handShakeValue;
    }

    void setHandShakeValue(HandShakeValue handShakeValue) {
        this.handShakeValue = handShakeValue;
    }

    public boolean isHandShake() {
        return handShakeValue != null;
    }
}
