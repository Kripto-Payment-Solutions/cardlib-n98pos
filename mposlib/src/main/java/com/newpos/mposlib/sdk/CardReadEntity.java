package com.newpos.mposlib.sdk;

public class CardReadEntity {

    private boolean supportFallback;
    private int timeout;
    private String amount;
    private int tradeType;

    public CardReadEntity() {
    }

    public boolean isSupportFallback() {
        return this.supportFallback;
    }

    public void setSupportFallback(boolean supportFallback) {
        this.supportFallback = supportFallback;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public int getTradeType() {
        return this.tradeType;
    }

    public void setTradeType(int tradeType) {
        this.tradeType = tradeType;
    }
}
