package com.newpos.mposlib.model;

public class TerminalInfo
{
    private String strAppVersion = null;
    private String strChipCode = "0000000000000000";
    private String strCompanyId = null;
    private String strPNCode = null;
    private String strProductId = null;
    private String strSNCode = null;
    private String strTUSN = null;
    private String strTerminalNum = "";

    public String getStrTUSN() {
        return strTUSN;
    }

    public void setStrTUSN(String strTUSN) {
        this.strTUSN = strTUSN;
    }
    public String getStrAppVersion()
    {
        return this.strAppVersion;
    }

    public String getStrChipCode()
    {
        return this.strChipCode;
    }

    public String getStrCompanyId()
    {
        return this.strCompanyId;
    }

    public String getStrPNCode()
    {
        return this.strPNCode;
    }

    public String getStrProductId()
    {
        return this.strProductId;
    }

    public String getStrSNCode()
    {
        return this.strSNCode;
    }

    public String getStrTerminalNum()
    {
        return this.strTerminalNum;
    }

    public void setStrAppVersion(String paramString)
    {
        this.strAppVersion = paramString;
    }

    public void setStrChipCode(String paramString)
    {
        this.strChipCode = paramString;
    }

    public void setStrCompanyId(String paramString)
    {
        this.strCompanyId = paramString;
    }

    public void setStrPNCode(String paramString)
    {
        this.strPNCode = paramString;
    }

    public void setStrProductId(String paramString)
    {
        this.strProductId = paramString;
    }

    public void setStrSNCode(String paramString)
    {
        this.strSNCode = paramString;
    }

    public void setStrTerminalNum(String paramString)
    {
        this.strTerminalNum = paramString;
    }

    public String toString()
    {
        return "TerminalInfo{strSNCode='" + this.strSNCode + '\'' + ", strPNCode='" + this.strPNCode + '\'' + ", strProductId='" + this.strProductId + '\'' + ", strAppVersion='" + this.strAppVersion + '\'' + ", strCompanyId='" + this.strCompanyId + '\'' + ", strChipCode='" + this.strChipCode + '\'' + ", strTerminalNum='" + this.strTerminalNum + '\'' + '}';
    }
}