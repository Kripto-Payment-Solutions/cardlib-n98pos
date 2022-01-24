package com.newpos.mposlib.model;

public class SwipeCardResponse {

    private String KSN;
    private String carSeq = "";
    private String cardHolder;
    private String cardMAC;
    private CardType cardType = CardType.TRACK;
    private String encryptedCardPan;
    private String encryptedIcParams = "";
    private String encryptedTrack2Data= "";
    private String encryptedTrack3Data = "";
    private String encryptedTrack1Data = "";
    private String executeResult = "";
    private String expiryDate = "";
    private String extras;
    private String icParams = "";
    private String oneTrack = "";
    private String pan = "";
    private String panHash = "";
    private String random;
    private String randomP;
    private String randomT;
    private String serviceCode;
    private String threeTrack = "";
    private int track1Length = 0;
    private int track2Length = 0;
    private int track3Length = 0;
    private String twoTrack = "";
    private String unencryptedTrack2Data = "";
    private String track2_servicecode;
    public String getCarSeq()
    {
        return this.carSeq;
    }

    public String getCardHolder()
    {
        return this.cardHolder;
    }

    public String getCardMAC()
    {
        return this.cardMAC;
    }

    public CardType getCardType()
    {
        return this.cardType;
    }

    public String getEncryptedCardPan()
    {
        return this.encryptedCardPan;
    }

    public String getEncryptedIcParams()
    {
        return this.encryptedIcParams;
    }

    public String getEncryptedTrack2Data() {
        return this.encryptedTrack2Data;
    }

    public String getExecuteResult()
    {
        return this.executeResult;
    }

    public String getExpiryDate()
    {
        return this.expiryDate;
    }

    public String getExtras()
    {
        return this.extras;
    }

    public String getIcParams()
    {
        return this.icParams;
    }

    public String getKSN()
    {
        return this.KSN;
    }

    public String getOneTrack()
    {
        return this.oneTrack;
    }

    public String getPan()
    {
        return this.pan;
    }

    public String getPanHash()
    {
        return this.panHash;
    }

    public String getRandom()
    {
        return this.random;
    }

    public String getRandomP()
    {
        return this.randomP;
    }

    public String getRandomT()
    {
        return this.randomT;
    }

    public String getServiceCode()
    {
        return this.serviceCode;
    }

    public String getThreeTrack()
    {
        return this.threeTrack;
    }

    public int getTrack1Length()
    {
        return this.track1Length;
    }

    public int getTrack2Length()
    {
        return this.track2Length;
    }

    public int getTrack3Length()
    {
        return this.track3Length;
    }

    public String getTwoTrack()
    {
        return this.twoTrack;
    }

    public String getUnencryptedTrack2Data()
    {
        return this.unencryptedTrack2Data;
    }

    public void setCarSeq(String paramString)
    {
        this.carSeq = paramString;
    }

    public void setCardHolder(String paramString)
    {
        this.cardHolder = paramString;
    }

    public void setCardMAC(String paramString)
    {
        this.cardMAC = paramString;
    }

    public void setCardType(CardType paramCardType)
    {
        this.cardType = paramCardType;
    }

    public void setEncryptedCardPan(String paramString)
    {
        this.encryptedCardPan = paramString;
    }

    public void setEncryptedIcParams(String paramString)
    {
        this.encryptedIcParams = paramString;
    }

    public void setEncryptedTrack2Data(String paramString)
    {
        this.encryptedTrack2Data = paramString;
    }

    public void setExecuteResult(String paramString)
    {
        this.executeResult = paramString;
    }

    public void setExpiryDate(String paramString)
    {
        this.expiryDate = paramString;
    }

    public void setExtras(String paramString)
    {
        this.extras = paramString;
    }

    public void setIcParams(String paramString)
    {
        this.icParams = paramString;
    }

    public void setKSN(String paramString)
    {
        this.KSN = paramString;
    }

    public void setOneTrack(String paramString)
    {
        this.oneTrack = paramString;
    }

    public void setPan(String paramString)
    {
        this.pan = paramString;
    }

    public void setPanHash(String paramString)
    {
        this.panHash = paramString;
    }

    public void setRandom(String paramString)
    {
        this.random = paramString;
    }

    public void setRandomP(String paramString)
    {
        this.randomP = paramString;
    }

    public void setRandomT(String paramString)
    {
        this.randomT = paramString;
    }

    public void setServiceCode(String paramString)
    {
        this.serviceCode = paramString;
    }

    public void setThreeTrack(String paramString)
    {
        this.threeTrack = paramString;
    }

    public void setTrack1Length(int paramInt)
    {
        this.track1Length = paramInt;
    }

    public void setTrack2Length(int paramInt)
    {
        this.track2Length = paramInt;
    }

    public void setTrack3Length(int paramInt)
    {
        this.track3Length = paramInt;
    }

    public void setTwoTrack(String paramString)
    {
        this.twoTrack = paramString;
    }

    public void setUnencryptedTrack2Data(String paramString) {
        this.unencryptedTrack2Data = paramString;
    }
    public String getEncryptedTrack3Data() {
        return encryptedTrack3Data;
    }

    public void setEncryptedTrack3Data(String encryptedTrack3Data) {
        this.encryptedTrack3Data = encryptedTrack3Data;
    }

    public String getEncryptedTrack1Data() {
        return encryptedTrack1Data;
    }

    public void setEncryptedTrack1Data(String encryptedTrack1Data) {
        this.encryptedTrack1Data = encryptedTrack1Data;
    }
    public String getTrack2Servicecode(){
        return track2_servicecode;
    }
    public void setTrack2Servicecode(String code){
        track2_servicecode=code;
    }


    public String toString()
    {
        return "SwipeCardResponse{pan='" + this.pan + '\'' + ", panHash='" + this.panHash + '\'' + ", oneTrack='" + this.oneTrack + '\'' + ", twoTrack='" + this.twoTrack + '\'' + ", threeTrack='" + this.threeTrack + '\'' + ", track1Length=" + this.track1Length + ", track2Length=" + this.track2Length + ", track3Length=" + this.track3Length + ", expiryDate='" + this.expiryDate + '\'' + ", KSN='" + this.KSN + '\'' + ", extras='" + this.extras + '\'' + ", icParams='" + this.icParams + '\'' + ", carSeq='" + this.carSeq + '\'' + ", cardType=" + this.cardType + ", executeResult='" + this.executeResult + '\'' + ", unencryptedTrack2Data='" + this.unencryptedTrack2Data + '\'' + ", encryptedIcParams='" + this.encryptedIcParams + '\'' + ", cardMAC='" + this.cardMAC + '\'' + ", random='" + this.random + '\'' + ", cardHolder='" + this.cardHolder + '\'' + ", encryptedCardPan='" + this.encryptedCardPan + '\'' + '}';
    }

    public static enum CardType {

        TRACK,
        IC,
        RF;
    }
}
