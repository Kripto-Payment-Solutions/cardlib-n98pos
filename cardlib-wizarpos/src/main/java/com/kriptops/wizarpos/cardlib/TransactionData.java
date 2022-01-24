package com.kriptops.wizarpos.cardlib;

public class TransactionData {

    public byte type;
    public String captureType;
    public String selectedApp;

    //clear data
    public String maskedPan;
    public String bin;

    //protected data
    public String pinblock;
    public String panSequenceNumber;
    public String track2;
    public String track2Clear;
    public String emvData;
    public String emvDataClear;
    public String aid;
    public String expiry;
    public String ecBalance;
    public boolean online = false;

    //data auth
    public String tsi;
    public String tsiFinal;
    public String tvr;
    public String tvrFinal;

    @Override
    public String toString() {
        return "TransactionData{" +
                "\ttype=" + type + "\n" +
                "\tcaptureType='" + captureType + '\'' + "\n" +
                "\tselectedApp='" + selectedApp + '\'' + "\n" +
                "\tmaskedPan='" + maskedPan + '\'' + "\n" +
                "\tbin='" + bin + '\'' + "\n" +
                "\tpinblock='" + pinblock + '\'' + "\n" +
                "\tpanSequenceNumber='" + panSequenceNumber + '\'' + "\n" +
                "\ttrack2='" + track2 + '\'' + "\n" +
                "\ttrack2Clear='" + track2Clear + '\'' + "\n" +
                "\temvData='" + emvData + '\'' + "\n" +
                "\temvDataClear='" + emvDataClear + '\'' + "\n" +
                "\taid='" + aid + '\'' + "\n" +
                "\texpiry='" + expiry + '\'' + "\n" +
                "\tecBalance='" + ecBalance + '\'' + "\n" +
                "\tonline=" + online + "\n" +
                "\ttsi='" + tsi + '\'' + "\n" +
                "\ttsiFinal='" + tsiFinal + '\'' + "\n" +
                "\ttvr='" + tvr + '\'' + "\n" +
                "\ttvrFinal='" + tvrFinal + '\'' + "\n" +
                '}';
    }
}
