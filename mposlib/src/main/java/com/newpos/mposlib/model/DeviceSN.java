package com.newpos.mposlib.model;

public class DeviceSN {

    private String tusn;
    private String encryptTusn;
    private String deviceType;
    private String random;

    public String getTusn() {
        return tusn;
    }

    public void setTusn(String tusn) {
        this.tusn = tusn;
    }

    public String getEncryptTusn() {
        return encryptTusn;
    }

    public void setEncryptTusn(String encryptTusn) {
        this.encryptTusn = encryptTusn;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

}
