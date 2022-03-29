package com.newpos.mposlib.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TlvUtil {
    public static final int CODE_LENGTH_OVERLENGTH = 52;
    public static final int CODE_PARAMS_INEXISTENCE = 53;
    public static final int CODE_VALUE_OVERLENGTH = 51;

    public static class TlvExcetion extends Exception {
        private static final long serialVersionUID = 5876132721837945560L;
        private int errCode;

        public TlvExcetion(String msg) {
            this(0, msg);
        }

        public TlvExcetion(int code, String msg) {
            super(msg);
            this.errCode = code;
        }

        public int getErrCode() {
            return this.errCode;
        }
    }

    public static Map<String, String> tlvToMap(String tlv) {
        return tlvToMap(hexStringToByte(tlv));
    }

    public static String mapToTlvStr(Map<String, String> map) {
        return bcd2str(mapToTlv(map));
    }

    public static byte[] mapToTlv(Map<String, String> map) {
        if (map == null) {
            throw new RuntimeException("map数据不能为null");
        }
        int len = 0;
        int lenght;
        for (Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                lenght = ((String) entry.getValue()).length() / 2;
                if (lenght <= 0) {
                    continue;
                } else if (lenght > 65535) {
                    throw new RuntimeException("value长度不能超过65535*2");
                } else {
                    if (lenght <= 127) {
                        len += 2;
                    }
                    if (lenght > 127 && lenght <= 255) {
                        len += 4;
                    }
                    if (lenght > 255 && lenght <= 65535) {
                        len += 6;
                    }
                    len = (len + ((String) entry.getValue()).length()) + ((String) entry.getKey()).length();
                }
            }
        }
        byte[] tlvData = new byte[(len / 2)];
        int pos = 0;
        for (Entry<String, String> entry2 : map.entrySet()) {
            if (entry2.getValue() != null) {
                byte[] value = hexStringToByte((String) entry2.getValue());
                lenght = value.length;
                if (lenght <= 0) {
                    continue;
                } else if (lenght > 65535) {
                    throw new RuntimeException("value长度不能超过65535*2");
                } else {
                    byte[] key = hexStringToByte((String) entry2.getKey());
                    System.arraycopy(key, 0, tlvData, pos, key.length);
                    pos += key.length;
                    if (lenght <= 127 && lenght > 0) {
                        tlvData[pos] = (byte) lenght;
                        pos++;
                    }
                    if (lenght > 127 && lenght <= 255) {
                        tlvData[pos] = (byte) -127;
                        pos++;
                        tlvData[pos] = (byte) lenght;
                        pos++;
                    }
                    if (lenght > 255 && lenght <= 65535) {
                        tlvData[pos] = (byte) -126;
                        pos++;
                        tlvData[pos] = (byte) ((lenght >> 8) & 255);
                        pos++;
                        tlvData[pos] = (byte) (lenght & 255);
                        pos++;
                    }
                    System.arraycopy(value, 0, tlvData, pos, lenght);
                    pos += lenght;
                }
            }
        }
        return tlvData;
    }

    public static Map<String, String> tlvToMap(byte[] tlv) {
        if (tlv == null) {
            throw new RuntimeException("tlv数据不能为null"); //los datos tlv no pueden ser nulos
        }
        Map<String, String> map = new HashMap();
        int index = 0;
        while (index < tlv.length) {
            byte[] tag;
            if ((tlv[index] & 0x1F) == 0x1F) {
                tag = new byte[2];
                System.arraycopy(tlv, index, tag, 0, 2);
                index = copyData(tlv, map, index + 2, tag);
            } else {
                tag = new byte[1];
                System.arraycopy(tlv, index, tag, 0, 1);
                index = copyData(tlv, map, index + 1, tag);
            }
        }
        return map;
    }

    private static int copyData(byte[] tlv, Map<String, String> map, int index, byte[] tag) {
        int length = 0;
        if ((tlv[index] >> 7) == 0) {
            length = tlv[index];
            index++;
        } else {
            int lenlen = tlv[index] & 0x7F;
            index++;
            for (int i = 0; i < lenlen; i++) {
                length = (length << 8) + (tlv[index] & 255);
                index++;
            }
        }
        byte[] value = new byte[length];
        System.arraycopy(tlv, index, value, 0, length);
        index += length;
        map.put(bcd2str(tag), bcd2str(value));
        return index;
    }

    public static String bcd2str(byte[] bcds) {
        char[] ascii = "0123456789abcdef".toCharArray();
        byte[] temp = new byte[(bcds.length * 2)];
        for (int i = 0; i < bcds.length; i++) {
            temp[i * 2] = (byte) ((bcds[i] >> 4) & 15);
            temp[(i * 2) + 1] = (byte) (bcds[i] & 15);
        }
        StringBuffer res = new StringBuffer();
        for (byte b : temp) {
            res.append(ascii[b]);
        }
        return res.toString().toUpperCase();
    }

    public static byte[] hexStringToByte(String hex) {
        hex = hex.toUpperCase();
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) ((toByte(achar[pos]) << 4) | toByte(achar[pos + 1]));
        }
        return result;
    }


    private static byte toByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap();
        map.put("5F11", null);
        map.put("5F12", "00120100");
        map.put("5F13", null);
        byte[] tlv = mapToTlv(map);
        Map<String, String> map1 = tlvToMap(tlv);
        for (String key : map1.keySet()) {
        }
    }

    /**
     *
     * @param src
     * @param totalLen
     * @param tag
     * @param value
     * @param withTL
     * @return
     */
    public static int getTlvData(byte[] src, int totalLen, int tag,
                                 byte[] value, boolean withTL) {
        int i, Tag, Len;
        int T;

        if (totalLen == 0)
            return 0;

        i = 0;
        while (i < totalLen) {
            T = i;

            if ((src[i] & 0x1f) == 0x1f) {
                Tag = StringUtil.byte2int(src, i, 2);
                i += 2;
            } else {
                Tag = StringUtil.byte2int(new byte[] { src[i++] });
            }

            Len = StringUtil.byte2int(new byte[] { src[i++] });
            if ((Len & (byte) 0x80) != 0) {
                int lenL = Len & 3;
                Len = StringUtil.byte2int(src, i, lenL);
                i += lenL;
            }
            if (tag == Tag) // 找到
            {
                if (withTL) // 包含Tag和Len
                {
                    Len = Len + (i - T);
                    System.arraycopy(src, T, value, 0, Len);
                    return Len;
                } else // 不包含Tag和Len
                {
                    System.arraycopy(src, i, value, 0, Len);
                    return Len;
                }
            } else
                i += Len;
        }
        return 0;
    }



    /**
     * pack a _tlv data
     *
     * @param result
     *            out
     * @param tag
     * @param len
     * @param value
     *            in
     * @return
     */
    public static int pack_tlv_data(byte[] result, int tag, int len,
                                    byte[] value, int valueOffset) {
        byte[] temp = null;
        int offset = 0;

        if (len == 0 || value == null || result == null)
            return 0;

        temp = result;
        if (tag > 0xff) {
            temp[offset++] = (byte) (tag >> 8);
            temp[offset++] = (byte) tag;
        } else
            temp[offset++] = (byte) tag;

        if (len < 128) {
            temp[offset++] = (byte) len;
        } else if (len < 256) {
            temp[offset++] = (byte) 0x81;
            temp[offset++] = (byte) len;
        } else {
            temp[offset++] = (byte) 0x82;
            temp[offset++] = (byte) (len >> 8);
            temp[offset++] = (byte) len;
        }

        // memmove(p, value, len);
        System.arraycopy(value, valueOffset, temp, offset, len);

        return offset + len;
    }

    public static int pack_tlv_data(String strTag, String value, byte[] result) {
        if(strTag != null && value != null && result != null) {
            int tag = Integer.parseInt(strTag, 16);
            byte[] valueBytes = StringUtil.hexStr2Bytes(value);
            return pack_tlv_data(result, tag, valueBytes.length, valueBytes, 0);
        }
        return 0;
    }


    /**
     * 根据AID查询内卡还是外卡
     */

    public static String getIssureByRid(String rid) {
        String cardCode = null;
        if (rid.length() < 10)
            return "CUP";
        if (rid.length() > 10) {
            cardCode = rid.substring(0, 10);
        } else
            cardCode = rid;

        if (cardCode.equals("A000000003"))
            return "VIS";
        if (cardCode.equals("A000000004"))
            return "MCC";
        if (cardCode.equals("A000000065"))
            return "JCB";
        if (cardCode.equals("A000000025"))
            return "AEX";
        return "CUP";
    }

    public static void sealAid(String aid){
        //List<EMVTLV> list = new ArrayList<EMVTLV>();
        EMVTLV emvtlv;
        for (int i=0; i<aid.length(); ){
            emvtlv = new EMVTLV();
            emvtlv.tag = aid.substring(i, i+4);
            String temp1 = aid.substring(i+4, i+5);
            Integer.parseInt(temp1);
            String temp2 = aid.substring(i+5, i+6);
            Integer.parseInt(temp2);
            emvtlv.length = Integer.parseInt(temp1)*16 + Integer.parseInt(temp2);
            emvtlv.value = aid.substring(i+6, i+6 +emvtlv.length*2);
            i += 6+emvtlv.length*2;
        }
    }

    static class EMVTLV{
        private String tag;
        private String value;
        private int length;
    }
}