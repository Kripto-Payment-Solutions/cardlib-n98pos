package com.kriptops.wizarpos.cardlib.kernel;

import android.text.TextUtils;

import com.kriptops.wizarpos.cardlib.tools.NumberUtil;
import com.kriptops.wizarpos.cardlib.tools.StringUtil;
import com.kriptops.wizarpos.cardlib.tools.Util;

public class AID {
    private String aid;                      // Application Identifier
    private String appLabel;                 // Application Label
    private String appPreferredName;         // Application Preferred Name
    private byte appPriority;                // Application Priority
    private int termFloorLimit;              // Terminal Floor Limit
    private String termActionCodeDefault;    // Terminal Action Code - Default
    private String termActionCodeDenial;     // Terminal Action Code - Denial
    private String termActionCodeOnline;     // Terminal Action Code - Online
    private byte targetPercentage;           // Target Percentage
    private int thresholdValue;              // threshold Value
    private byte maxTargetPercentage;        // Maximum Target Percentage
    private String acquirerId;               // Acquirer Identifier
    private String mcc;                      // Merchant Category Code
    private String mid;                      // Merchant Identifier
    private String appVersionNumber;         // Application Version Number
    private byte posEntryMode;               // Point-of-Service(POS) Entry Mode
    private String transReferCurrencyCode;   // Transaction Reference Currency Code
    private byte transReferCurrencyExponent; // Transaction Reference Currency Exponent
    private String defaultDDOL;              // Default Dynamic Data Authentication Data Object List(DDOL)
    private String defaultTDOL;              // Default Transaction Certificate Data Object List(TDOL)

    // supportOnlinePin[0] = 0 means the Application unsupported online PIN,
    // any other value means the Application supported online PIN
    private byte supportOnlinePin;
    private byte needCompleteMatching;
    private String termRiskManageData;
    private long contactlessLimit;
    private long cvmLimit;
    private long contactlessFloorLimit;

    // C2 - Mastercard Contactless
    private byte kernelConfig;
    private long ctlOnDeviceCVM;
    private long ctlNoOnDeviceCVM;
    private byte cvmCapCVMRequired;
    private byte cvmCapNoCVMRequired;
    private byte mscvmCapCVMRequired;
    private byte mscvmCapNoCVMRequired;

    // Visa peru
    private String auc;

    private byte contactlessKernelID;

    public AID() {
        init();
    }

    public void init() {
        aid = "";
        appLabel = "";
        appPreferredName = "";
        appPriority = 0;
        termFloorLimit = 0;
        termActionCodeDefault = "";
        termActionCodeDenial = "";
        termActionCodeOnline = "";
        targetPercentage = 0;
        thresholdValue = 0;
        maxTargetPercentage = 0;
        acquirerId = "";
        mcc = "";
        mid = "";
        appVersionNumber = "";
        posEntryMode = 0;
        transReferCurrencyCode = "";
        transReferCurrencyExponent = 0;
        defaultDDOL = "";
        defaultTDOL = "";
        supportOnlinePin = 0;
        needCompleteMatching = 0;
        termRiskManageData = "";

        contactlessLimit = 999999999999L;
        cvmLimit = 200000;
        contactlessFloorLimit = 200000;
        // C2
        kernelConfig = 0x20;
        ctlOnDeviceCVM = 0;
        ctlNoOnDeviceCVM = 0;
        cvmCapCVMRequired = 0;
        cvmCapNoCVMRequired = 0;
        mscvmCapCVMRequired = 0;
        mscvmCapNoCVMRequired = 0;
        contactlessKernelID = -1;
    }

    //  aid
    public String getAid() {
        return aid;
    }

    public void setAid(String aid) {
        this.aid = aid;
    }

    public int getAidLength() {
        return aid.length();
    }

    //	appLabel
    public String getAppLabel() {
        return appLabel;
    }

    public void setAppLabel(String appLabel) {
        this.appLabel = appLabel;
    }

