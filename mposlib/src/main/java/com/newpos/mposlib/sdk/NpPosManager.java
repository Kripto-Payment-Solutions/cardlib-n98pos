package com.newpos.mposlib.sdk;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.newpos.mposlib.R;
import com.newpos.mposlib.api.IBluetoothDevListener;
import com.newpos.mposlib.api.UpdateCallBackInterface;
import com.newpos.mposlib.bluetooth.BluetoothService;
import com.newpos.mposlib.exception.ConstantStr;
import com.newpos.mposlib.exception.ERRORS;
import com.newpos.mposlib.exception.EmvParams;
import com.newpos.mposlib.exception.SDKException;
import com.newpos.mposlib.impl.Command;
import com.newpos.mposlib.impl.DownloadFile;
import com.newpos.mposlib.model.AidVersion;
import com.newpos.mposlib.model.CalMacResponse;
import com.newpos.mposlib.model.DeviceSN;
import com.newpos.mposlib.model.InputPinResponse;
import com.newpos.mposlib.model.KeyType;
import com.newpos.mposlib.model.SwipeCardResponse;
import com.newpos.mposlib.model.TerminalInfo;
import com.newpos.mposlib.util.ContextUtils;
import com.newpos.mposlib.util.FileUtils;
import com.newpos.mposlib.util.HexUtil;
import com.newpos.mposlib.util.ISOUtil;
import com.newpos.mposlib.util.LogUtil;
import com.newpos.mposlib.util.QRCodeUtil;
import com.newpos.mposlib.util.StringUtil;
import com.newpos.mposlib.util.TimeUtils;
import com.newpos.mposlib.util.TlvUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.newpos.mposlib.exception.ConstantStr.DEFAULT_DATEPATTERN;
import static com.newpos.mposlib.util.TlvUtil.mapToTlv;

public class NpPosManager implements INpPosControler {

    private static NpPosManager instance;

    private NpPosManager() {
    }

    public static NpPosManager sharedInstance(Context context, INpSwipeListener posManagerDelegate) {
        if (instance == null) {
            synchronized (NpPosManager.class) {
                if (instance == null) {
                    instance = new NpPosManager();
                }
            }
        }
        instance.mListener = posManagerDelegate;
        if (context != null) {
            instance.mContext = context.getApplicationContext();
            ContextUtils.init(context.getApplicationContext());
        }
        mAidVersion = FileUtils.getAidVersion(instance.mContext);
        //LogUtil.d("sn:" + mAidVersion.getSn());
        //LogUtil.d("ver:" + mAidVersion.getVersion());
        return instance;
    }

    private INpSwipeListener mListener;
    private Context mContext;
    private static AidVersion mAidVersion;

    private boolean lUpdateWorkKeys;

    private IBluetoothDevListener mIBluetoothDevListener = new IBluetoothDevListener() {

        @Override
        public void onConnectedDevice(boolean isConnected) {
            if (isConnected) {
                syncTime();

                if (mListener != null) {
                    mListener.onDeviceConnected();

                    if (!TextUtils.isEmpty(mMacAddr) && !TextUtils.equals(mMacAddr, beforeAddr)) {
                        EmvParams.haveCheckParams = false;
                        LogUtil.d("not same mac");
                    } else {
                        LogUtil.d("same mac");
                    }
                }
            } else {
//                onError(ERRORS.DEVICE_CONNECT_ERROR, ERRORS.DEVICE_CONNECT_ERROR_DESC);
                onError(ERRORS.DEVICE_CONNECT_ERROR, mContext.getString(R.string.device_connect_error_desc));
            }
        }

        @Override
        public void onDisconnectedDevice() {
            initParams();
            if (mListener != null) {
                mListener.onDeviceDisConnected();
            }
        }

        @Override
        public void onSearchComplete() {

        }

        @Override
        public void onSearchOneDevice(BluetoothDevice bluetoothDevInfo) {
            if (mListener != null) {
                mListener.onScannerResult(bluetoothDevInfo);
            }
        }
    };

    @Override
    public void scanBlueDevice(int timeOut) {
        timeOut = timeOut * StringUtil.SEC_MS;
        BluetoothService.I().searchBluetoothDev(mIBluetoothDevListener, mContext, timeOut);
    }

    @Override
    public void stopScan() {
        BluetoothService.I().stopSearch();
    }

    private volatile String mMacAddr;
    private volatile String beforeAddr;

    public boolean isUpdateWorkKeys(){ return this.lUpdateWorkKeys; }

    @Override
    public void connectBluetoothDevice(String macAddr) {
        this.mMacAddr = macAddr;
        BluetoothService.I().setCallback(mIBluetoothDevListener);
        BluetoothService.I().connectDevice(macAddr);
    }

    @Override
    public boolean isConnected() {
        return BluetoothService.I().isConnected();
    }

    private void initParams() {
        mTerminalInfo = null;
        isSetTime = false;
        EmvParams.haveCheckParams = false;
    }

    @Override
    public void disconnectDevice() {
        Command.I().resetSomeCommand();
        initParams();
        if (BluetoothService.I().isConnected()) {
            BluetoothService.I().stop(true);
        } else {
            if (mListener != null) {
                mListener.onDeviceDisConnected();
            }
        }
    }

    private final static int TYPE_MPOS_WITH_PINPAD = 0;
    private volatile TerminalInfo mTerminalInfo;

    @Override
    public void getDeviceInfo() {
        try {
            if (mTerminalInfo == null || mTerminalInfo.getStrSNCode() == null) {
                mTerminalInfo = Command.getTerminalInfo();
            }

            byte[] bat = new byte[2];
            System.arraycopy(Command.getBatteryAndState(), 0, bat, 0, bat.length);
            if (mTerminalInfo != null) {
                if (mListener != null) {
                    DeviceInfoEntity deviceInfoEntity = new DeviceInfoEntity();
                    // 0: mpos with pinpad
                    deviceInfoEntity.setDeviceTypeStr(mTerminalInfo.getStrProductId());
                    deviceInfoEntity.setDeviceType(TYPE_MPOS_WITH_PINPAD);
                    deviceInfoEntity.setKsn(mTerminalInfo.getStrSNCode());
                    float batteryPer = Float.parseFloat(StringUtil.byteToStrGBK(bat));
                    deviceInfoEntity.setCurrentElePer(batteryPer);
                    deviceInfoEntity.setFirmwareVer(mTerminalInfo.getStrAppVersion());
                    mListener.onGetDeviceInfo(deviceInfoEntity);
                }
            } else {
//                onError(ERRORS.DEVICE_GET_INFO_ERROR, ERRORS.DEVICE_GET_INFO_ERROR_DESC);
                onError(ERRORS.DEVICE_GET_INFO_ERROR, mContext.getString(R.string.device_get_info_error_desc));
            }
        } catch (Throwable e) {
//            onError(ERRORS.DEVICE_GET_INFO_ERROR, ERRORS.DEVICE_GET_INFO_ERROR_DESC);
            onError(ERRORS.DEVICE_GET_INFO_ERROR, mContext.getString(R.string.device_get_info_error_desc));
        }

    }

    private final static String RSA_EXP = "000003";//""010001";

