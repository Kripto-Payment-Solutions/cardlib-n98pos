package com.kriptops.wizarpos.cardlib;

import android.util.Log;

import com.cloudpos.AlgorithmConstants;
import com.cloudpos.DeviceException;
import com.cloudpos.OperationListener;
import com.cloudpos.jniinterface.PinPadCallbackHandler;
import com.cloudpos.pinpad.KeyInfo;
import com.cloudpos.pinpad.PINPadDevice;
import com.cloudpos.pinpad.extend.PINPadExtendDevice;
import com.kriptops.wizarpos.cardlib.bridge.CloseableDeviceWrapper;
import com.kriptops.wizarpos.cardlib.crypto.PaddingMode;
import com.kriptops.wizarpos.cardlib.db.IVController;
import com.kriptops.wizarpos.cardlib.func.Consumer;
import com.kriptops.wizarpos.cardlib.tools.Util;

import java.io.Closeable;

public class Pinpad extends CloseableDeviceWrapper<PINPadExtendDevice> implements Closeable {

    private static final String IV_DATA = "iv_data";
    private static final String IV_PIN = "iv_pin";
    private static final String DEFAULT_IV = "0000000000000000";

    private static final int PINPAD_ENCRYPT_STRING_MODE_CBC = 1;
    private int minLenPin = 4;
    private int maxLenPin = 6;
    private int timeout;
    private IVController ivController;

    public Pinpad(PINPadDevice device, IVController ivController) {
        super((PINPadExtendDevice) device);
        this.ivController = ivController;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setPinLength(int minLen, int maxLen){
        this.minLenPin = minLen;
        this.maxLenPin = maxLen;
    };

    public boolean setGUIConfiguration(String key, String value) {
        try {
            this.getDevice().setGUIConfiguration(key, value);
            return true;
        } catch (DeviceException e) {
            e.printStackTrace();
            Log.i("PINPAD", "setGUIConfiguration errorCode:" + e.getCode());
            return false;
        }
    }

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
        try {
            this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_PIN_SLOT, pinKey);
            this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_DATA_SLOT, dataKey);
        } catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "No se puede actualizar las llaves", e);
            return false;
        }

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
                PINPadDevice.KEY_TYPE_MK_SK,
                Defaults.MK_SLOT, // 0
                Defaults.UK_DATA_SLOT, // 1
                AlgorithmConstants.ALG_3DES
        );

        byte[] dataIv = getIv(IV_DATA);

        try {
            return this.getDevice().encryptData(
                    info,
                    plain,
                    PINPAD_ENCRYPT_STRING_MODE_CBC, //CBC
                    dataIv,
                    dataIv.length
            );
        } catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "No puede encryptar");
            throw new RuntimeException(e);
        }
    }

    public boolean listenForPinBlock(String pan, OperationListener listener, Consumer<Integer> digitsAvailableListener) {
        if (!isOpen()) {
            return false;
        }
        KeyInfo keyInfo = new KeyInfo(
                PINPadDevice.KEY_TYPE_MK_SK,
                Defaults.MK_SLOT,
                Defaults.UK_PIN_SLOT,
                AlgorithmConstants.ALG_3DES
        );
        try {
            this.getDevice().showText(0, "Ingrese su pin en el teclado seguro");
            this.getDevice().setPINLength(this.minLenPin,this.maxLenPin);
            this.getDevice().setupCallbackHandler(new PinPadCallbackHandler() {
                @Override
                public void processCallback(byte[] bytes) {
                    // Log.d(Defaults.LOG_TAG, "pinpad event " + Util.toHexString(bytes));
                    if (digitsAvailableListener != null) {
                        int digits = bytes[0];
                        digitsAvailableListener.accept(digits);
                    }
                }

                @Override
                public void processCallback(int i, int i1) {
                    // Log.d(Defaults.LOG_TAG, "pinpad event a:" + i + ", b: " + i1);
                }
            });
            this.getDevice().listenForPinBlock(
                    keyInfo,
                    pan,
                    true,
                    listener,
                    timeout * 1000
            );
            return true;
        } catch (DeviceException e) {
            e.printStackTrace();
        }
        return false;
    }

    private byte[] getIv(String tag) {
        String ivHex = this.ivController.readIv(IV_DATA);
        if (ivHex == null) ivHex = DEFAULT_IV;
        byte[] iv = Util.toByteArray(ivHex);
        return iv;
    }
}
