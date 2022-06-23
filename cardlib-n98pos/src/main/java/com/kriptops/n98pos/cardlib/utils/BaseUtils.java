package com.kriptops.n98pos.cardlib.utils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Vibrator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @author panjp
 * @function
 * @time 2014-9-28下午3:54:33
 */
public final class BaseUtils {


    private static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * 把16进制字符串转换成字节数组
     *
     * @param hexString
     * @return byte[]
     */
    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }


    /**
     * byte[] to HexString
     *
     * @param data
     * @return
     */
    public static String byteArr2HexStr(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (byte b : data) {
            String value = Integer.toHexString(b & 0xff);
            if (value.length() < 2) {
                sb.append("0");
            }
            sb.append(value);
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 将BCD字符串转化为byte数组
     *
     * @param bcdStr
     * @return
     */
    public static byte[] convertBCD2ByteArr(String bcdStr) {
        byte[] datas = new byte[bcdStr.length() / 2];
        for (int i = 0; i < bcdStr.length(); i += 2) {
            datas[i / 2] = (byte) (Short.valueOf(bcdStr.substring(i, i + 2).toString(), 16) & 0xff);
        }


        return datas;
    }

    /**
     * 将String型格式化,比如想要将2011-11-11格式化成2011年11月11日,就StringPattern("2011-11-11",
     * "yyyy-MM-dd","yyyy年MM月dd日").
     *
     * @param date       String 想要格式化的日期
     * @param oldPattern String 想要格式化的日期的现有格式
     * @param newPattern String 想要格式化成什么格式
     * @return String
     */
    public static String stringPattern(String date, String oldPattern, String newPattern) {
        String datetime = null;
        try {
            if (date == null || oldPattern == null || newPattern == null)
                return "";
            SimpleDateFormat sdf1 = new SimpleDateFormat(oldPattern); // 实例化模板对象
            SimpleDateFormat sdf2 = new SimpleDateFormat(newPattern); // 实例化模板对象
            Date d = null;
            try {
                d = sdf1.parse(date); // 将给定的字符串中的日期提取出来
                datetime = sdf2.format(d);
            } catch (Exception e) { // 如果提供的字符串格式有错误，则进行异常处理
                e.printStackTrace(); // 打印异常信息
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datetime;
    }

    /**
     * 生成指定长度的byte数组
     *
     * @param arraySize 数组长度
     * @return 返回生成的随机数组
     */
    public static byte[] generateRandByteArray(int arraySize) {
        byte[] result = new byte[arraySize];
        new Random().nextBytes(result);
        return result;
    }

    /**
     * 相同长度的byte数组异或
     *
     * @param b1 数组1
     * @param b2 数组2
     * @return 数组1和数组2的异或结果
     */
    public static byte[] bytesXor(byte[] b1, byte[] b2) {
        byte[] xorRet = null;
        if (b1 != null && b2 != null) {
            int b1Len = b1.length;
            int b2Len = b2.length;
            if (b1Len == b2Len) {
                xorRet = new byte[b1Len];
                for (int i = 0; i < b1Len; i++) {
                    xorRet[i] = (byte) ((b1[i] & 0xFF) ^ (b2[i] & 0xFF));
                }
            }
        }
        return xorRet;
    }

    /**
     * (8字节长度)byte数组异或
     */

    public static byte[] bytesXor(byte[] array) {

        byte[] b1 = new byte[]{array[0], array[1], array[2], array[3]};
        byte[] b2 = new byte[]{array[4], array[5], array[6], array[7]};
        byte[] xorRet = null;
        if (b1 != null && b2 != null) {
            int b1Len = b1.length;
            int b2Len = b2.length;
            if (b1Len == b2Len) {
                xorRet = new byte[b1Len];
                for (int i = 0; i < b1Len; i++) {
                    xorRet[i] = (byte) ((b1[i] & 0xFF) ^ (b2[i] & 0xFF));
                }
            }
        }
        return xorRet;
    }

    /**
     * Copies the specified array, truncating or padding with zeros (if
     * necessary) so the copy has the specified length. For all indices that are
     * valid in both the original array and the copy, the two arrays will
     * contain identical values. For any indices that are valid in the copy but
     * not the original, the copy will contain <tt>(byte)0</tt>. Such indices
     * will exist if and only if the specified length is greater than that of
     * the original array.
     *
     * @param original  the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros to
     * obtain the specified length
     * @throws NegativeArraySizeException if <tt>newLength</tt> is negative
     * @throws NullPointerException       if <tt>original</tt> is null
     * @since 1.6
     */
    public static byte[] copyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * 将int转换为byte数组表示，网络字顺序（高字节在前低字节在后）
     *
     * @param i
     * @return
     */
    public static byte[] int2ByteArr(int i) {
        byte[] arr = new byte[4];
        arr[0] = (byte) ((i >>> 24) & 0xFF);
        arr[1] = (byte) ((i >>> 16) & 0xFF);
        arr[2] = (byte) ((i >>> 8) & 0xFF);
        arr[3] = (byte) (i & 0xFF);
        return arr;
    }

    /**
     * 将byte数组转换为int，网络字顺序（高字节在前低字节在后）
     *
     * @param i
     * @return
     */
    public static int byteArrtoInt(byte[] arr) {
        int ret = 0;
        if (arr != null && arr.length == 4) {
            ret = ((arr[0] << 24) & 0xFFFFFFFF) | ((arr[1] << 16) & 0xFFFFFFFF)
                    | ((arr[2] << 8) & 0xFFFFFFFF)
                    | (arr[3] & 0xFFFFFFFF);
        }
        return ret;
    }

    /**
     * 数组逆序
     *
     * @param byteArray
     */
    public static void reverseArray(byte[] byteArray) {
        int i = 0, n = byteArray.length - 1;
        while (n > 2 * i) {
            byte x = byteArray[i];
            byteArray[i] = byteArray[n - i];
            byteArray[n - i] = x;
            i++;
        }

    }

    /**
     * 数组不够8的倍数，在数组后面补字节b,填满8的倍数
     *
     * @param data 源数据
     * @param b    填充数据
     * @return 返回填充后8的倍数的数组
     */
    public static byte[] arrayPadding(byte[] data, byte b) {
        byte[] ret = null;
        int len = data.length;
        int residue = len % 8;

        if (residue != 0) {
            // 需要填充的长度
            int fillLen = 8 - residue;

            int newLen = len + fillLen;
            ret = new byte[newLen];

            byte[] padding = new byte[fillLen];
            for (int i = 0; i < fillLen; i++) {
                padding[i] = b;
            }

            System.arraycopy(data, 0, ret, 0, len);
            System.arraycopy(padding, 0, ret, len, fillLen);

        } else {
            ret = data;
        }
        return ret;
    }

    /**
     * 判断是否有网络连接
     *
     * @param context
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    /**
     * 震动
     *
     * @param activity
     * @param milliseconds 毫秒数
     */
    public static void vibrate(Activity activity, long milliseconds) {
        Vibrator vib = (Vibrator) activity.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(milliseconds);
    }
}
