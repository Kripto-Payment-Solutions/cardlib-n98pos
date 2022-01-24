package com.kriptops.wizarpos.cardlib.bridge;

import android.content.Context;

import com.cloudpos.POSTerminal;
import com.cloudpos.TerminalSpec;
import com.cloudpos.msr.MSRDevice;
import com.cloudpos.pinpad.PINPadDevice;
import com.cloudpos.printer.PrinterDevice;

public class Terminal {

    private POSTerminal posTerminal;
    private Printer printer;
    private Pinpad pinpad;
    private Msr msr;

    public Terminal() {
    }

    public void init(Context context) {
        this.posTerminal = POSTerminal.getInstance(context);
        printer = new Printer(this.posTerminal);
        pinpad = new Pinpad(this.posTerminal);
        msr = new Msr(this.posTerminal);
    }

    public String getSerialNumber() {
        return this.posTerminal.getTerminalSpec().getSerialNumber();
    }

    public String getHardwareVersion() {
        return this.posTerminal.getTerminalSpec().getHardwareVersion();
    }

    public String getManufacturer(){
        return this.posTerminal.getTerminalSpec().getManufacturer();
    }

    public String getModel(){
        return this.posTerminal.getTerminalSpec().getModel();
    }

    public POSTerminal getPosTerminal() {
        return posTerminal;
    }

    public void setPosTerminal(POSTerminal posTerminal) {
        this.posTerminal = posTerminal;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public Pinpad getPinpad() {
        return pinpad;
    }

    public void setPinpad(Pinpad pinpad) {
        this.pinpad = pinpad;
    }

    public Msr getMsr() {
        return msr;
    }

    public void setMsr(Msr msr) {
        this.msr = msr;
    }
}
