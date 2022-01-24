package com.newpos.mposlib.sdk;

public class CardInfoEntity {

    private int cardType;
    private String track1;
    private String track2;
    private String track3;
    private String cardNumber;
    private String encryptPin;
    private String expDate;
    private String csn;
    private String ic55Data;
    private String tusn;
    private String encryptedSN;
    private String deviceType;
    private String ksn;

    public CardInfoEntity() {
    }

    public int getCardType() {
        return this.cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public String getTrack1() {
        return this.track1;
    }

    public void setTrack1(String track1) {
        this.track1 = track1;
    }

    public String getTrack2() {
        return this.track2;
    }

    public void setTrack2(String track2) {
        this.track2 = track2;
    }

    public String getTrack3() {
        return this.track3;
    }

    public void setTrack3(String track3) {
        this.track3 = track3;
    }

    public String getCardNumber() {
        return this.cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpDate() {
        return this.expDate;
    }

    public void setExpDate(String expDate) {
        this.expDate = expDate;
    }

    public String getCsn() {
        return this.csn;
    }

    public void setCsn(String csn) {
        this.csn = csn;
    }

    public String getEncryptPin() {
        return this.encryptPin;
    }

    public void setEncryptPin(String encryptPin) {
        this.encryptPin = encryptPin;
    }

    public String getIc55Data() {
        return this.ic55Data;
    }

    public void setIc55Data(String ic55Data) {
        this.ic55Data = ic55Data;
    }

    public String getTusn() {
        return this.tusn;
    }

    public void setTusn(String tusn) {
        this.tusn = tusn;
    }

    public String getEncryptedSN() {
        return this.encryptedSN;
    }

    public void setEncryptedSN(String encryptedSN) {
        this.encryptedSN = encryptedSN;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getKsn() {
        return this.ksn;
    }

    public void setKsn(String ksn) {
        this.ksn = ksn;
    }
}
