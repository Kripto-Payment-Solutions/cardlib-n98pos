package com.kriptops.n98pos.demoapp.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

/**
 * RSAUtil.
 */

public class RSAUtil {
    private final static String TAG = RSAUtil.class.getSimpleName();
    public final static String PUBLIC_KEY_PATH = "/sdcard/RSA_PUBLIC.key";
    public final static String PRIVATE_KEY_PATH = "/sdcard/RSA_PRIVATE.key";

    public static byte[] generateRandom(int length) {
        Random random = new Random();
        byte[] data = new byte[length];
        random.nextBytes(data);
        return data;
    }

    public static byte[] encryptKey(byte[] key) {
        return null;
    }

    public static boolean generateRSAKeyPair(){
        KeyPairGenerator keyPairGenerator;
        try {
            /***
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            SecureRandom secureRandom = new SecureRandom(generateRandom(16));
            keyPairGenerator.initialize(1024, secureRandom);//2048
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            ***/
            File file = new File(PUBLIC_KEY_PATH);
            if(file.exists()){
                Log.d("N98Demo","RSA file exist");
                file.delete();
                //return true;
            }
            SecureRandom random = new SecureRandom();
            //RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F0);//1024
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F0);//1024
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(spec, random);
            generator.generateKeyPair();
            KeyPair keyPair = generator.generateKeyPair();
            //       byte [] rsa_public_Modulus=publicKey.getModulus().toByteArray();
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            Log.d("PUBLIC KEY",publicKeyBytes.toString());
            publicKeyBytes = Base64.encode(publicKeyBytes, Base64.DEFAULT);

            //File file = new File(PUBLIC_KEY_PATH);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException(
                            String.format("create new file error  %s",
                                    file.getAbsolutePath()));
                }
            }
            FileOutputStream fos = new FileOutputStream(PUBLIC_KEY_PATH);
            fos.write(publicKeyBytes);
            fos.close();

            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            Log.d("PRIVATE KEY",privateKeyBytes.toString());
            privateKeyBytes = Base64.encode(privateKeyBytes, Base64.DEFAULT);
            file = new File(PRIVATE_KEY_PATH);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException(
                            String.format("create new file error  %s",
                                    file.getAbsolutePath()));
                }
            }
            fos = new FileOutputStream(PRIVATE_KEY_PATH);
            fos.write(privateKeyBytes);
            fos.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static PublicKey getPublicKey(String filename) throws Exception {
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        keyBytes = Base64.decode(keyBytes, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey getPrivateKey(String filename)throws Exception {
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int)f.length()];
        dis.readFully(keyBytes);
        dis.close();
        keyBytes = Base64.decode(keyBytes, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec =new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static PublicKey getPublicKey(Context context, int certId) {

        try {
            InputStream in = context.getResources().openRawResource(certId);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate oCert = (X509Certificate) cf.generateCertificate(in);
            in.close();
            return oCert.getPublicKey();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * load RSA private key from pfx file.
     * @param filename
     * @param password
     * @return
     * @throws Exception
     */
    public static PrivateKey getPrivateKeyFromPfx(String filename, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        File f = new File(filename);
        FileInputStream fis = new FileInputStream(f);
        char[] nPassword = null;
        if (!TextUtils.isEmpty(password)) {
            nPassword = password.toCharArray();
        }
        ks.load(fis, nPassword);
        fis.close();

        System.out.println("keystore type=" + ks.getType());

        String keyAlias = "client1";
        System.out.println("is key entry=" + ks.isKeyEntry(keyAlias));
        PrivateKey prikey = (PrivateKey) ks.getKey(keyAlias, nPassword);
        Certificate cert = ks.getCertificate(keyAlias);
        PublicKey pubkey = cert.getPublicKey();

        System.out.println("cert class = " + cert.getClass().getName());
//        System.out.println("cert = " + cert);
//        System.out.println("public key = " + pubkey);
//        System.out.println("private key = " + prikey);

        return prikey;
    }
}
