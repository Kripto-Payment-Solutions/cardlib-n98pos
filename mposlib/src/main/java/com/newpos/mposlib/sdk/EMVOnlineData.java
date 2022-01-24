package com.newpos.mposlib.sdk;

public class EMVOnlineData {
    private String responseCode;
    private String onlineData;

    public EMVOnlineData() {
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getOnlineData() {
        return this.onlineData;
    }

    public void setOnlineData(String onlineData) {
        this.onlineData = onlineData;
    }
}
