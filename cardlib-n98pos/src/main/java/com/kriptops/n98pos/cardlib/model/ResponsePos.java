package com.kriptops.n98pos.cardlib.model;

public class ResponsePos<T> {
    private String code;
    private String nameEvent;
    private String message;
    private String encryTransportKey;
    private String cardNum;
    private String inputInfo;
    private String encryMacData;
    private String transactionInfo;

    private T objResp;

    public ResponsePos(){
        this.code = "";
        this.nameEvent = "";
        this.encryTransportKey = "";
        this.message = "";
        this.inputInfo = "";
        this.cardNum = "";
        this.encryMacData = "";
        this.transactionInfo = "";
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public T getObjResp() {
        return objResp;
    }

    public void setObjResp(T objResp) {
        this.objResp = objResp;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public String getInputInfo() {
        return inputInfo;
    }

    public void setInputInfo(String inputInfo) {
        this.inputInfo = inputInfo;
    }

    public String getEncryMacData() {
        return encryMacData;
    }

    public void setEncryMacData(String encryMacData) {
        this.encryMacData = encryMacData;
    }

    public String getTransactionInfo() {
        return transactionInfo;
    }

    public void setTransactionInfo(String transactionInfo) {
        this.transactionInfo = transactionInfo;
    }
}
