package com.newpos.mposlib.sdk;

public class CardReadEntity {

    private boolean supportFallback;
    private int timeout;
    private String amount;
    private int tradeType;
    private int readCardType;
    private String terminalCoutryCode = "0604"; // default peru
    private String currency = "0604"; // default soles

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
        if (this.amount == null) {
            return "000000000000";
        }
        if (amount.contains(".")) {
            amount = amount.replace(".", "");
        }
        if (amount.length() < 12) {
            amount = "000000000000" + amount;
            amount = amount.substring(amount.length() - 12);
        }
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

    public int getReadCardType() {
        return this.readCardType;
    }

    public void setReadCardType(int type) {
        this.readCardType = type;
    }

    public String getTerminalCoutryCode() {
        return terminalCoutryCode;
    }

    public void setTerminalCoutryCode(String terminalCoutryCode) {
        this.terminalCoutryCode = terminalCoutryCode;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
