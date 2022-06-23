package com.mpos.demo.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TDesUtil {
    /**
     * 初始向量默认
     */
    private final static byte[] ivbyte = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * 3DES加密 (亦称为：DESede加密) CBC模式；若为0填充，填充模式：零字节填充 （DESede/CBC/ZeroBytePadding）
     *
     * @param key  加密密钥
     * @param data 待加密数据
     * @return
     */
    public static byte[] encryptCBC(byte[] key, byte[] data) {
        byte[] result = null;
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] keyBytes = BaseUtils.copyOf(key, 24);
            int j = 0, k = 16;
            while (j < 8) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");
            IvParameterSpec iv3 = new IvParameterSpec(ivbyte);
            Cipher cipher3 = Cipher.getInstance("DESede/CBC/NoPadding");// 不填充
            cipher3.init(Cipher.ENCRYPT_MODE, key3, iv3);

            result = cipher3.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();

        }
        return result;
    }

    /**
     * 3DES 解密 （CBC）
     *
     * @param key  解密秘钥
     * @param data 带加密数据
     * @return
     */
    public static byte[] descryptCBC(byte[] key, byte[] data) {
        byte[] result = null;
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] keyBytes = BaseUtils.copyOf(key, 24);
            int j = 0, k = 16;
            while (j < 8) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");
            IvParameterSpec iv3 = new IvParameterSpec(ivbyte);
            Cipher cipher3 = Cipher.getInstance("DESede/CBC/NoPadding");
            cipher3.init(Cipher.DECRYPT_MODE, key3, iv3);

            result = cipher3.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 3DES 加密 (ECB)
     *
     * @param key
     * @param data
     * @return
     */
    public static byte[] encryptECB(byte[] key, byte[] data) {
        byte[] result = null;
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] keyBytes = BaseUtils.copyOf(key, 24);
            int j = 0, k = 16;
            while (j < 8) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher3 = Cipher.getInstance("DESede/ECB/NoPadding");// 不填充
            cipher3.init(Cipher.ENCRYPT_MODE, key3);

            result = cipher3.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();

        }
        return result;
    }

    /**
     * 3DES 解密 (ECB)
     *
     * @param key  解密秘钥
     * @param data 带加密数据
     * @return
     */
    public static byte[] descryptECB(byte[] key, byte[] data) {
        byte[] result = null;
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] keyBytes = BaseUtils.copyOf(key, 24);
            int j = 0, k = 16;
            while (j < 8) {
                keyBytes[k++] = keyBytes[j++];
            }

            SecretKey key3 = new SecretKeySpec(keyBytes, "DESede");
            Cipher cipher3 = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher3.init(Cipher.DECRYPT_MODE, key3);

            result = cipher3.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * MAC计算,数据不为8的倍数，需要补0，将数据8个字节通过单DES加密后，再将加密的结果与下一个8个字节异或
     * 一直到最后，将加密异或后的数据进行双DES计算得到MAC
     *
     * @param key   密钥
     * @param input 输入数据
     * @return 返回8字节MAC
     */
    public static byte[] calcMAC(byte[] key, byte[] input) {
        // input不够8的倍数填充0
        byte[] data = BaseUtils.arrayPadding(input, (byte) 0);
        int position = 0;

        // 每组数据8字节，第一组数据
        byte[] oper1 = new byte[8];
        System.arraycopy(data, position, oper1, 0, 8);
        position += 8;

        // 先对第一组数据加密
        oper1 = desEncrypt(key, oper1);

        int k = data.length / 8;

        for (int i = 1; i < k; i++) {
            // 后一组数据
            byte[] oper2 = new byte[8];
            System.arraycopy(data, position, oper2, 0, 8);
            // 异或结果
            byte[] xorRet = BaseUtils.bytesXor(oper1, oper2);
            if (i == k - 1) {
                // 最后一步使用双DES
                oper1 = encryptCBC(key, xorRet);
                break;
            } else {
                // 单倍DES加密
                oper1 = desEncrypt(key, xorRet);
            }
            position += 8;
        }

        return oper1;
    }

    /**
     * 计算MAC，先对input进行SHA-256计算得到32字节HASH值，再对HASH按银联MAC算法计算得到4字节MAC.
     *
     * @param key   密钥
     * @param input 输入数据
     * @return 返回MAC
     */
    public static byte[] mCalcMAC(byte[] key, byte[] input) {
        byte[] mac = new byte[4];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input);
            // input的Hash
            byte[] hash = digest.digest();

            byte[] unMac = calcMACUnionPay(key, hash);
            System.arraycopy(unMac, 0, mac, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return mac;
    }

    /**
     * 单倍DES加密算法
     *
     * @param key     加密密钥
     * @param content 被加密数据
     * @return
     */
    public static byte[] desEncrypt(byte[] key, byte[] content) {
        byte[] encryptedData = new byte[8];
        try {
            // content不够8的倍数填充0
            byte[] plainData = BaseUtils.arrayPadding(content, (byte) 0);

            DESKeySpec dks = new DESKeySpec(key);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");

            SecretKey secretKey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
            IvParameterSpec iv = new IvParameterSpec(ivbyte);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            encryptedData = cipher.doFinal(plainData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedData;
    }

    /**
     * 银联MAC算法 ;数据不为8的倍数 需要补0，将数据8个字节进行异或，再将异或的结果与下一个8个字节异或，一直到最后，将异或后的数据进行DES计算
     *
     * @param key   密钥
     * @param input 输入数据
     * @return 返回8字节MAC
     */
    public static byte[] calcMACUnionPay(byte[] key, byte[] input) {
        // input不够8的倍数填充0
        byte[] data = BaseUtils.arrayPadding(input, (byte) 0);
        int position = 0;
        // 每组数据8字节，第一组数据
        byte[] oper1 = new byte[8];
        System.arraycopy(data, position, oper1, 0, 8);
        position += 8;
        for (int i = 1; i < data.length / 8; i++) {
            byte[] oper2 = new byte[8];
            System.arraycopy(data, position, oper2, 0, 8);
            // 异或结果
            byte[] t = BaseUtils.bytesXor(oper1, oper2);
            oper1 = t;
            position += 8;
        }

        byte[] buff = null;
        try {
            buff = encryptCBC(key, oper1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 取8个长度字节
        byte[] retBuf = new byte[8];
        System.arraycopy(buff, 0, retBuf, 0, 8);
        return retBuf;
    }

}
