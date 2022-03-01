package com.kriptops.n98pos.cardlib.model;

public class ResponsePos {
    private String code;
    private String nameEvent;
    private String encryTransportKey;

    public ResponsePos(){
        code = "";
        nameEvent = "";
        encryTransportKey = "";
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameEvent() {
        return nameEvent;
    }

    public void setNameEvent(String nameEvent) {
        this.nameEvent = nameEvent;
    }

    public String getEncryTransportKey() {
        return encryTransportKey;
    }

    public void setEncryTransportKey(String encryTransportKey) {
        this.encryTransportKey = encryTransportKey;
    }
}
