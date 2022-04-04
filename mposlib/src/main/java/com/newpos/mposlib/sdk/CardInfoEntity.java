package com.newpos.mposlib.sdk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

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

    private String panSequenceNumber; //cardSeq;
    private String maskedPan; //cardSeq;
    private String emv;
    private String bin;
    private String captureType;
    private String aid;
    private String encryptTrack2;
    private Map<String, String> dataMap = new HashMap<>();

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

    public String getPanSequenceNumber() {
        return panSequenceNumber;
    }

    public void setPanSequenceNumber(String panSequenceNumber) {
        this.panSequenceNumber = panSequenceNumber;
    }

    public String getMaskedPan() {
        return maskedPan;
    }

    public void setMaskedPan(String maskedPan) {
        this.maskedPan = maskedPan;
    }

    public String getEmv() {
        return emv;
    }

    public void setEmv(String emv) {
        this.emv = emv;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getCaptureType() {
        return captureType;
    }

    public void setCaptureType(String captureType) {
        this.captureType = captureType;
    }

    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getEncryptTrack2() {
        return encryptTrack2;
    }

    public void setEncryptTrack2(String encryptTrack2) {
        this.encryptTrack2 = encryptTrack2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field field : this.getClass().getDeclaredFields()) {
            String item;
            if(! field.getType().getSimpleName().equals("ArrayList")) {
                try {
                    Object value = field.get(this);
                    item = String.format("%s %s %s: %s%n", Modifier.toString(field.getModifiers()), field.getType().getSimpleName(), field.getName(), String.valueOf(value));
                    sb.append(item);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                item = String.format("%s %s %s: ArrayList<>%n", Modifier.toString(field.getModifiers()), field.getType().getSimpleName(), field.getName());
                sb.append(item);
            }
        }
        return sb.toString();
    }

    public void setDataMap(Map<String, String> dataMap) {
        this.dataMap = dataMap;
    }

    public Map<String, String> getDataMap() {
        return dataMap;
    }
}