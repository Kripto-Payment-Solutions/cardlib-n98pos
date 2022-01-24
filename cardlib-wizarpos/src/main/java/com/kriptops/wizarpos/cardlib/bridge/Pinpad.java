package com.kriptops.wizarpos.cardlib.bridge;

import com.cloudpos.POSTerminal;
import com.cloudpos.pinpad.extend.PINPadExtendDevice;

public class Pinpad extends CloseableDeviceWrapper<PINPadExtendDevice> {

    public Pinpad(POSTerminal posTerminal) {
        super((PINPadExtendDevice) posTerminal.getDevice("cloudpos.device.pinpad"));
    }

}