    @Override
    public void getTransportSessionKey(final String publicKey) {
        try {
            byte[] modules = StringUtil.hexStr2Bytes(publicKey);
            byte[] exps = StringUtil.hexStr2Bytes(RSA_EXP);

            String result = Command.getTransportSessionKey(modules, exps);
            if (result != null) {
                if (mListener != null) {
                    mListener.onGetTransportSessionKey(result);
                }
            } else {
                onError(ERRORS.DEVICE_GET_TRANSPORT_SESSION_KEY_ERROR, mContext.getString(R.string.device_get_transport_session_key_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }

            onError(ERRORS.DEVICE_GET_TRANSPORT_SESSION_KEY_ERROR, mContext.getString(R.string.device_get_transport_session_key_error_desc));
        }
    }

    @Override
    public void updateMasterKey(final String masterKey) {
        byte KEKType ;
        LogUtil.e("updateMasterKey ="+masterKey);
        if(masterKey.length() > 40){
            KEKType = 1;
        }else{
            KEKType = 0;
        }
        try {
            byte index ;
            byte[] result = null;
            if(KEKType==0) {
                index = KeyType.MASTERKEY;
                if (masterKey.length() == 24) {
                    LogUtil.e("updateMasterKey length 24");
                    byte[] masterKeyBytes = StringUtil.hexStr2Bytes(masterKey.substring(0, 16));
                    byte checkMode = 1;
                    byte[] checkValue = StringUtil.hexStr2Bytes(masterKey.substring(16, 24));
                    result = Command.loadMasterKey(KEKType, index, masterKeyBytes, checkMode,
                            checkValue);
                } else if (masterKey.length() == 40) {
                    LogUtil.e("updateMasterKey length 40");
                    byte[] masterKeyBytes = StringUtil.hexStr2Bytes(masterKey.substring(0, 32));
                    byte checkMode = 1;
                    byte[] checkValue = StringUtil.hexStr2Bytes(masterKey.substring(32, 40));
                    result = Command.loadMasterKey(KEKType, index, masterKeyBytes, checkMode,
                            checkValue);
                }
            } else if(KEKType==1){
                //for dukpt
                //IPEK + IKSN
                LogUtil.e("IPEK + KSN"); //16+10
                index = KeyType.DUKPTKEY;
                byte[] masterKeyBytes = StringUtil.hexStr2Bytes(masterKey.substring(0, 52));
                byte checkMode = 0;
                byte[] checkValue = StringUtil.hexStr2Bytes(masterKey.substring(52, 60));
                result = Command.loadMasterKey(KEKType, index, masterKeyBytes, checkMode,
                        checkValue);
            }/*{
//IPEK + IKSN
                LogUtil.e("IPEK=KSN"); //16+10,if encrypt
                byte[] masterKeyBytes = StringUtil.hexStr2Bytes(masterKey.substring(0, 52));
                byte checkMode = 0;
       //         byte[] checkValue = StringUtil.hexStr2Bytes(masterKey.substring(64, 72));//first 8 byte for IPEK, last 8 byet for IKSN
  //              result = Command.loadMasterKey(KEKType, index, masterKeyBytes, checkMode,
   //                  new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00});//   checkValue);
                result = Command.loadMasterKey(KEKType, index, masterKeyBytes, checkMode,
                       ISOUtil.hex2byte("B2DE27"));//   checkValue);
            }*/
            LogUtil.e("updateMasterKey result ="+ISOUtil.byte2hex(result));
            if (result != null) {
                if (mListener != null) {
                    mListener.onUpdateMasterKeySuccess();
                }
            } else {
//                onError(ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR,
//                        ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR_DESC);
                onError(ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR,
                        mContext.getString(R.string.device_update_master_key_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR,
//                    ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR_DESC);
            onError(ERRORS.DEVICE_UPDATE_MASTER_KEY_ERROR,
                    mContext.getString(R.string.device_update_master_key_error_desc));
        }
    }

    public boolean updateKeys(byte []encryptPIN, byte []PINkcv,
                              byte []encryptMAC, byte []MACkcv,
                              byte []encryptTrack, byte []TRACKkcv){

        //String pinKeyHex, String pinIvHex, String dataKeyHex, String dataIvHex;

        //TODO Agregar validaciones de parametros
        //paso 0 tanto el pinkey como el data key deben ser de 32 caracteres hexadecimales
        //si no cumple emitir illegal argument exception

        //paso 1 convertir el pinKey en un byte array
        //byte[] pinKey = Util.toByteArray(pinKeyHex);
        //paso 2 convertir el datakey en un byte array
        //byte[] dataKey = Util.toByteArray(dataKeyHex);


        try {
            this.updateWorkingKey(ISOUtil.byte2hex(encryptPIN)+ISOUtil.byte2hex(PINkcv,0,4),
                    ISOUtil.byte2hex(encryptMAC)+ISOUtil.byte2hex(MACkcv,0,4),
                    ISOUtil.byte2hex(encryptTrack)+ISOUtil.byte2hex(TRACKkcv,0,4));

            return this.isUpdateWorkKeys();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }

        //inyectar en los slots respectivos, usaremos pin en el slot 0 y data en el slot 1
        //try {
        //    this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_PIN_SLOT, pinKey);
        //   this.getDevice().updateUserKey(Defaults.MK_SLOT, Defaults.UK_DATA_SLOT, dataKey);
        //} catch (DeviceException e) {
        //    // Log.d(Defaults.LOG_TAG, "No se puede actualizar las llaves", e);
        //    return false;
        //}

        //this.ivController.saveIv(IV_DATA, dataIvHex);
        //this.ivController.saveIv(IV_PIN, pinIvHex);
    }

    private byte[] updateKey(byte masterKeyIndex, byte keyType, byte keyIndex, String keyMaterial) throws SDKException {
        byte[] result;
        byte[] wKey;
        byte[] checkValue;
        byte checkMode;
        if (keyMaterial.length() == 32) {
            wKey = StringUtil.hexStr2Bytes(keyMaterial);
            checkValue = new byte[8];
            checkMode = 0;
        } else if (keyMaterial.length() == 24) {
            wKey = StringUtil.hexStr2Bytes(keyMaterial.substring(0, 16));
            checkValue = StringUtil.hexStr2Bytes(keyMaterial.substring(16, 24));
            checkMode = 1;
        } else if (keyMaterial.length() == 40) {
            wKey = StringUtil.hexStr2Bytes(keyMaterial.substring(0, 32));
            checkValue = StringUtil.hexStr2Bytes(keyMaterial.substring(32, 40));
            checkMode = 1;
        } else {
            return null;
        }
        result = Command.loadWorkKey(keyType, masterKeyIndex, keyIndex, wKey,
                checkMode, checkValue);
        return result;
    }

    /**
     * @param pinKey   PIN密钥密文 + KCV
     * @param macKey   MAC密钥密文 + KCV
     * @param trackKey 磁道密钥密文 + KCV
     */
    @Override
    public void updateWorkingKey(String pinKey, String macKey, String trackKey) {
        try {
            this.lUpdateWorkKeys =  false;
            syncTime();

            byte mainKeyIndex = KeyType.MASTERKEY;
            byte checkMode = 1;
            byte[] result = null;
            if (pinKey != null) {
                byte keyType = KeyType.PIN;
                byte wKeyIndex = KeyType.PIN;

                //Inyeccion de solo dos llaves
                result = updateKey(mainKeyIndex, keyType, wKeyIndex, pinKey);

                /*
                if (pinKey.length() == 24) {
                    byte[] wKey = StringUtil.hexStr2Bytes(pinKey.substring(0, 16));
                    byte[] checkValue = StringUtil.hexStr2Bytes(pinKey.substring(16, 24));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                } else if (pinKey.length() == 40) {
                    byte[] wKey = StringUtil.hexStr2Bytes(pinKey.substring(0, 32));
                    byte[] checkValue = StringUtil.hexStr2Bytes(pinKey.substring(32, 40));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                }
                */
            }

            if (macKey != null) {
                byte keyType = KeyType.MAC;
                byte wKeyIndex = KeyType.MAC;

                result = updateKey(mainKeyIndex, keyType, wKeyIndex, macKey);

                /*
                if (macKey.length() == 24) {
                    byte[] wKey = StringUtil.hexStr2Bytes(macKey.substring(0, 16));
                    byte[] checkValue = StringUtil.hexStr2Bytes(macKey.substring(16, 24));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                } else if (macKey.length() == 40) {
                    byte[] wKey = StringUtil.hexStr2Bytes(macKey.substring(0, 32));
                    byte[] checkValue = StringUtil.hexStr2Bytes(macKey.substring(32, 40));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                }
                */
            }

            if (trackKey != null) {
                byte keyType = KeyType.TRACK;
                byte wKeyIndex = KeyType.TRACK;

                result = updateKey(mainKeyIndex, keyType, wKeyIndex, trackKey);

                /*
                if (trackKey.length() == 24) {
                    byte[] wKey = StringUtil.hexStr2Bytes(trackKey.substring(0, 16));
                    byte[] checkValue = StringUtil.hexStr2Bytes(trackKey.substring(16, 24));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                } else if (trackKey.length() == 40) {
                    byte[] wKey = StringUtil.hexStr2Bytes(trackKey.substring(0, 32));
                    byte[] checkValue = StringUtil.hexStr2Bytes(trackKey.substring(32, 40));
                    result = Command.loadWorkKey(keyType, mainKeyIndex, wKeyIndex, wKey,
                            checkMode, checkValue);
                }
                */
            }

            if (result != null) {
                downloadAidOnly();
                if (mListener != null) {
                    this.lUpdateWorkKeys =  true;
                    mListener.onUpdateWorkingKeySuccess();
                }
            } else {
//                onError(ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR,
//                        ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR_DESC);
                onError(ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR,
                        mContext.getString(R.string.device_update_work_key_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR,
//                    ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR_DESC);
            onError(ERRORS.DEVICE_UPDATE_WORK_KEY_ERROR,
                    mContext.getString(R.string.device_update_work_key_error_desc));
        }
    }

    @Override
    public void clearAids() {
        try {
            Command.operateAID((byte) 3, null);
            if (mListener != null) {
                mListener.onClearAids();
            }
            return;
        } catch (SDKException e) {
            e.printStackTrace();
        }
//        onError(ERRORS.DELETE_ALL_AID_ERROR,
//                ERRORS.DELETE_ALL_AID_ERROR_DESC);
        onError(ERRORS.DELETE_ALL_AID_ERROR,
                mContext.getString(R.string.delete_all_aid_error_desc));
    }

    @Override
    public void addAid(String aid) {
        try {
            boolean result = Command.operateAID((byte) 2, HexUtil.toBCD(aid));
            if (result) {
                if (mListener != null) {
                    mListener.onAddAidSuccess();
                }
            } else {
//                onError(ERRORS.DEVICE_ADD_AID_ERROR,
//                        ERRORS.DEVICE_ADD_AID_ERROR_DESC);
                onError(ERRORS.DEVICE_ADD_AID_ERROR,
                        mContext.getString(R.string.device_add_aid_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_ADD_AID_ERROR,
//                    ERRORS.DEVICE_ADD_AID_ERROR_DESC);
            onError(ERRORS.DEVICE_ADD_AID_ERROR,
                    mContext.getString(R.string.device_add_aid_error_desc));
        }
    }

    @Override
    public void clearRids() {
        try {
            Command.operatePubicKey((byte) 3, null);
            if (mListener != null) {
                mListener.onClearRids();
            }
            return;
        } catch (SDKException e) {
            e.printStackTrace();
        }
//        onError(ERRORS.DELETE_ALL_RID_ERROR,
//                ERRORS.DELETE_ALL_RID_ERROR_DESC);
        onError(ERRORS.DELETE_ALL_RID_ERROR,
                mContext.getString(R.string.delete_all_rid_error_desc));
    }

    @Override
    public void addRid(String rid) {
        try {
            boolean result = Command.operatePubicKey((byte) 2, HexUtil.toBCD(rid));
            if (result) {
                if (mListener != null) {
                    mListener.onAddRidSuccess();
                }
            } else {
//                onError(ERRORS.DEVICE_ADD_RID_ERROR,
//                        ERRORS.DEVICE_ADD_RID_ERROR_DESC);
                onError(ERRORS.DEVICE_ADD_RID_ERROR,
                        mContext.getString(R.string.device_add_rid_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            //                onError(ERRORS.DEVICE_ADD_RID_ERROR,
//                        ERRORS.DEVICE_ADD_RID_ERROR_DESC);
            onError(ERRORS.DEVICE_ADD_RID_ERROR,
                    mContext.getString(R.string.device_add_rid_error_desc));
        }
    }

    private final static int TYPE_READ_CARD_NUMBER = 1;
    private final static int TYPE_SALE = 0;

    @Override
    public void getCardNumber(int timeout) {
        CardReadEntity cardReadEntity = new CardReadEntity();
        cardReadEntity.setSupportFallback(false);
        cardReadEntity.setTimeout(timeout);
        cardReadEntity.setAmount("000000080000");
        cardReadEntity.setTradeType(TYPE_SALE);
        readCard(cardReadEntity);
    }

    @Override
    public void getCurrentBatteryStatus() {
        try {
            byte[] bat = new byte[2];
            System.arraycopy(Command.getBatteryAndState(), 0, bat, 0, bat.length);
            int batteryPer = Integer.parseInt(StringUtil.bytes2GBK(bat));

            if (mListener != null) {
                if (batteryPer > 20) {
                    mListener.onGetDeviceBattery(true);
                } else {
                    mListener.onGetDeviceBattery(false);
                }
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_GET_BATTERY_ERROR,
//                    ERRORS.DEVICE_GET_BATTERY_ERROR_DESC);
            onError(ERRORS.DEVICE_GET_BATTERY_ERROR,
                    mContext.getString(R.string.device_get_battery_error_desc));
        }
    }
    public void readCard_UPTS(final CardReadEntity cardReadEntity) {
        try {
   //         checkEmvParams();
            byte type = 7;
            byte tmo = (byte) cardReadEntity.getTimeout();
            byte[] message = ConstantStr.Tips.READ_CARD_TIPS.getBytes("GBK");
            byte fallback = 1;

            byte[] result = Command.openCardReader(type, tmo, message, fallback);
            LogUtil.e("test readcard ="+ISOUtil.byte2hex(result));
            int length=ISOUtil.bcd2int(result,0,2);

            LogUtil.e("test readcard length="+length);
            int cardType = ISOUtil.bcd2int(result[2]);
            LogUtil.e("test readcard cardType="+cardType);
            byte[]data=new byte[length-1];
            System.arraycopy(result,3,data,0,length-1);
            switch (cardType) {
                case 9:
                    LogUtil.e("test readcard processEMVtag="+cardType+"    date ="+ISOUtil.byte2hex(data));
                    processEMVtag(data);
                    break;

                default:
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_GET_CARD_NO_FAIL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_get_card_no_fail));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_FAIL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_fail));
                    }
                    break;
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof SDKException) {
                String errorCode = ((SDKException) e).getErrCode();
                if (TextUtils.equals(errorCode, SDKException.ERR_CODE_USER_CANCEL)) {
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_CANCEL);
                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, mContext.getString(R.string.device_get_card_no_cancel));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_CANCEL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_cancel));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_TIME_OUT)) {
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_TIMEOUT);
                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, mContext.getString(R.string.device_get_card_no_timeout));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_TIMEOUT);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_timeout));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_RESET)) {
                    //nothing
                    return;
                }
            }

            if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_FAIL);
                onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_get_card_no_fail));
            } else {
//                onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_FAIL);
                onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_fail));
            }
        }

    }
    @Override
    public void readCard(final CardReadEntity cardReadEntity) {
        try {
            checkEmvParams();
           // byte transType = (byte)cardReadEntity.getTradeType();
            byte cardType = (byte)cardReadEntity.getReadCardType();
            byte tmo = (byte) cardReadEntity.getTimeout();
            byte[] message = ConstantStr.Tips.READ_CARD_TIPS.getBytes("GBK");
            byte fallback = 1;

            byte[] result = Command.openCardReader(cardType, tmo, message, fallback);
            LogUtil.e("test readcard ="+ISOUtil.byte2hex(result));
            int RespcardType = result[0];
            switch (RespcardType) {
                case 1:
                    //magcard
                    getMagCard(cardReadEntity);
                    break;

                case 2:
                    //iccard
                    getIcCard(cardReadEntity);
                    break;

                case 4:
                    //rfcard
                    getRfCard(cardReadEntity);
                    break;

                default:
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_GET_CARD_NO_FAIL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_get_card_no_fail));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_FAIL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_fail));
                    }
                    break;
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof SDKException) {
                String errorCode = ((SDKException) e).getErrCode();
                if (TextUtils.equals(errorCode, SDKException.ERR_CODE_USER_CANCEL)) {
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_CANCEL);
                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, mContext.getString(R.string.device_get_card_no_cancel));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_CANCEL);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_cancel));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_TIME_OUT)) {
                    if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_TIMEOUT);
                        onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, mContext.getString(R.string.device_get_card_no_timeout));
                    } else {
//                        onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_TIMEOUT);
                        onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_timeout));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_RESET)) {
                    //nothing
                    return;
                }
            }

            if (cardReadEntity.getTradeType() == TYPE_READ_CARD_NUMBER) {
//                onError(ERRORS.DEVICE_GET_CARD_NO_ERROR, ERRORS.DEVICE_GET_CARD_NO_FAIL);
                onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_get_card_no_fail));
            } else {
//                onError(ERRORS.DEVICE_READ_CARD_ERROR, ERRORS.DEVICE_READ_CARD_FAIL);
                onError(ERRORS.DEVICE_READ_CARD_ERROR, mContext.getString(R.string.device_read_card_fail));
            }
        }

    }

    final static class CardType {
        public static int MAG_CARD = 0;
        public static int IC_CARD = 1;
        public static int RF_CARD = 2;
    }

    private void getMagCard(CardReadEntity cardReadEntity) throws SDKException {
        SwipeCardResponse swipeCardResponse = Command.readTrackDataWithUnencrypted((byte) 1);
        if (swipeCardResponse != null) {
            if (mListener != null) {
                CardInfoEntity cardInfoEntity = new CardInfoEntity();
/*                try {
                    //String pan = swipeCardResponse.getPan();
                    //String random = pan.substring(pan.length() - 6, pan.length());
                    //DeviceSN deviceSN = Command.getDeviceSn(random.getBytes());
                    //cardInfoEntity.setTusn(deviceSN.getTusn());
                    //cardInfoEntity.setEncryptedSN(deviceSN.getEncryptTusn());
                    //cardInfoEntity.setDeviceType(deviceSN.getDeviceType());
                    //cardInfoEntity.setKsn(random);
                } catch (Throwable e) {
                    e.printStackTrace();
                }*/
                cardInfoEntity.setCardType(CardType.MAG_CARD);
                Log.e("N98","service code ="+swipeCardResponse.getTrack2Servicecode());
                if (TextUtils.isEmpty(swipeCardResponse.getEncryptedTrack2Data())) {
                    LogUtil.d("track data no encrypted");
                    cardInfoEntity.setTrack1(swipeCardResponse.getOneTrack());
                    cardInfoEntity.setTrack2(swipeCardResponse.getTwoTrack());
                    cardInfoEntity.setTrack3(swipeCardResponse.getThreeTrack());
                } else {
                    cardInfoEntity.setTrack1(swipeCardResponse.getEncryptedTrack1Data());
                    cardInfoEntity.setTrack2(swipeCardResponse.getEncryptedTrack2Data());
                    cardInfoEntity.setTrack3(swipeCardResponse.getEncryptedTrack3Data());
                }
                cardInfoEntity.setCardNumber(swipeCardResponse.getPan());
                cardInfoEntity.setExpDate(swipeCardResponse.getExpiryDate());
                cardInfoEntity.setCsn(swipeCardResponse.getCarSeq());
                cardInfoEntity.setIc55Data(swipeCardResponse.getIcParams());

                mListener.onGetReadCardInfo(cardInfoEntity);
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    final class TradeType {
        public final static int SALE = 0;
        public final static int GET_CARD_NUMBER = 1;
    }
    private void processEMVtag( byte[] result )
    {
        LogUtil.e("test readcard ="+ISOUtil.byte2hex(result));
        Map<String, String> dataMap = TlvUtil.tlvToMap(result);
        String executeResult = (String) dataMap.get("DF75");
        if (TextUtils.equals(executeResult, "00")) {
            String unEncTrack2Data = dataMap.get("57");
            String encTrack2Data = dataMap.get("DF81");
            String pan = dataMap.get("5A");

            String expireDT = "";
            if (unEncTrack2Data != null) {
                int index = unEncTrack2Data.indexOf("D");
                if (index != -1) {
                    expireDT = unEncTrack2Data.substring(index + 1, index + 5);
                }
            }
            String cardSeq = "";
            cardSeq = dataMap.get("5F34");
            if (cardSeq != null) {
                int seq = Integer.valueOf(cardSeq).intValue();
                cardSeq = String.format("%03d", seq);
            }
            if (pan != null) {
                pan = pan.replace("F", "");
            }
            else{
                if(unEncTrack2Data!=null) {
                    int index = unEncTrack2Data.indexOf("D");
                    if(index>0&&index<unEncTrack2Data.length()) {
                        pan = unEncTrack2Data.substring(0, index);

                    }
                }
            }
            StringBuilder filed55 = new StringBuilder();
            String tag9F26 = dataMap.get("9F26");
            String tag9F27 = dataMap.get("9F27");
            String tag9F10 = dataMap.get("9F10");
            String tag9F37 = dataMap.get("9F37");
            String tag9F36 = dataMap.get("9F36");
            String tag95 = dataMap.get("95");
            String tag9A = dataMap.get("9A");
            String tag9C = dataMap.get("9C");
            String tag9F02 = dataMap.get("9F02");
            String tag5F2A = dataMap.get("5F2A");
            String tag82 = dataMap.get("82");
            String tag9F1A = dataMap.get("9F1A");
            String tag9F03 = dataMap.get("9F03");
            String tag9F33 = dataMap.get("9F33");
            String tag9F34 = dataMap.get("9F34");
            String tag9F35 = dataMap.get("9F35");
            String tag9F1E = dataMap.get("9F1E");
            String tag84 = dataMap.get("84");
            String tag9F09 = dataMap.get("9F09");
            String tag9F41 = dataMap.get("9F41");
            String tag9F63 = dataMap.get("9F63");

            byte[] ret = new byte[256];
            int packLen = 0;
            if (tag9F26 != null) {
                packLen = TlvUtil.pack_tlv_data("9F26", tag9F26, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F27 != null) {
                packLen = TlvUtil.pack_tlv_data("9F27", tag9F27, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F10 != null) {
                packLen = TlvUtil.pack_tlv_data("9F10", tag9F10, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F37 != null) {
                packLen = TlvUtil.pack_tlv_data("9F37", tag9F37, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F36 != null) {
                packLen = TlvUtil.pack_tlv_data("9F36", tag9F36, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag95 != null) {
                packLen = TlvUtil.pack_tlv_data("95", tag95, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9A != null) {
                packLen = TlvUtil.pack_tlv_data("9A", tag9A, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            } else {
                packLen = TlvUtil.pack_tlv_data("9A", TimeUtils.getCurrentDate(), ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9C != null) {
                packLen = TlvUtil.pack_tlv_data("9C", tag9C, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F02 != null) {
                packLen = TlvUtil.pack_tlv_data("9F02", tag9F02, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag5F2A != null) {
                packLen = TlvUtil.pack_tlv_data("5F2A", tag5F2A, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag82 != null) {
                packLen = TlvUtil.pack_tlv_data("82", tag82, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F1A != null) {
                packLen = TlvUtil.pack_tlv_data("9F1A", tag9F1A, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F03 != null) {
                packLen = TlvUtil.pack_tlv_data("9F03", tag9F03, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F33 != null) {
                packLen = TlvUtil.pack_tlv_data("9F33", tag9F33, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F34 != null) {
                packLen = TlvUtil.pack_tlv_data("9F34", tag9F34, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F35 != null) {
                packLen = TlvUtil.pack_tlv_data("9F35", tag9F35, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F1E != null) {
                packLen = TlvUtil.pack_tlv_data("9F1E", tag9F1E, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag84 != null) {
                packLen = TlvUtil.pack_tlv_data("84", tag84, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F09 != null) {
                packLen = TlvUtil.pack_tlv_data("9F09", tag9F09, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F41 != null) {
                packLen = TlvUtil.pack_tlv_data("9F41", tag9F41, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }

            if (tag9F63 != null) {
                packLen = TlvUtil.pack_tlv_data("9F63", tag9F63, ret);
                filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
            }


            CardInfoEntity cardInfoEntity = new CardInfoEntity();
            try {
                String random = pan.substring(pan.length() - 6, pan.length());
      //          DeviceSN deviceSN = Command.getDeviceSn(random.getBytes());
       //         cardInfoEntity.setTusn(deviceSN.getTusn());
       //         cardInfoEntity.setEncryptedSN(deviceSN.getEncryptTusn());
     //           cardInfoEntity.setDeviceType(deviceSN.getDeviceType());
    //            cardInfoEntity.setKsn(random);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            cardInfoEntity.setCardType(CardType.IC_CARD);
            if (TextUtils.isEmpty(encTrack2Data)) {
                cardInfoEntity.setTrack1("");
                cardInfoEntity.setTrack2(unEncTrack2Data);
                cardInfoEntity.setTrack3("");
            } else {
                cardInfoEntity.setTrack1("");
                cardInfoEntity.setTrack2(encTrack2Data);
                cardInfoEntity.setTrack3("");
            }
            cardInfoEntity.setCardNumber(pan);
            cardInfoEntity.setExpDate(expireDT);
            cardInfoEntity.setCsn(cardSeq);
            cardInfoEntity.setIc55Data(filed55.toString());
            mListener.onGetReadCardInfo(cardInfoEntity);
            return;
        }
    }
    private void getIcCard(final CardReadEntity cardReadEntity) throws SDKException {
        if (mListener != null) {
            mListener.onDetachedIC();
        }

        Map<String, String> map = new HashMap();
        if (cardReadEntity.getTradeType() == TradeType.SALE) {
            map.put("9C", "00");
            map.put("9F02", cardReadEntity.getAmount());
            map.put("9F1A","0840");//terminal country code
            map.put("5F2A","0840");//transaction currency code
            map.put("9F41","00000001");
            map.put("5C","9F119F129B50");

        } else {
            map.put("9C", "F1");
            map.put("9F02", "000000000000");
            map.put("9F1A","0840");//terminal country code
            map.put("5F2A","0840");//transaction currency code
        }
        byte[] result = Command.executeStandardProcess(cardReadEntity.getTimeout(), mapToTlv(map));
        if (result != null) {
            if (cardReadEntity.getTradeType() == TradeType.GET_CARD_NUMBER) {
                String pan = TlvUtil.tlvToMap(result).get("5A");
                if (pan != null) {
                    pan = pan.replace("F", "");
                    if (mListener != null) {
                        CardInfoEntity cardInfoEntity = new CardInfoEntity();
                        cardInfoEntity.setCardType(CardType.IC_CARD);
                        cardInfoEntity.setCardNumber(pan);
                        mListener.onGetReadCardInfo(cardInfoEntity);
                        return;
                    }
                }
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            } else {
                if (mListener != null) {
                    Map<String, String> dataMap = TlvUtil.tlvToMap(result);
                    String executeResult = (String) dataMap.get("DF75");
                    if (TextUtils.equals(executeResult, "00")) {
                        String unEncTrack2Data = dataMap.get("57");
                        String encTrack2Data = dataMap.get("DF81");
                        Log.e("N98","track2 ="+encTrack2Data);
                        String pan = dataMap.get("5A");
                        if (pan != null) {
                            pan = pan.replace("F", "");
                        }
                        String expireDT = "";
                        if (unEncTrack2Data != null) {
                            int index = unEncTrack2Data.indexOf("D");
                            if (index != -1) {
                                expireDT = unEncTrack2Data.substring(index + 1, index + 5);
                            }
                        }
                        String cardSeq = "";
                        cardSeq = dataMap.get("5F34");
                        if (cardSeq != null) {
                            int seq = Integer.valueOf(cardSeq).intValue();
                            cardSeq = String.format("%03d", seq);
                        }

                        StringBuilder filed55 = new StringBuilder();
                        String tag9F26 = dataMap.get("9F26");
                        String tag9F27 = dataMap.get("9F27");
                        String tag9F10 = dataMap.get("9F10");
                        String tag9F37 = dataMap.get("9F37");
                        String tag9F36 = dataMap.get("9F36");
                        String tag95 = dataMap.get("95");
                        String tag9A = dataMap.get("9A");
                        String tag9C = dataMap.get("9C");
                        String tag9F02 = dataMap.get("9F02");
                        String tag5F2A = dataMap.get("5F2A");
                        String tag82 = dataMap.get("82");
                        String tag9F1A = dataMap.get("9F1A");
                        String tag9F03 = dataMap.get("9F03");
                        String tag9F33 = dataMap.get("9F33");
                        String tag9F34 = dataMap.get("9F34");
                        String tag9F35 = dataMap.get("9F35");
                        String tag9F1E = dataMap.get("9F1E");
                        String tag84 = dataMap.get("84");
                        String tag9F09 = dataMap.get("9F09");
                        String tag9F41 = dataMap.get("9F41");
                        String tag9F63 = dataMap.get("9F63");

                        byte[] ret = new byte[256];
                        int packLen = 0;
                        if (tag9F26 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F26", tag9F26, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F27 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F27", tag9F27, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F10 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F10", tag9F10, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F37 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F37", tag9F37, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F36 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F36", tag9F36, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag95 != null) {
                            packLen = TlvUtil.pack_tlv_data("95", tag95, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9A != null) {
                            packLen = TlvUtil.pack_tlv_data("9A", tag9A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        } else {
                            packLen = TlvUtil.pack_tlv_data("9A", TimeUtils.getCurrentDate(), ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9C != null) {
                            packLen = TlvUtil.pack_tlv_data("9C", tag9C, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F02 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F02", tag9F02, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag5F2A != null) {
                            packLen = TlvUtil.pack_tlv_data("5F2A", tag5F2A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag82 != null) {
                            packLen = TlvUtil.pack_tlv_data("82", tag82, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F1A != null) {
                            packLen = TlvUtil.pack_tlv_data("9F1A", tag9F1A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F03 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F03", tag9F03, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F33 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F33", tag9F33, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F34 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F34", tag9F34, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F35 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F35", tag9F35, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F1E != null) {
                            packLen = TlvUtil.pack_tlv_data("9F1E", tag9F1E, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag84 != null) {
                            packLen = TlvUtil.pack_tlv_data("84", tag84, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F09 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F09", tag9F09, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F41 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F41", tag9F41, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F63 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F63", tag9F63, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }


                        CardInfoEntity cardInfoEntity = new CardInfoEntity();
                        try {
                            String random = pan.substring(pan.length() - 6, pan.length());
                            DeviceSN deviceSN = Command.getDeviceSn(random.getBytes());
                            cardInfoEntity.setTusn(deviceSN.getTusn());
                            cardInfoEntity.setEncryptedSN(deviceSN.getEncryptTusn());
                            cardInfoEntity.setDeviceType(deviceSN.getDeviceType());
                            cardInfoEntity.setKsn(random);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        cardInfoEntity.setCardType(CardType.IC_CARD);
                        if (TextUtils.isEmpty(encTrack2Data)) {
                            cardInfoEntity.setTrack1("");
                            cardInfoEntity.setTrack2(unEncTrack2Data);
                            cardInfoEntity.setTrack3("");
                        } else {
                            cardInfoEntity.setTrack1("");
                            cardInfoEntity.setTrack2(encTrack2Data);
                            cardInfoEntity.setTrack3("");
                        }
                        cardInfoEntity.setCardNumber(pan);
                        cardInfoEntity.setExpDate(expireDT);
                        cardInfoEntity.setCsn(cardSeq);
                        cardInfoEntity.setIc55Data(filed55.toString());
                        mListener.onGetReadCardInfo(cardInfoEntity);
                        return;
                    }
                    throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                } else {
                    throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                }
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    private void getRfCard(CardReadEntity cardReadEntity) throws SDKException {
        Map<String, String> map = new HashMap();
        if (cardReadEntity.getTradeType() == TradeType.SALE) {
            map.put("9C", "00");
            map.put("9F02", cardReadEntity.getAmount());
            map.put("9F03","000000000000");
            map.put("5F2A","0860");
            //map.put("5C","9F119F129B50");
        } else {
            map.put("9C", "F1");
            map.put("9F02", "000000000000");
        }
        byte[] result = Command.executeQPBOCStandardProcess(cardReadEntity.getTimeout(), mapToTlv(map));
        if (result != null) {
            if (cardReadEntity.getTradeType() == TradeType.GET_CARD_NUMBER) {
                String pan = TlvUtil.tlvToMap(result).get("5A");
                if (pan != null) {
                    pan = pan.replace("F", "");
                    if (mListener != null) {
                        CardInfoEntity cardInfoEntity = new CardInfoEntity();
                        cardInfoEntity.setCardType(CardType.RF_CARD);
                        cardInfoEntity.setCardNumber(pan);
                        mListener.onGetReadCardInfo(cardInfoEntity);
                        return;
                    }
                }

                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            } else {
                if (mListener != null) {
                    Map<String, String> dataMap = TlvUtil.tlvToMap(result);
                    String executeResult = (String) dataMap.get("DF75");
                    if (TextUtils.equals(executeResult, "00")) {
                        String unEncTrack2Data = dataMap.get("57");
                        String encTrack2Data = dataMap.get("DF81");
                        String pan = "";
                        String expireDT = "";
                        if (unEncTrack2Data != null) {
                            int index = unEncTrack2Data.indexOf("D");
                            if (index != -1) {
                                pan = unEncTrack2Data.substring(0, index);
                                expireDT = unEncTrack2Data.substring(index + 1, index + 5);
                            }
                        }
                        String cardSeq = "";
                        cardSeq = dataMap.get("5F34");
                        if (cardSeq != null) {
                            int seq = Integer.valueOf(cardSeq).intValue();
                            cardSeq = String.format("%03d", seq);
                        }

                        StringBuilder filed55 = new StringBuilder();
                        String tag9F26 = dataMap.get("9F26");
                        String tag9F27 = dataMap.get("9F27");
                        String tag9F10 = dataMap.get("9F10");
                        String tag9F37 = dataMap.get("9F37");
                        String tag9F36 = dataMap.get("9F36");
                        String tag95 = dataMap.get("95");
                        String tag9A = dataMap.get("9A");
                        String tag9C = dataMap.get("9C");
                        String tag9F02 = dataMap.get("9F02");
                        String tag5F2A = dataMap.get("5F2A");
                        String tag82 = dataMap.get("82");
                        String tag9F1A = dataMap.get("9F1A");
                        String tag9F03 = dataMap.get("9F03");
                        String tag9F33 = dataMap.get("9F33");
                        String tag9F34 = dataMap.get("9F34");
                        String tag9F35 = dataMap.get("9F35");
                        String tag9F1E = dataMap.get("9F1E");
                        String tag84 = dataMap.get("84");
                        String tag9F09 = dataMap.get("9F09");
                        String tag9F41 = dataMap.get("9F41");
                        String tag9F63 = dataMap.get("9F63");

                        byte[] ret = new byte[256];
                        int packLen = 0;
                        if (tag9F26 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F26", tag9F26, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F27 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F27", tag9F27, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F10 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F10", tag9F10, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F37 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F37", tag9F37, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F36 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F36", tag9F36, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag95 != null) {
                            packLen = TlvUtil.pack_tlv_data("95", tag95, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9A != null) {
                            packLen = TlvUtil.pack_tlv_data("9A", tag9A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        } else {
                            packLen = TlvUtil.pack_tlv_data("9A", TimeUtils.getCurrentDate(), ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9C != null) {
                            packLen = TlvUtil.pack_tlv_data("9C", tag9C, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F02 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F02", tag9F02, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag5F2A != null) {
                            packLen = TlvUtil.pack_tlv_data("5F2A", tag5F2A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag82 != null) {
                            packLen = TlvUtil.pack_tlv_data("82", tag82, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F1A != null) {
                            packLen = TlvUtil.pack_tlv_data("9F1A", tag9F1A, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F03 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F03", tag9F03, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F33 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F33", tag9F33, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F34 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F34", tag9F34, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F35 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F35", tag9F35, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F1E != null) {
                            packLen = TlvUtil.pack_tlv_data("9F1E", tag9F1E, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag84 != null) {
                            packLen = TlvUtil.pack_tlv_data("84", tag84, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F09 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F09", tag9F09, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F41 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F41", tag9F41, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        if (tag9F63 != null) {
                            packLen = TlvUtil.pack_tlv_data("9F63", tag9F63, ret);
                            filed55.append(StringUtil.bytes2HexStr(ret, 0, packLen));
                        }

                        CardInfoEntity cardInfoEntity = new CardInfoEntity();
                        try {
                            String random = pan.substring(pan.length() - 6);
                            DeviceSN deviceSN = Command.getDeviceSn(random.getBytes());
                            cardInfoEntity.setTusn(deviceSN.getTusn());
                            cardInfoEntity.setEncryptedSN(deviceSN.getEncryptTusn());
                            cardInfoEntity.setDeviceType(deviceSN.getDeviceType());
                            cardInfoEntity.setKsn(random);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        cardInfoEntity.setCardType(CardType.RF_CARD);
                        if (TextUtils.isEmpty(encTrack2Data)) {
                            cardInfoEntity.setTrack1("");
                            cardInfoEntity.setTrack2(unEncTrack2Data);
                            cardInfoEntity.setTrack3("");
                        } else {
                            cardInfoEntity.setTrack1("");
                            cardInfoEntity.setTrack2(encTrack2Data);
                            cardInfoEntity.setTrack3("");
                        }
                        cardInfoEntity.setCardNumber(pan);
                        cardInfoEntity.setExpDate(expireDT);
                        cardInfoEntity.setCsn(cardSeq);
                        cardInfoEntity.setIc55Data(filed55.toString());
                        mListener.onGetReadCardInfo(cardInfoEntity);
                    }
                } else {
                    throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                }
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    private final static int INPUT_AMOUNT = 0;
    private final static int INPUT_PINPAD = 1;
    private final static int INPUT_NO_PINPAD = 2;

    @Override
    public void getInputInfoFromKB(final InputInfoEntity inputInfoEntity) {
        try {
            byte index = KeyType.PIN;
            byte keyType = 0;
            byte[] pan = new byte[10];
            byte[] keyLenLimit = new byte[]{0, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            byte isUseEnterKey = 1;

            byte tmo = (byte) inputInfoEntity.getTimeout();
            byte[] displayData;
            if (inputInfoEntity.getInputType() == INPUT_AMOUNT) {
                displayData = StringUtil.str2bytesGBK(ConstantStr.Tips.INPUT_AMOUNT);
            } else if (inputInfoEntity.getInputType() == INPUT_PINPAD) {
                pan = StringUtil.padRight(inputInfoEntity.getPan(), 20, 'F').getBytes();
                displayData = StringUtil.str2bytesGBK(ConstantStr.Tips.INPUT_PIN);
            } else {
//                onError(ERRORS.DEVICE_INPUT_INFO_ERROR, ERRORS.DEVICE_INPUT_INFO_ERROR_DESC);
                onError(ERRORS.DEVICE_INPUT_INFO_ERROR, mContext.getString(R.string.device_input_info_error_desc));
                return;
            }

            if (inputInfoEntity.getInputType() == INPUT_AMOUNT) {
                String amount = Command.inputAmount(tmo, displayData);
                if (mListener != null) {
                    mListener.onGetReadInputInfo(amount);
                }
            } else {
                InputPinResponse result = Command.inputPin(index, keyType, pan, keyLenLimit,
                        isUseEnterKey, tmo, displayData);
                if (result != null) {
                    if (mListener != null) {
                        mListener.onGetReadInputInfo(result.getEncryptedData());
                    }
                } else {
//                    onError(ERRORS.DEVICE_GET_PIN_ERROR, ERRORS.DEVICE_GET_PIN_ERROR_DESC);
                    onError(ERRORS.DEVICE_GET_PIN_ERROR, mContext.getString(R.string.device_get_pin_error_desc));
                }
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof SDKException) {
                String errorCode = ((SDKException) e).getErrCode();
                if (TextUtils.equals(errorCode, SDKException.ERR_CODE_USER_CANCEL)) {
                    if (inputInfoEntity.getInputType() == INPUT_AMOUNT) {
//                        onError(ERRORS.DEVICE_INPUT_INFO_ERROR, ERRORS.DEVICE_GET_AMOUNT_CANCEL);
                        onError(ERRORS.DEVICE_INPUT_INFO_ERROR, mContext.getString(R.string.device_get_amount_cancel));
                    } else {
//                        onError(ERRORS.DEVICE_GET_PIN_ERROR, ERRORS.DEVICE_GET_PIN_CANCEL);
                        onError(ERRORS.DEVICE_GET_PIN_ERROR, mContext.getString(R.string.device_get_pin_cancel));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_TIME_OUT)) {
                    if (inputInfoEntity.getInputType() == INPUT_AMOUNT) {
//                        onError(ERRORS.DEVICE_INPUT_INFO_ERROR, ERRORS.DEVICE_GET_AMOUNT_TIMEOUT);
                        onError(ERRORS.DEVICE_INPUT_INFO_ERROR, mContext.getString(R.string.device_get_amount_timeout));
                    } else {
//                        onError(ERRORS.DEVICE_GET_PIN_ERROR, ERRORS.DEVICE_GET_PIN_TIMEOUT);
                        onError(ERRORS.DEVICE_GET_PIN_ERROR, mContext.getString(R.string.device_get_pin_timeout));
                    }
                    return;
                } else if (TextUtils.equals(errorCode, SDKException.ERR_CODE_RESET)) {
                    //nothing
                    return;
                }
            }

            if (inputInfoEntity.getInputType() == INPUT_AMOUNT) {
//                onError(ERRORS.DEVICE_INPUT_INFO_ERROR, ERRORS.DEVICE_INPUT_INFO_ERROR_DESC);
                onError(ERRORS.DEVICE_INPUT_INFO_ERROR, mContext.getString(R.string.device_input_info_error_desc));
            } else {
//                onError(ERRORS.DEVICE_GET_PIN_ERROR, ERRORS.DEVICE_GET_PIN_ERROR_DESC);
                onError(ERRORS.DEVICE_GET_PIN_ERROR, mContext.getString(R.string.device_get_pin_error_desc));
            }
        }
    }

    @Override
    public void icCardWriteback(final EMVOnlineData emvOnlineData) {
        try {
            Map<String, String> reqMap = TlvUtil.tlvToMap(emvOnlineData.getOnlineData());

            byte[] data = mapToTlv(reqMap);
            LogUtil.d("getOnlineData:" + StringUtil.byte2HexStr(data));
            LogUtil.d("getResponseCode:" + emvOnlineData.getResponseCode());
            int code = Integer.parseInt(emvOnlineData.getResponseCode());
            String strRespCode = String.format("%02d", code);
            byte[] respCode = strRespCode.getBytes();
            byte[] result = Command.twiceAuthorization(respCode, data, 0);
            if (result != null) {
                if (mListener != null) {
                    mListener.onGetICCardWriteback(true);
                }
            } else {
                if (mListener != null) {
                    mListener.onGetICCardWriteback(false);
                }
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_IC_CARD_ONLINE_PROCESS_ERROR,
//                    ERRORS.DEVICE_IC_CARD_ONLINE_PROCESS_ERROR_DESC);
            onError(ERRORS.DEVICE_IC_CARD_ONLINE_PROCESS_ERROR,
                    mContext.getString(R.string.device_ic_card_online_process_error_desc));
        }

    }

    @Override
    public void cancelTrade() {
        try {
            boolean result = Command.reset();
            if (result) {
                if (mListener != null) {
                    mListener.onCancelReadCard();
                }
            } else {
//                onError(ERRORS.DEVICE_CANCEL_ERORR,
//                        ERRORS.DEVICE_CANCEL_ERORR_DESC);
                onError(ERRORS.DEVICE_CANCEL_ERORR,
                        mContext.getString(R.string.device_cancel_erorr_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_CANCEL_ERORR,
//                        ERRORS.DEVICE_CANCEL_ERORR_DESC);
            onError(ERRORS.DEVICE_CANCEL_ERORR,
                    mContext.getString(R.string.device_cancel_erorr_desc));
        }
    }

    @Override
    public void calculateMac(final String macData) {
        try {
            byte keyIndex = KeyType.MAC;
            byte keyType = 0;
            byte MACType = 2;
            //1 for 16 byte data CBC
            //2 for X9.19
            byte[] data = StringUtil.str2BCD(macData);
            LogUtil.d("d:" + StringUtil.byte2HexStr(data));
            CalMacResponse result = Command.calMAC(keyIndex, keyType, MACType, data);
            if (result != null) {
                if (mListener != null) {
                    mListener.onGetCalcMacResult(result.getMAC());
                }
            } else {
//                onError(ERRORS.DEVICE_CALC_MAC_ERROR,
//                        ERRORS.DEVICE_CALC_MAC_ERROR_DESC);
                onError(ERRORS.DEVICE_CALC_MAC_ERROR,
                        mContext.getString(R.string.device_calc_mac_error_desc));
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            //                onError(ERRORS.DEVICE_CALC_MAC_ERROR,
//                        ERRORS.DEVICE_CALC_MAC_ERROR_DESC);
            onError(ERRORS.DEVICE_CALC_MAC_ERROR,
                    mContext.getString(R.string.device_calc_mac_error_desc));
        }
    }

    @Override
    public void updateFirmware(final String filePath) {
        InputStream is;
        try {
            is = new FileInputStream(new File(filePath));
        } catch (Exception e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR, ConstantStr.Error.NOT_OPEN_FILE);
            onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR,mContext.getString(R.string.not_open_file));
            return;
        }

        byte mode = (byte) 0;
        int i = 0;
        while (i < 3) {
            try {
                mode = Command.interUpdate();
                if (mode != (byte) 3) {
                    break;
                } else {
                    i++;
                }
            } catch (Exception e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
//                onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR, ConstantStr.Error.ENTER_UPDATE_FAIL);
                onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR,mContext.getString(R.string.enter_update_fail));
                return;
            }
        }

        if (mode != (byte) 1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e2) {
                if (LogUtil.DEBUG) {
                    e2.printStackTrace();
                }
            }

            DownloadFile downloadFile = new DownloadFile();
            downloadFile.updateApp(is, (byte) 3, mUpdateCallBackInterface);
            return;
        } else {
//            onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR, ConstantStr.Error.ENTER_UPDATE_FAIL);
            onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR,mContext.getString(R.string.enter_update_fail));
        }
    }

    private UpdateCallBackInterface mUpdateCallBackInterface = new UpdateCallBackInterface() {

        @Override
        public void onDownloadComplete() {
            if (mListener != null) {
                mListener.onUpdateFirmwareSuccess();
            }
        }

        @Override
        public void onDownloadFail(int error, String message) {
            onError(ERRORS.DEVICE_UPDATE_FIRMWARE_ERROR, message);
        }

        @Override
        public void onDownloadProgress(int download, int total) {
            if (mListener != null) {
                try {
                    if (total > 0) {
                        float percent = (float) download / total;
                        DecimalFormat decimalFormat = new DecimalFormat(".00");
                        String strPercent = decimalFormat.format(percent);
                        percent = Float.parseFloat(strPercent);
                        mListener.onUpdateFirmwareProcess(percent);
                    }
                } catch (Exception e) {
                    if (LogUtil.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private boolean judgeIsUpdate(int threshold) {
        try {
            byte[] battery = Command.getBatteryAndState();
            if (battery == null) {
                return false;
            }
            if (battery[2] == (byte) 1) {
                return true;
            }
            if (Integer.valueOf(StringUtil.byteToStr(new byte[]{battery[0], battery[1]})).intValue() >= threshold) {
                return true;
            }
            return false;
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    public void generateQRCode(final int keepShowTime, final String content) {
        final int time;
        if (keepShowTime <= 0) {
            time = 60;
        } else {
            time = keepShowTime;
        }

        try {
            byte[] x = HexUtil.lenTo2Hex(0);
            byte[] y = HexUtil.lenTo2Hex(0);
            byte[] width = HexUtil.lenTo2Hex(64);

            Command.showQRCode(x, y, HexUtil.lenTo2Hex(64), width,
                    (byte) time, ConstantStr.Tips.PLS_SCAN.getBytes("GBK"),
                    QRCodeUtil.QRbitMatrix(content, 64, 64));

            if (mListener != null) {
                mListener.onGenerateQRCodeSuccess();
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_GENERATE_QRCODE_ERROR,
//                    ERRORS.DEVICE_GENERATE_QRCODE_ERROR_DESC);
            onError(ERRORS.DEVICE_GENERATE_QRCODE_ERROR,
                    mContext.getString(R.string.device_generate_qrcode_error_desc));
        }
    }

//    public boolean initFile() {
//        byte[] zero = new byte[2];
//        byte[] recordNameBytes = StringUtil.str2bytesGBK("TRACE");
//        byte[] recordNoBytes = StringUtil.intToBytes2(1);
//        byte[] recordLen = StringUtil.lenToLLVAR(12);
//
//        try {
//            Command.initRecords(recordNameBytes, recordLen, zero, zero, zero, zero);
//            return true;
//        } catch (SDKException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    @Override
    public void setTransactionInfo(final String flowNumber) {
        try {
            byte[] zero = new byte[2];
            byte[] recordNameBytes = StringUtil.str2bytesGBK("TRACE");
            byte[] recordNoBytes = StringUtil.intToBytes2(1);

            Command.updateRecord(recordNameBytes, recordNoBytes,
                    zero, zero, flowNumber.getBytes());
            if (mListener != null) {
                mListener.onSetTransactionInfoSuccess();
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof SDKException &&
                    !TextUtils.equals(SDKException.ERR_CODE_COMMUNICATE_ERROR, ((SDKException) e).getErrCode())) {
                try {
                    byte[] zero = new byte[2];
                    byte[] recordNameBytes = StringUtil.str2bytesGBK("TRACE");
                    byte[] recordLen = StringUtil.lenToLLVAR(flowNumber.getBytes().length);
                    Command.initRecords(recordNameBytes, recordLen, zero, zero, zero, zero);
                    Command.addRecord(recordNameBytes, flowNumber.getBytes());
                } catch (Throwable e1) {
                    if (LogUtil.DEBUG) {
                        e1.printStackTrace();
                    }
//                    onError(ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR,
//                            ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR_DESC);
                    onError(ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR,
                            mContext.getString(R.string.device_save_trade_data_error_desc));
                }
            } else {
                //                    onError(ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR,
//                            ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR_DESC);
                onError(ERRORS.DEVICE_SAVE_TRADE_DATA_ERROR,
                        mContext.getString(R.string.device_save_trade_data_error_desc));
            }
        }
    }

    @Override
    public void getTransactionInfo() {
        try {
            byte[] zero = new byte[2];
            byte[] recordNameBytes = StringUtil.str2bytesGBK("TRACE");
            byte[] recordNoBytes = StringUtil.intToBytes2(1);

            byte[] result = Command.readRecord(recordNameBytes, recordNoBytes,
                    zero, zero);
            String trace = StringUtil.bytes2Ascii(result);

            if (mListener != null) {
                mListener.onGetTransactionInfoSuccess(trace);
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_GET_TRADE_DATA_ERROR,
//                    ERRORS.DEVICE_GET_TRADE_DATA_ERROR_DESC);
            onError(ERRORS.DEVICE_GET_TRADE_DATA_ERROR,
                    mContext.getString(R.string.device_get_trade_data_error_desc));
        }
    }

    @Override
    public void displayTextOnScreen(int keepShowTime, String content) {
        try {
            Command.showStrScreen(content.toString().getBytes("GBK"), keepShowTime);
            if (mListener != null) {
                mListener.onDisplayTextOnScreenSuccess();
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
//            onError(ERRORS.DEVICE_SHOW_TEXT_ERROR,
//                    ERRORS.DEVICE_SHOW_TEXT_ERROR_DESC);
            onError(ERRORS.DEVICE_SHOW_TEXT_ERROR,
                    mContext.getString(R.string.device_show_text_error_desc));
        }
    }

    private void onError(int error, String desc) {
        try {
            if (mListener != null) {
                LogUtil.d("error:" + error + " " + desc);
                mListener.onReceiveErrorCode(error, desc);
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private long mSetTime;

    private void setTimeBl(final String setTime) {
        try {
            Command.setDeviceTime(setTime);
            isSetTime = true;
            mSetTime = SystemClock.uptimeMillis();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private void checkEmvParams() {
        try {
            //already check emv params
            if (EmvParams.haveCheckParams) {
                return;
            }

            byte[] result = Command.checkEmvFile();
            //CAPK
            if (result[0] == 0) {
                for (String d : EmvParams.PublicKeyList()) {
                    if (!Command.operatePubicKey((byte) 2, HexUtil.toBCD(d))) {
                        throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                    }
                }
            }

            //AID
            if (result[1] == 0) {
                for (String d : EmvParams.AIDList()) {
                    if (!Command.operateAID((byte) 2, HexUtil.toBCD(d))) {
                        throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                    }
                }
            }

            if (!EmvParams.haveCheckParams) {
                EmvParams.haveCheckParams = true;
                beforeAddr = mMacAddr;
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private volatile boolean isSetTime;
    private static final long SET_TIME_PERIOD = 30 * 60 * 1000;

    private synchronized void syncTime() {
        if (!isSetTime || (SystemClock.uptimeMillis() - mSetTime) > SET_TIME_PERIOD)
        {
            // when sign in update device time
            setTimeBl(new SimpleDateFormat(DEFAULT_DATEPATTERN).format(new Date()));
        }
    }

    private void downloadAidOnly() {
        try {
            int currentVersion = mAidVersion.getVersion();
            String sn = mAidVersion.getSn();
            if (mTerminalInfo == null || mTerminalInfo.getStrSNCode() == null) {
                mTerminalInfo = Command.getTerminalInfo();
            }

            if (currentVersion != EmvParams.EMV_PARAM_VER || !TextUtils.equals(sn, mTerminalInfo.getStrSNCode())) {
                for (String d : EmvParams.AIDList()) {
                    if (!Command.operateAID((byte) 2, HexUtil.toBCD(d))) {
                        throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                    }
                }
                mAidVersion.setSn(mTerminalInfo.getStrSNCode());
                mAidVersion.setVersion(EmvParams.EMV_PARAM_VER);
                FileUtils.setAidVersion(mContext, mAidVersion);
            } else {
                //LogUtil.d("already download");
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
    }

}
