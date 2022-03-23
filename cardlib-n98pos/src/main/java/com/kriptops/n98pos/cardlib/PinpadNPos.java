package com.kriptops.n98pos.cardlib;

import android.util.Log;

import com.cloudpos.pinpad.KeyInfo;
import com.kriptops.n98pos.cardlib.bridge.CloseableDeviceWrapper;
import com.kriptops.n98pos.cardlib.crypto.PaddingMode;
import com.kriptops.n98pos.cardlib.db.IVController;
import com.kriptops.n98pos.cardlib.func.Consumer;
import com.kriptops.n98pos.cardlib.tools.Util;

public class PinpadNPos {
    public static final String IV_DATA = "iv_data";

    public static final String IV_PIN = "iv_pin";
    public static final String DEFAULT_IV = "0000000000000000";

    private static final int PINPAD_ENCRYPT_STRING_MODE_CBC = 1;

    private int ALG_3DES = 5;

    private int minLenPin = 4;
    private int maxLenPin = 6;
    private int timeout;
    private IVController ivController;

    public PinpadNPos(IVController ivController) {
        super();
        this.ivController = ivController;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setPinLength(int minLen, int maxLen){
        this.minLenPin = minLen;
        this.maxLenPin = maxLen;
    };

    public boolean updateKeys(String pinKeyHex, String dataKeyHex) {
        return this.updateKeys(
                pinKeyHex,
                DEFAULT_IV,
                dataKeyHex,
                DEFAULT_IV
        );
    }

    public boolean updateKeys(String pinKeyHex, String pinIvHex, String dataKeyHex, String dataIvHex) {
        //TODO Agregar validaciones de parametros
        //paso 0 tanto el pinkey como el data key deben ser de 32 caracteres hexadecimales
        //si no cumple emitir illegal argument exception

        //paso 1 convertir el pinKey en un byte array
        byte[] pinKey = Util.toByteArray(pinKeyHex);
        //paso 2 convertir el datakey en un byte array
        byte[] dataKey = Util.toByteArray(dataKeyHex);

        //inyectar en los slots respectivos, usaremos pin en el slot 0 y data en el slot 1
        //try {
            //this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_PIN_SLOT, pinKey);
            //this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_DATA_SLOT, dataKey);
        //} catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "No se puede actualizar las llaves", e);
        //    return false;
        //}

        this.ivController.saveIv(IV_DATA, dataIvHex);
        this.ivController.saveIv(IV_PIN, pinIvHex);

        return true;
    }

    public String encryptHex(String plain) {
        return this.encryptHex(plain, null);
    }

    public String encryptHex(String plain, PaddingMode paddingMode) {
        if (plain == null) return null;
        if (paddingMode == null) paddingMode = PaddingMode.F;
        plain = paddingMode.pad(plain);
        byte[] data = Util.toByteArray(plain);
        data = this.encrypt(data);
        return Util.toHexString(data);
    }

    public byte[] encrypt(byte[] plain) {
        KeyInfo info = new KeyInfo(
                2, //PINPadDevice.KEY_TYPE_MK_SK
                Defaults.MK_SLOT, // 0
                Defaults.UK_DATA_SLOT, // 1
                ALG_3DES
        );

        byte[] dataIv = getIv(IV_DATA);

        try {
            /*
            return this.getDevice().encryptData(
                    info,
                    plain,
                    PINPAD_ENCRYPT_STRING_MODE_CBC, //CBC
                    dataIv,
                    dataIv.length
            );
            */
            return dataIv;

        } catch (Exception e) {
            // Log.d(Defaults.LOG_TAG, "No puede encryptar");
            throw new RuntimeException(e);
        }
    }

    private byte[] getIv(String tag) {
        String ivHex = this.ivController.readIv(IV_DATA);
        if (ivHex == null) ivHex = DEFAULT_IV;
        byte[] iv = Util.toByteArray(ivHex);
        return iv;
    }

    public void setIvController(IVController ivController) {
        this.ivController = ivController;
    }
}
