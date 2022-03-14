package com.newpos.mposlib.model;

public class TerminalInfo {
    private String strAppVersion = null;
    private String strHardwareVer = null;
    private String strChipCode = "0000000000000000";
    private String strCompanyId = null;
    private String strBranchnum = null;
    private String strProductId = null;
    private String strSNCode = null;
    private String strTraceNum = null;
    private String strTerminalNum = "";

    public String getTraceNum() {
        return strTraceNum;
    }

    public void setStrTraceNum(String strtraceid) {
        this.strTraceNum = strtraceid;
    }

    public String getStrAppVersion() {
        return this.strAppVersion;
    }
    public String getStrHardwareVer(){
        return this.strHardwareVer;
    }

    public String getStrChipCode() {
        return this.strChipCode;
    }

    public String getStrCompanyId() {
        return this.strCompanyId;
    }

    public String getstrBranchnum() {
        return this.strBranchnum;
    }

    public String getStrProductId() {
        return this.strProductId;
    }

    public String getStrSNCode() {
        return this.strSNCode;
    }

    public String getStrTerminalNum() {
        return this.strTerminalNum;
    }

    public void setStrAppVersion(String paramString) {
        this.strAppVersion = paramString;
    }

    public void setStrHardwareVer(String paramString) {
        this.strHardwareVer = paramString;
    }

    public void setStrChipCode(String paramString) {
        this.strChipCode = paramString;
    }

    public void setStrCompanyId(String paramString) {
        this.strCompanyId = paramString;
    }

    public void setstrBranchnum(String paramString) {
        this.strBranchnum = paramString;
    }

    public void setStrProductId(String paramString) {
        this.strProductId = paramString;
    }

    public void setStrSNCode(String paramString) {
        this.strSNCode = paramString;
    }

    public void setStrTerminalNum(String paramString) {
        this.strTerminalNum = paramString;
    }

    public String toString()
    {
        return "TerminalInfo{strTraceNum='" + this.strTraceNum + '\''  + ", strSNCode='" + this.strSNCode + '\'' + ", strProductId='" + this.strProductId + '\'' + ", strAppVersion='" + this.strAppVersion + '\'' + ", strHardwareVer='" + this.strHardwareVer + '\'' +", strPNCode='" + this.strBranchnum + '\'' + ", strCompanyId='" + this.strCompanyId + '\'' + ", strChipCode='" + this.strChipCode + '\'' + ", strTerminalNum='" + this.strTerminalNum + '\'' + '}';
    }
/*    public String toString()
    {
        return "TerminalInfo{strSNCode='" + this.strSNCode + '\'' + ", strPNCode='" + this.strPNCode + '\'' + ", strProductId='" + this.strProductId + '\'' + ", strAppVersion='" + this.strAppVersion + '\'' + ", strCompanyId='" + this.strCompanyId + '\'' + ", strChipCode='" + this.strChipCode + '\'' + ", strTerminalNum='" + this.strTerminalNum + '\'' + '}';
    }*/
}