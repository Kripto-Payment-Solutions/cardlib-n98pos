/**
 * Generated by smali2java 1.0.0.558
 * Copyright (C) 2013 Hensence.com
 */

package com.newpos.mposlib.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class StringUtil {

    public final static int SEC_MS =  1000;
    public final static int MIN_MS =  60 * SEC_MS;
    public final static int HOUR_MS = 60 * MIN_MS;
    public final static int DAY_MS = 24 * HOUR_MS;
    public final static int WEEK_MS = 7 * DAY_MS;

    public static int byte2int(byte[] bb, int offset, int len) {
        byte[] temp = new byte[len];
        System.arraycopy(bb, offset, temp, 0, len);
        return byte2int(temp);
    }

    public static int byte2int(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        for (int i = 0; i < 4 - bytes.length; i++) {
            byteBuffer.put((byte) 0);
        }
        for (int i = 0; i < bytes.length; i++) {
            byteBuffer.put(bytes[i]);
        }
        byteBuffer.position(0);
        return byteBuffer.getInt();
    }

    public static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
        } catch(Exception ex) {
            return false;
        }
        return true;
    }

    public static boolean isEqualsByte(byte[] src, int srcPos, byte[] bt2, int length) {
        byte[] temp = new byte[length];
        System.arraycopy(src, srcPos, temp, 0x0, length);
        return Arrays.equals(temp, bt2);
    }

    public static String str2DateTime(String format, String toformat, String time) {
        String str = "";
        try {
            Date date = new SimpleDateFormat(format).parse(time);
            return str;
        } catch(ParseException localParseException1) {
        }
        return str;
    }

    public static byte[] shortToByteArray(short s) {
        byte[] targets = new byte[0x2];
        targets[0x0] = (byte)(s & 0xff);
        targets[0x1] = (byte)(0xff00 >> 0x8);
        return targets;
    }

    public static byte[] shortToByteArrayTwo(short s) {
        byte[] targets = new byte[0x2];
        targets[0x1] = (byte)(s & 0xff);
        targets[0x0] = (byte)(0xff00 >> 0x8);
        return targets;
    }

    public static byte[] shortArrayToByteArray(short[] s) {
        byte[] targets = new byte[(s.length * 0x2)];
        for(int i = 0x0; i < s.length; i = i + 0x1) {
            byte[] tmp = shortToByteArray(s[i]);
            targets[(i * 0x2)] = tmp[0x0];
            targets[((i * 0x2) + 0x1)] = tmp[0x1];
        }
        return targets;
    }

    public static short[] byteArraytoShort(byte[] buf) {
        short[] targets = new short[(buf.length / 0x2)];
        int len = 0x0;
        for(int i = 0x0; i < buf.length; i = i + 0x2) {
            short vSample = (short)(buf[i] & 0xff);
            vSample = (short)((short)((short)buf[(i + 0x1)] << 0x8) | vSample);
            targets[(len ++)] = vSample;
        }
        return targets;
    }

    public static String str2HexStr(String str) {
        char[] arrayOfChar = "0123456789ABCDEF".toCharArray();
        StringBuilder localStringBuilder = new StringBuilder("");
        byte[] arrayOfByte = str.getBytes();
        for (int i = 0; i < arrayOfByte.length; i++)
        {
            localStringBuilder.append(arrayOfChar[((0xF0 & arrayOfByte[i]) >> 4)]);
            localStringBuilder.append(arrayOfChar[(0xF & arrayOfByte[i])]);
            localStringBuilder.append(' ');
        }
        return localStringBuilder.toString().trim();
    }

    public static String str2HexStrNoSpace(String str) {
        char[] arrayOfChar = "0123456789ABCDEF".toCharArray();
        StringBuilder localStringBuilder = new StringBuilder("");
        byte[] arrayOfByte = str.getBytes();
        for (int i = 0; i < arrayOfByte.length; i++)
        {
            localStringBuilder.append(arrayOfChar[((0xF0 & arrayOfByte[i]) >> 4)]);
            localStringBuilder.append(arrayOfChar[(0xF & arrayOfByte[i])]);
        }
        return localStringBuilder.toString().trim();
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[(hexStr.length() / 0x2)];
        for(int i = 0x0; i < bytes.length; i = i + 0x1) {
            int n = str.indexOf(hexs[(i * 0x2)]) * 0x10;
            n += str.indexOf(hexs[((i * 0x2) + 0x1)]);
            bytes[i] = (byte)(n & 0xff);
        }
        try {
            return new String(bytes, "ISO-8859-1");
        } catch(UnsupportedEncodingException localUnsupportedEncodingException1) {
        }
        return "";
    }

    public static byte[] str2bytesISO88591(String str) {
        try {
            return str.getBytes("ISO-8859-1");
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String bytes2GBK(byte[] data) {
        try {
            return new String(data, "GBK");
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        return "";
    }

    public static byte[] str2bytesGBK(String str) {
        try {
            return str.getBytes("GBK");
        }  catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String bytes2Ascii(byte[] data) {
        if (data == null) {
            return "";
        }
		
        try {
            return new String(data, "US-ASCII");
        }  catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static String bytes2HexStr(byte[] arr, int offset, int len) {
        StringBuilder sb = new StringBuilder();
        if(arr != null && 0 <= offset && ((offset + len) <= arr.length)) {
            for(int i = offset; i < (len + offset); i++) {
                sb.append(String.format("%02x", arr[i]));
            }
        }
        return sb.toString().toUpperCase();
    }

    public static String byte2HexStr(byte[] arr) {
        if (arr == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%02x", arr[i]));
        }
        return sb.toString().toUpperCase();
    }
    public static byte[] hexStr2Bytes(String s) {
        if (s.length() % 2 == 0) {
            return hex2byte(s.getBytes(), 0, s.length() >> 1);
        } else {
            // Padding left zero to make it even size #Bug raised by tommy
            return hexStr2Bytes("0" + s);
        }
    }

    /**
     * 右补字符
     * @param oriStr  原字符串
     * @param len  目标字符串长度
     * @param alexin  补位字符
     * @return  目标字符串
     */
    public static String padRight(String oriStr,int len,char alexin) {
        int strlen = oriStr.length();
        String str = new String("");
        if(strlen < len) {
            for(int i = 0; i < len-strlen; i++){
                str = str+alexin;
            }
        }
        str = oriStr + str;
        return str;
    }

    public static byte[] hex2byte(byte[] b, int offset, int len) {
        byte[] d = new byte[len];
        for (int i = 0; i < len * 2; i++) {
            // Buginfo when i oddness then this line won't be work
            // but in the for judge i>0 & i++ so i absolutely won't be oddness
            int shift = ((i % 2 == 1) ? 0 : 4);
            d[i >> 1] |= Character.digit((char) b[offset + i], 16) << shift;
        }
        return d;
    }

    public static String strToUnicode(String strText) throws Exception {
        StringBuilder str = new StringBuilder();
        for(int i = 0x0; i < strText.length(); i = i + 0x1) {
            char c = strText.charAt(i);
            char intAsc = c;
            String strHex = Integer.toHexString(intAsc);
            if(intAsc > 0x80) {
                str.append("\\u" + strHex);
                continue;
            }
            str.append("\\u00" + strHex);
        }
        return str.toString();
    }

    public static String unicodeToString(String hex) {
        int t = hex.length() / 0x6;
        StringBuilder str = new StringBuilder();
        for(int i = 0x0; i < t; i = i + 0x1) {
            String s = hex.substring((i * 0x6), ((i + 0x1) * 0x6));
            String s1 = s.substring(0x2, 0x4) + "00";
            String s2 = s.substring(0x4);
            int n = Integer.valueOf(s1, 0x10).intValue() + Integer.valueOf(s2, 0x10).intValue();
            char[] chars = Character.toChars(n);
            str.append(new String(chars));
        }
        return str.toString();
    }

    public static int byteToInt(byte[] src) {
        int tmp = 0x0;
        for(int i = 0x0; i < src.length; i = i + 0x1) {
            tmp += src[i];
        }
        return tmp;
    }

    public static int bytesToInt2(byte[] src, int offset) {
        int value = ((((src[offset] & 0xff) << 0x18) | ((src[(offset + 0x1)] & 0xff) << 0x10)) | ((src[(offset + 0x2)] & 0xff) << 0x8)) | (src[(offset + 0x3)] & 0xff);
        return value;
    }

    public static byte[] intToByte(int src) {
        byte[] tmp = new byte[0x4];
        for(int i = 0x0; i < tmp.length; i = i + 0x1) {
            tmp[i] = (byte)((src >> (i * 0x8)) & 0xff);
        }
        return tmp;
    }

    public static byte[] intToBytes2(int value) {
        byte[] src = new byte[0x4];
        src[0x0] = (byte)((value >> 0x18) & 0xff);
        src[0x1] = (byte)((value >> 0x10) & 0xff);
        src[0x2] = (byte)((value >> 0x8) & 0xff);
        src[0x3] = (byte)(value & 0xff);
        return src;
    }

    public static byte[] intToByte1024(int src) {
        byte[] tmp = new byte[0x400];
        for(int i = 0x0; i < tmp.length; i = i + 0x1) {
            tmp[i] = (byte)((src >> (i * 0x8)) & 0xff);
        }
        return tmp;
    }

    public static String byteToStr(byte[] paramArrayOfByte)
    {
        try
        {
            String str = new String(paramArrayOfByte, "ISO-8859-1");
            return str;
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static String byteToStrGBK(byte[] paramArrayOfByte)
    {
        try
        {
            String str = new String(paramArrayOfByte, "GBK");
            return str;
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static byte[] hexStringToByte(String hex) {
        int len = hex.length() / 0x2;
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for(int i = 0x0; i < len; i = i + 0x1) {
            int pos = i * 0x2;
            result[i] = (byte)((toByte(achar[pos]) << 0x4) | toByte(achar[(pos + 0x1)]));
        }
        return result;
    }

    private static byte toByte(char c) {
        byte b = (byte)"0123456789ABCDEF".indexOf(c);
        return b;
    }

    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        for(int i = 0x0; i < bArray.length; i = i + 0x1) {
            String sTemp = Integer.toHexString((bArray[i] & 0xff));
            if(sTemp.length() < 0x2) {
                sb.append(0x0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public static byte[] intToByteArray(int i) {
        byte[] targets = new byte[0x4];
        targets[0x0] = (byte)(i & 0xff);
        targets[0x1] = (byte)((i >> 0x8) & 0xff);
        targets[0x2] = (byte)((i >> 0x10) & 0xff);
        targets[0x3] = (byte)((i >> 0x18) & 0xff);
        return targets;
    }

    public static int byteArrayToInt(byte[] b) {
        int result = 0x0;
        result = (((b[0x0] & 0xff) | (b[0x1] << 0x8)) | (b[0x2] << 0x10)) | ((b[0x3] << 0x18) & -0x1);
        return result;
    }

    /**
     * convert bcd string to byte arrays
     * @param asc
     * @return
     */
    public static byte[] str2BCD(String asc) {
        int len = asc.length();
        int mod = len % 2;
        if (mod != 0) {
            asc = "0" + asc;
            len = asc.length();
        }
        if (len >= 2) {
            len = len / 2;
        }
        byte[] bbt = new byte[len];
        byte[] abt = asc.getBytes();
        int j, k;
        for (int p = 0; p < asc.length() / 2; p++) {
            if ((abt[2 * p] >= '0') && (abt[2 * p] <= '9')) {
                j = abt[2 * p] - '0';
            } else if ((abt[2 * p] >= 'a') && (abt[2 * p] <= 'z')) {
                j = abt[2 * p] - 'a' + 0x0a;
            } else {
                j = abt[2 * p] - 'A' + 0x0a;
            }
            if ((abt[2 * p + 1] >= '0') && (abt[2 * p + 1] <= '9')) {
                k = abt[2 * p + 1] - '0';
            } else if ((abt[2 * p + 1] >= 'a') && (abt[2 * p + 1] <= 'z')) {
                k = abt[2 * p + 1] - 'a' + 0x0a;
            } else {
                k = abt[2 * p + 1] - 'A' + 0x0a;
            }
            int a = (j << 4) + k;
            byte b = (byte) a;
            bbt[p] = b;
        }
        return bbt;
    }

    /**
     * calc LRC
     * @param data
     * @param from
     * @param to excluded last   index.
     * @return
     */
    public static byte calcLRC(byte[] data, int from, int to) {
        byte result = 0;
        if (data != null) {

            for (int i = from; i < to; i++) {
                result ^= data[i];
            }
        }

        return result;

    }

    /**
     * check LRC
     * @param buffer
     * @param checkValue
     * @return
     */
    public static boolean checkCommLRC(byte[] buffer, byte checkValue) {
        byte calcVal = StringUtil.calcLRC(buffer, 1, buffer.length - 1);
        ///LogUtil.d("calcVal:" + calcVal + " checkValue:" + checkValue);
        if (calcVal == checkValue) {
            return true;
        } else {
            return false;
        }
    }

    public static byte[] lenToLLVAR(int paramInt) {
        byte[] arrayOfByte1 = new byte[2];
        Locale localLocale = Locale.US;
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = Integer.valueOf(paramInt);
        byte[] arrayOfByte2 = str2BCD(String.format(localLocale, "%04d", arrayOfObject));
        arrayOfByte1[0] = arrayOfByte2[0];
        arrayOfByte1[1] = arrayOfByte2[1];
        return arrayOfByte1;
    }

    public static int llvarToLen(byte up, byte low) {
        return Integer.valueOf(StringUtil.byte2HexStr(new byte[]{up, low})).intValue();
    }
}
