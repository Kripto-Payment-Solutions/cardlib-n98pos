package com.kriptops.wizarpos.cardlib.bridge;

import com.cloudpos.POSTerminal;
import com.cloudpos.msr.MSRDevice;

public class Msr extends CloseableDeviceWrapper<MSRDevice> {

    public Msr(POSTerminal posTerminal) {
        super((MSRDevice) posTerminal.getDevice("cloudpos.device.msr"));
    }

}
