package com.kriptops.n98pos.cardlib.bridge;

import com.cloudpos.POSTerminal;
import com.cloudpos.printer.PrinterDevice;

public class Printer extends CloseableDeviceWrapper<PrinterDevice> {

    protected Printer(POSTerminal posTerminal) {
        super((PrinterDevice) posTerminal.getDevice("cloudpos.device.printer"));
    }
}
