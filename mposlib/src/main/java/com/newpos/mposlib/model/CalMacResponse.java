package com.newpos.mposlib.model;

public class CalMacResponse {
    private String KSN = null;
    private String MAC = null;
    private String Random = null;

    public String getKSN()
    {
        return this.KSN;
    }

    public String getMAC()
    {
        return this.MAC;
    }

    public String getRandom()
    {
        return this.Random;
    }

    public void setKSN(String paramString)
    {
        this.KSN = paramString;
    }

    public void setMAC(String paramString)
    {
        this.MAC = paramString;
    }

    public void setRandom(String paramString)
    {
        this.Random = paramString;
    }

    public String toString() {
        return "CalMacResponse{MAC='" + this.MAC + '\'' + ", KSN='" + this.KSN + '\'' + ", Random='" + this.Random + '\'' + '}';
    }
}