    public int getAppLabelLength() {
        return appLabel.length();
    }

    //	appPreferredName
    public String getAppPreferredName() {
        return appPreferredName;
    }

    public void setAppPreferredName(String appPreferredName) {
        this.appPreferredName = appPreferredName;
    }

    public int getAppPreferredNameLength() {
        return appPreferredName.length();
    }

    //	appPriority
    public byte getAppPriority() {
        return appPriority;
    }

    public void setAppPriority(byte appPriority) {
        this.appPriority = appPriority;
    }

    //	termFloorLimit
    public int getTermFloorLimit() {
        return termFloorLimit;
    }

    public void setTermFloorLimit(int termFloorLimit) {
        this.termFloorLimit = termFloorLimit;
    }

    //	termActionCodeDefault
    public String getTACDefault() {
        return termActionCodeDefault;
    }

    public void setTACDefault(String tacDefault) {
        this.termActionCodeDefault = tacDefault;
    }

    //	termActionCodeDenial
    public String getTACDenial() {
        return termActionCodeDenial;
    }

    public void setTACDenial(String tacDenial) {
        this.termActionCodeDenial = tacDenial;
    }

    //	termActionCodeOnline
    public String getTACOnline() {
        return termActionCodeOnline;
    }

    public void setTACOnline(String tacOnline) {
        this.termActionCodeOnline = tacOnline;
    }

