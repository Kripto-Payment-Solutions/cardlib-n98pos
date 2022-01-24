package com.newpos.mposlib.sdk;

public class InputInfoEntity {
    private int inputType;
    private int timeout;
    private String pan;

    public InputInfoEntity() {
    }

    public int getInputType() {
        return this.inputType;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPan() {
        return this.pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }
}
