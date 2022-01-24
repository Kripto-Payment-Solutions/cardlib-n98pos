package com.newpos.mposlib.model;

public class InputPinResponse
{
    private String KSN;
    private String Random = null;
    private String encryptedData = null;
    private byte keyID;
    private byte pinLen;

    public String getEncryptedData()
    {
        return this.encryptedData;
    }

    public String getKSN()
    {
        return this.KSN;
    }

    public byte getKeyID()
    {
        return this.keyID;
    }

    public byte getPinLen()
    {
        return this.pinLen;
    }

    public String getRandom()
    {
        return this.Random;
    }

    public void setEncryptedData(String paramString)
    {
        this.encryptedData = paramString;
    }

    public void setKSN(String paramString)
    {
        this.KSN = paramString;
    }

    public void setKeyID(byte paramByte)
    {
        this.keyID = paramByte;
    }

    public void setPinLen(byte paramByte)
    {
        this.pinLen = paramByte;
    }

    public void setRandom(String paramString)
    {
        this.Random = paramString;
    }

    public String toString()
    {
        return "InputPinResponse{keyID=" + this.keyID + ", pinLen=" + this.pinLen + ", encryptedData='" + this.encryptedData + '\'' + ", KSN='" + this.KSN + '\'' + ", Random='" + this.Random + '\'' + '}';
    }
}