    //	targetPercentage
    public byte getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(byte targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    //	thresholdValue
    public int getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(int thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    //	maxTargetPercentage
    public byte getMaxTargetPercentage() {
        return maxTargetPercentage;
    }

    public void setMaxTargetPercentage(byte maxTargetPercentage) {
        this.maxTargetPercentage = maxTargetPercentage;
    }

    //	acquirerId
    public String getAcquirerId() {
        return acquirerId;
    }

    public void setAcquirerId(String acquirerId) {
        this.acquirerId = acquirerId;
    }

    //	mcc
    public String getMCC() {
        return mcc;
    }

    public void setMCC(String mcc) {
        this.mcc = mcc;
    }

    //	mid
    public String getMID() {
        return mid;
    }

    public void setMID(String mid) {
        this.mid = mid;
    }

    //	appVersionNumber
    public String getAppVersionNumber() {
        return appVersionNumber;
    }

    public void setAppVersionNumber(String appVersionNumber) {
        this.appVersionNumber = appVersionNumber;
    }

    //	posEntryMode
    public byte getPOSEntryMode() {
        return posEntryMode;
    }

    public void setPOSEntryMode(byte entryMode) {
        this.posEntryMode = entryMode;
    }

    //	transReferCurrencyCode
    public String getTransReferCurrencyCode() {
        return transReferCurrencyCode;
    }

    public void setTransReferCurrencyCode(String currencyCode) {
        this.transReferCurrencyCode = currencyCode;
    }

    //	transReferCurrencyExponent
    public byte getTransReferCurrencyExponent() {
        return transReferCurrencyExponent;
    }

    public void setTransReferCurrencyExponent(byte currencyExponent) {
        this.transReferCurrencyExponent = currencyExponent;
    }

    //	defaultDDOL
    public String getDefaultDDOL() {
        return defaultDDOL;
    }

    public void setDefaultDDOL(String ddol) {
        this.defaultDDOL = ddol;
    }

    //	defaultTDOL
    public String getDefaultTDOL() {
        return defaultTDOL;
    }

    public void setDefaultTDOL(String tdol) {
        this.defaultTDOL = tdol;
    }

    //	supportOnlinePin
    public byte getSupportOnlinePin() {
        return supportOnlinePin;
    }

    public void setSupportOnlinePin(byte supportOnlinePin) {
        this.supportOnlinePin = supportOnlinePin;
    }

    //	needCompleteMatching
    public byte getNeedCompleteMatching() {
        return needCompleteMatching;
    }

    public void setNeedCompleteMatching(byte needCompleteMatching) {
        this.needCompleteMatching = needCompleteMatching;
    }

    public String getTermRiskManageData() {
        return termRiskManageData;
    }

    public void setTermRiskManageData(String termRiskManageData) {
        this.termRiskManageData = termRiskManageData;
    }

    public long getContactlessLimit() {
        return contactlessLimit;
    }

    public void setContactlessLimit(long contactlessLimit) {
        this.contactlessLimit = contactlessLimit;
    }

    public long getCvmLimit() {
        return cvmLimit;
    }

    public void setCvmLimit(int cvmLimit) {
        this.cvmLimit = cvmLimit;
    }

    public long getContactlessFloorLimit() {
        return contactlessFloorLimit;
    }

    public void setContactlessFloorLimit(long contactlessFloorLimit) {
        this.contactlessFloorLimit = contactlessFloorLimit;
    }

    // C2
    public byte getKernelConfig() {
        return kernelConfig;
    }

    public void setKernelConfig(byte kernelConfig) {
        this.kernelConfig = kernelConfig;
    }

    public long getCtlOnDeviceCVM() {
        return ctlOnDeviceCVM;
    }

    public void setCtlOnDeviceCVM(long ctlOnDeviceCVM) {
        this.ctlOnDeviceCVM = ctlOnDeviceCVM;
    }

    public long getCtlNoOnDeviceCVM() {
        return ctlNoOnDeviceCVM;
    }

    public void setCtlNoOnDeviceCVM(long ctlNoOnDeviceCVM) {
        this.ctlNoOnDeviceCVM = ctlNoOnDeviceCVM;
    }

    public byte getCvmCapCVMRequired() {
        return cvmCapCVMRequired;
    }

    public void setCvmCapCVMRequired(byte cvmCapCVMRequired) {
        this.cvmCapCVMRequired = cvmCapCVMRequired;
    }

    public byte getCvmCapNoCVMRequired() {
        return cvmCapNoCVMRequired;
    }

    public void setCvmCapNoCVMRequired(byte cvmCapNoCVMRequired) {
        this.cvmCapNoCVMRequired = cvmCapNoCVMRequired;
    }

    public byte getMscvmCapCVMRequired() {
        return mscvmCapCVMRequired;
    }

    public void setMscvmCapCVMRequired(byte mscvmCapCVMRequired) {
        this.mscvmCapCVMRequired = mscvmCapCVMRequired;
    }

    public byte getMscvmCapNoCVMRequired() {
        return mscvmCapNoCVMRequired;
    }

    public void setMscvmCapNoCVMRequired(byte mscvmCapNoCVMRequired) {
        this.mscvmCapNoCVMRequired = mscvmCapNoCVMRequired;
    }

    public byte getContactlessKernelID() {
        return contactlessKernelID;
    }

    public void setContactlessKernelID(byte contactlessKernelID) {
        this.contactlessKernelID = contactlessKernelID;
    }

    public String getAuc() {
        return auc;
    }

    public void setAuc(String auc) {
        this.auc = auc;
    }

    public byte[] getDataBuffer() {
        byte[] data = new byte[512];
        int offset = 0;
        // 1 AID
        if (aid != null && aid.length() > 0) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x06;
            data[offset + 2] = (byte) (aid.length() / 2);
            System.arraycopy(StringUtil.hexString2bytes(aid), 0, data, offset + 3, aid.length() / 2);
            offset += (3 + aid.length() / 2);
        }
        // 2 CompleteMatching
        data[offset] = (byte) 0xDF;
        data[offset + 1] = 0x01;
        data[offset + 2] = 0x01;
        data[offset + 3] = needCompleteMatching;
        offset += 4;

        /*
        //auc
        if (this.auc != null) {
            byte[] value = Util.toByteArray(this.auc);
            data[offset] = (byte) 0x9f;
            data[offset + 1] = (byte) 0x07;
            data[offset + 2] = 0x02;
            data[offset + 3] = value[0];
            data[offset + 4] = value[1];
            offset += 5;
        }*/

        // 3 appVersion
        if (appVersionNumber != null && appVersionNumber.length() == 4) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x08;
            data[offset + 2] = 0x02;
            System.arraycopy(StringUtil.hexString2bytes(appVersionNumber), 0, data, offset + 3, 2);
            offset += 5;
        }
        // 4 TAC-Default
        if (termActionCodeDefault != null && termActionCodeDefault.length() == 10) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x11;
            data[offset + 2] = 0x05;
            System.arraycopy(StringUtil.hexString2bytes(termActionCodeDefault), 0, data, offset + 3, 5);
            offset += 8;
        }
        // 5 TAC-Online
        if (termActionCodeOnline != null && termActionCodeOnline.length() == 10) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x12;
            data[offset + 2] = 0x05;
            System.arraycopy(StringUtil.hexString2bytes(termActionCodeOnline), 0, data, offset + 3, 5);
            offset += 8;
        }
        // 6 TAC-Denial
        if (termActionCodeDenial != null && termActionCodeDenial.length() == 10) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x13;
            data[offset + 2] = 0x05;
            System.arraycopy(StringUtil.hexString2bytes(termActionCodeDenial), 0, data, offset + 3, 5);
            offset += 8;
        }
        // 7 terminal floor limit
        data[offset] = (byte) 0x9F;
        data[offset + 1] = 0x1B;
        data[offset + 2] = 0x04;
        System.arraycopy(NumberUtil.intToByte4(termFloorLimit), 0, data, offset + 3, 4);
        offset += 7;
        // 8 threshold value
        data[offset] = (byte) 0xDF;
        data[offset + 1] = 0x15;
        data[offset + 2] = 0x04;
        System.arraycopy(NumberUtil.intToByte4(thresholdValue), 0, data, offset + 3, 4);
        offset += 7;
        // 9 max target percentage
        data[offset] = (byte) 0xDF;
        data[offset + 1] = 0x16;
        data[offset + 2] = 0x01;
        data[offset + 3] = maxTargetPercentage;
        offset += 4;
        //10 target percentage
        data[offset] = (byte) 0xDF;
        data[offset + 1] = 0x17;
        data[offset + 2] = 0x01;
        data[offset + 3] = targetPercentage;
        offset += 4;
        //11 default DDOL
        if (defaultDDOL != null && defaultDDOL.length() > 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x14;
            data[offset + 2] = (byte) (defaultDDOL.length() / 2);
            System.arraycopy(StringUtil.hexString2bytes(defaultDDOL), 0, data, offset + 3, defaultDDOL.length() / 2);
            offset += (3 + defaultDDOL.length() / 2);
        }
        //12 support online pin
        data[offset] = (byte) 0xDF;
        data[offset + 1] = 0x18;
        data[offset + 2] = 0x01;
        data[offset + 3] = supportOnlinePin;
        offset += 4;

        //17 appLabel
        if (appLabel != null && appLabel.length() > 0) {
            data[offset] = (byte) 0x50;
            byte appLabelLength = (byte) appLabel.length();
            data[offset + 1] = appLabelLength;
            System.arraycopy(appLabel.getBytes(), 0, data, offset + 2, appLabelLength);
            offset += (2 + appLabelLength);
        }
        //18 app preferred Name
        if (appPreferredName != null && appPreferredName.length() > 0) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = (byte) 0x12;
            byte appPreferredNameLength = (byte) appPreferredName.length();
            data[offset + 2] = appPreferredNameLength;
            System.arraycopy(appPreferredName.getBytes(), 0, data, offset + 3, appPreferredNameLength);
            offset += (3 + appPreferredNameLength);
        }
        //19 appPriority
        data[offset] = (byte) 0x87;
        data[offset + 1] = 0x01;
        data[offset + 2] = appPriority;
        offset += 3;
        //20 mid
        if (mid != null && mid.length() > 0) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x16;
            data[offset + 2] = 15;
            System.arraycopy(StringUtil.fillSpace(mid, 15).getBytes(), 0, data, offset + 3, 15);
            offset += 18;
        }
        //21 acquirer id
        if (acquirerId != null && acquirerId.length() > 0) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x01;
            data[offset + 2] = (byte) 6;
            System.arraycopy(StringUtil.hexString2bytes(StringUtil.fillZero(acquirerId, 12)), 0, data, offset + 3, 6);
            offset += 9;
        }
        //22 mcc
        if (mcc != null && mcc.length() == 4) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x15;
            data[offset + 2] = (byte) 2;
            System.arraycopy(StringUtil.hexString2bytes(mcc), 0, data, offset + 3, 2);
            offset += 5;
        }
        //23 pos entry mode
        data[offset] = (byte) 0x9F;
        data[offset + 1] = 0x39;
        data[offset + 2] = (byte) 1;
        data[offset + 3] = posEntryMode;
        offset += 4;
        //24 trans reference currency code
        if (transReferCurrencyCode != null && transReferCurrencyCode.length() > 0) {
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x3C;
            data[offset + 2] = (byte) 2;
            System.arraycopy(StringUtil.hexString2bytes(StringUtil.fillZero(transReferCurrencyCode, 4)), 0, data, offset + 3, 2);
            offset += 5;
        }
        //25 trans reference currency exponent
        data[offset] = (byte) 0x9F;
        data[offset + 1] = 0x3D;
        data[offset + 2] = (byte) 1;
        data[offset + 3] = transReferCurrencyExponent;
        offset += 4;
        //26 default TDOL
        if (defaultTDOL != null && defaultTDOL.length() > 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x22;
            data[offset + 2] = (byte) (defaultTDOL.length() / 2);
            System.arraycopy(StringUtil.hexString2bytes(defaultTDOL), 0, data, offset + 3, defaultTDOL.length() / 2);
            offset += (3 + defaultTDOL.length() / 2);
        }
        if (!TextUtils.isEmpty(termRiskManageData)) {
            int termRiskManageDataLength = termRiskManageData.length();
            data[offset] = (byte) 0x9F;
            data[offset + 1] = 0x1D;
            data[offset + 2] = (byte) (termRiskManageDataLength / 2);
            System.arraycopy(StringUtil.hexString2bytes(termRiskManageData), 0, data, offset + 3, termRiskManageDataLength / 2);
            offset += (3 + termRiskManageDataLength / 2);
        }

        //contactlessLimit
        //cvmLimit
        //contactlessFloorLimit
        if (contactlessFloorLimit >= 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x19;
            data[offset + 2] = 0x06;
            System.arraycopy(StringUtil.hexString2bytes(String.format("%012d", contactlessFloorLimit)), 0, data, offset + 3, 6);
            offset += (3 + 6);

        }
        if (contactlessLimit >= 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x20;
            data[offset + 2] = 0x06;
            System.arraycopy(StringUtil.hexString2bytes(String.format("%012d", contactlessLimit)), 0, data, offset + 3, 6);
            offset += (3 + 6);
        }
        if (cvmLimit >= 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = 0x21;
            data[offset + 2] = 0x06;
            System.arraycopy(StringUtil.hexString2bytes(String.format("%012d", cvmLimit)), 0, data, offset + 3, 6);
            offset += (3 + 6);
        }

        if ("A000000004".equals(aid.substring(0, 10))
                || contactlessKernelID == 2
        ) {
            // C2
            // Reader Contactless Transaction Limit (No On-device CVM)			'DF8124'
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x24;
            data[offset + 3] = 6;
            System.arraycopy(StringUtil.hexString2bytes(StringUtil.fillZero(Long.toString(ctlNoOnDeviceCVM), 12)), 0, data, offset + 4, 6);
            offset += 10;

            // Reader Contactless Transaction Limit (On-device CVM)				'DF8125'
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x25;
            data[offset + 3] = 6;
            System.arraycopy(StringUtil.hexString2bytes(StringUtil.fillZero(Long.toString(ctlOnDeviceCVM), 12)), 0, data, offset + 4, 6);
            offset += 10;

            // CVM Capability - CVM Required				'DF8118'
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x18;
            data[offset + 3] = 1;
            data[offset + 4] = cvmCapCVMRequired;
            offset += 5;

            // CVM Capability - No CVM Required				'DF8119'
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x19;
            data[offset + 3] = 1;
            data[offset + 4] = cvmCapNoCVMRequired;
            offset += 5;

            // Mag-stripe CVM Capability - CVM Required      DF811E   - 0x20 (Online PIN)
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x1E;
            data[offset + 3] = 1;
            data[offset + 4] = mscvmCapCVMRequired;
            offset += 5;

            // Mag-stripe CVM Capability - No CVM Required   DF812C   - 0x20 (Online PIN)
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x2C;
            data[offset + 3] = 1;
            data[offset + 4] = mscvmCapNoCVMRequired;
            offset += 5;

            // Kernel Config                                 DF811B   - 0x20 (On device cardholder verification supported)
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x1B;
            data[offset + 3] = 1;
            data[offset + 4] = kernelConfig;
            offset += 5;
        }

        if (contactlessKernelID >= 0) {
            data[offset] = (byte) 0xDF;
            data[offset + 1] = (byte) 0x81;
            data[offset + 2] = 0x0C;
            data[offset + 3] = 1;
            data[offset + 4] = contactlessKernelID;
            offset += 5;
        }

        byte[] dataOut = new byte[offset];
        System.arraycopy(data, 0, dataOut, 0, dataOut.length);

        return dataOut;
    }

    @Override
    public String toString() {
        return "AIDT{" +
                "aid='" + aid + '\'' +
                ", appLabel='" + appLabel + '\'' +
                ", appPreferredName='" + appPreferredName + '\'' +
                ", appPriority=" + appPriority +
                ", termFloorLimit=" + termFloorLimit +
                ", termActionCodeDefault='" + termActionCodeDefault + '\'' +
                ", termActionCodeDenial='" + termActionCodeDenial + '\'' +
                ", termActionCodeOnline='" + termActionCodeOnline + '\'' +
                ", targetPercentage=" + targetPercentage +
                ", thresholdValue=" + thresholdValue +
                ", maxTargetPercentage=" + maxTargetPercentage +
                ", acquirerId='" + acquirerId + '\'' +
                ", mcc='" + mcc + '\'' +
                ", mid='" + mid + '\'' +
                ", appVersionNumber='" + appVersionNumber + '\'' +
                ", posEntryMode=" + posEntryMode +
                ", transReferCurrencyCode='" + transReferCurrencyCode + '\'' +
                ", transReferCurrencyExponent=" + transReferCurrencyExponent +
                ", defaultDDOL='" + defaultDDOL + '\'' +
                ", defaultTDOL='" + defaultTDOL + '\'' +
                ", supportOnlinePin=" + supportOnlinePin +
                ", needCompleteMatching=" + needCompleteMatching +
                ", termRiskManageData='" + termRiskManageData + '\'' +
                ", contactlessLimit=" + contactlessLimit +
                ", cvmLimit=" + cvmLimit +
                ", contactlessFloorLimit=" + contactlessFloorLimit +
                ", kernelConfig=" + kernelConfig +
                ", ctlOnDeviceCVM=" + ctlOnDeviceCVM +
                ", ctlNoOnDeviceCVM=" + ctlNoOnDeviceCVM +
                ", cvmCapCVMRequired=" + cvmCapCVMRequired +
                ", cvmCapNoCVMRequired=" + cvmCapNoCVMRequired +
                ", mscvmCapCVMRequired=" + mscvmCapCVMRequired +
                ", mscvmCapNoCVMRequired=" + mscvmCapNoCVMRequired +
                ", contactlessKernelID=" + contactlessKernelID +
                ", auc='" + auc + "'" +
                '}';
    }
}
