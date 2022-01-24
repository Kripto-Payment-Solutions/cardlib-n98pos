package com.kriptops.wizarpos.cardlib;

import android.util.Log;

import com.cloudpos.DeviceException;
import com.cloudpos.OperationListener;
import com.cloudpos.TimeConstants;
import com.cloudpos.msr.MSRDevice;
import com.kriptops.wizarpos.cardlib.bridge.CloseableDeviceWrapper;


public class Msr extends CloseableDeviceWrapper<MSRDevice> {

    public Msr(MSRDevice device) {
        super(device);
    }

    private OperationListener listener;

    public void setListener(OperationListener listener) {
        this.listener = listener;
    }

    public boolean waitForTrack() {
        try {
            this.device.listenForSwipe(listener, TimeConstants.FOREVER);
            return true;
        } catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "no puede iniciar el msr");
            return false;
        }
    }

}
