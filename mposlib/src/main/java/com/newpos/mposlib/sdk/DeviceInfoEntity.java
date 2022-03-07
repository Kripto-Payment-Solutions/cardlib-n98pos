package com.newpos.mposlib.sdk;

import android.text.TextUtils;

public class DeviceInfoEntity {
    private int deviceType;
    private String devicetypestr;
    private String ksn;
    private float currentElePer;
    private String firmwareVer;
    private String SerialNumber;

    public DeviceInfoEntity() {
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public String getKsn() {
        return TextUtils.isEmpty(this.ksn) ? "" : this.ksn.trim();
    }

    public void setKsn(String ksn) {
        this.ksn = ksn;
    }

    public float getCurrentElePer() {
        return this.currentElePer;
    }

    public void setCurrentElePer(float currentElePer) {
        this.currentElePer = currentElePer;
    }

    public String getFirmwareVer() {
        return this.firmwareVer;
    }

    public void setFirmwareVer(String firmwareVer) {
        this.firmwareVer = firmwareVer;
    }

    public String getDeviceTypeStr() {
        return this.devicetypestr;
    }

    public void setDeviceTypeStr(String temp) {
        this.devicetypestr = temp;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        SerialNumber = serialNumber;
    }
}
