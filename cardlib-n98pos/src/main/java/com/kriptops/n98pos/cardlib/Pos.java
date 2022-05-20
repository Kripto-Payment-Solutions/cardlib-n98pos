package com.kriptops.n98pos.cardlib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kriptops.n98pos.cardlib.android.PosApp;
import com.kriptops.n98pos.cardlib.crypto.FitMode;
import com.kriptops.n98pos.cardlib.crypto.PaddingMode;
import com.kriptops.n98pos.cardlib.db.MapIVController;
import com.kriptops.n98pos.cardlib.func.BiConsumer;
import com.kriptops.n98pos.cardlib.func.Consumer;
import com.kriptops.n98pos.cardlib.model.ResponsePos;
import com.kriptops.n98pos.cardlib.tools.Util;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.CardReadEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.InputInfoEntity;
import com.newpos.mposlib.sdk.NpPosManager;
import com.newpos.mposlib.util.StringUtil;

import java.util.List;
import java.util.Map;

public class Pos {
    //TODO migrate to new infrastructure
    private final Emv emv;
    private final PinpadNPos pinpadNPos;

    private final PosOptions posOptions;

    private boolean pinpadCustomUI;
    private Runnable onPinRequested;
    private Runnable onPinCaptured;
    private BiConsumer<String, String> onError;
    private BiConsumer<String, String> onWarning;
    private BiConsumer<String, ResponsePos> onSuccess;

    private Consumer<Integer> digitsListener;
    private Consumer<TransactionData> goOnline;
    protected TransactionData data;
    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    private NpPosManager posManager;
    private PosApp posApp;

    final class TradeType {
        public final static int SALE = 0;
        public final static int VOID = 1;
        public final static int GET_CARD_NUMBER = 2;
    }

    private INpSwipeListener mNpSwipeListener = new INpSwipeListener() {
        @Override
        public void onScannerResult(BluetoothDevice devInfo) {
        }

        @Override
        public void onDeviceConnected() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Context context = posApp.getApplicationContext();
                    String messageShow = context.getText(R.string.device_connect_success).toString();

                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onDeviceConnected");
                    response.setMessage(messageShow);
                    raiseSuccess("onDeviceConnected", response);
                }
            });
        }

        @Override
        public void onDeviceDisConnected() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Context context = posApp.getApplicationContext();
                    String messageShow = context.getText(R.string.device_disconnect).toString();

                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onDeviceDisConnected");
                    response.setMessage(messageShow);
                    raiseSuccess("onDeviceDisConnected", response);

                }
            });
        }

        @Override
        public void onGetDeviceInfo(DeviceInfoEntity info) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Context context = posApp.getApplicationContext();
                    String messageShow = context.getText(R.string.get_device_info).toString();

                    ResponsePos<DeviceInfoEntity> response = new ResponsePos<DeviceInfoEntity>();
                    response.setNameEvent("onGetDeviceInfo");
                    response.setMessage(messageShow);
                    response.setObjResp(info);
                    raiseSuccess("onGetDeviceInfo", response);
                }
            });
        }

        @Override
        public void onGetTransportSessionKey(String encryTransportKey) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetTransportSessionKey");
                    response.setEncryTransportKey(encryTransportKey);
                    raiseSuccess("onGetTransportSessionKey", response);
                }
            });
        }

        @Override
        public void onUpdateMasterKeySuccess() {
            Context context = posApp.getApplicationContext();
            ResponsePos response = new ResponsePos();
            response.setNameEvent("onUpdateMasterKeySuccess");
            response.setMessage(context.getText(R.string.update_master_key_success).toString());
            raiseSuccess("onUpdateMasterKeySuccess", response);
        }

        @Override
        public void onUpdateWorkingKeySuccess() {
            Context context = posApp.getApplicationContext();
            ResponsePos response = new ResponsePos();
            response.setNameEvent("onUpdateWorkingKeySuccess");
            response.setMessage(context.getText(R.string.update_working_key_success).toString());
            raiseSuccess("onUpdateWorkingKeySuccess", response);
        }

        @Override
        public void onAddAidSuccess() {
            Context context = posApp.getApplicationContext();
            ResponsePos response = new ResponsePos();
            response.setNameEvent("onAddAidSuccess");
            response.setMessage(context.getText(R.string.add_aid_success).toString());
            raiseSuccess("onAddAidSuccess", response);
        }

        @Override
        public void onAddRidSuccess() {
            Context context = posApp.getApplicationContext();
            ResponsePos response = new ResponsePos();
            response.setNameEvent("onAddRidSuccess");
            response.setMessage(context.getText(R.string.add_rid_success).toString());
            raiseSuccess("onAddRidSuccess", response);
        }

        @Override
        public void onClearAids() {

        }

        @Override
        public void onClearRids() {

        }

        @Override
        public void onGetCardNumber(String cardNum) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetCardNumber");
                    response.setCardNum(cardNum);

                    raiseSuccess("onGetCardNumber", response);

                }
            });
        }

        @Override
        public void onGetDeviceBattery(boolean result) {

        }

        @Override
        public void onDetachedIC() {

        }

        @Override
        public void onGetReadCardInfo(CardInfoEntity cardInfoEntity) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos<CardInfoEntity> response = new ResponsePos<CardInfoEntity>();
                    response.setNameEvent("onGetReadCardInfo");
                    response.setObjResp(cardInfoEntity);

                    readAppData(cardInfoEntity);

                    raiseSuccess("onGetReadCardInfo", response);

                }
            });
        }

        @Override
        public void onGetReadInputInfo(String inputInfo) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetReadInputInfo");
                    response.setInputInfo(inputInfo);

                    raiseSuccess("onGetReadInputInfo", response);

                }
            });
        }

        @Override
        public void onGetICCardWriteback(boolean result) {

        }

        @Override
        public void onCancelReadCard() {
            Context context = posApp.getApplicationContext();
            ResponsePos response = new ResponsePos();
            response.setNameEvent("onCancelReadCard");
            response.setMessage(context.getText(R.string.cancel_trade).toString());
            raiseSuccess("onCancelReadCard", response);
        }

        @Override
        public void onGetCalcMacResult(String encryMacData) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetCalcMacResult");
                    response.setEncryMacData(encryMacData);

                    raiseSuccess("onGetCalcMacResult", response);

                }
            });
        }

        @Override
        public void onUpdateFirmwareProcess(float percent) {

        }

        @Override
        public void onUpdateFirmwareSuccess() {

        }

        @Override
        public void onGenerateQRCodeSuccess() {

        }

        @Override
        public void onSetTransactionInfoSuccess() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onSetTransactionInfoSuccess");
                    response.setMessage("onSetTransactionInfoSuccess");

                    raiseSuccess("onSetTransactionInfoSuccess", response);

                }
            });
        }

        @Override
        public void onGetTransactionInfoSuccess(String transactionInfo) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetTransactionInfoSuccess");
                    response.setTransactionInfo(transactionInfo);

                    raiseSuccess("onGetTransactionInfoSuccess", response);

                }
            });
        }

        @Override
        public void onDisplayTextOnScreenSuccess() {

        }

        @Override
        public void onReceiveErrorCode(int error, String message) {
            System.out.println("onReceiverErrorCode custom listener");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d("Pos","onReceiveErrorCode()");
                    raiseError("onReceiveErrorCode", message);
                }
            });
        }

        /**
         * 数据加密回调
         * **/
        @Override
        public void onGetEncryptData(String eData){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onGetEncryptData");
                    response.setEncryptData(eData);

                    raiseSuccess("onGetEncryptData", response);

                }
            });
        };

        /**
         * 显示信息
         * **/
        @Override
        public void onDispMsgOnScreen(String disp){
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    ResponsePos response = new ResponsePos();
                    response.setNameEvent("onDispMsgOnScreen");
                    response.setMessage(disp);

                    raiseSuccess("onDispMsgOnScreen", response);

                }
            });
        };
    };

    public Pos(PosApp posApp) {
        this(posApp, new PosOptions());
    }

    public Pos(PosApp posApp, PosOptions posOptions) {
        if (posOptions == null) {
            throw new IllegalArgumentException("posOptions is null" );
        }

        this.posApp = posApp;
        // inicializa el manejador de vectores de inicializacion
        this.posOptions = new PosOptions();
        this.posOptions.setIvController(Util.nvl(posOptions.getIvController(), new MapIVController()));
        this.posOptions.setTrack2FitMode(Util.nvl(posOptions.getTrack2FitMode(), FitMode.F_FIT));
        this.posOptions.setTrack2PaddingMode(Util.nvl(posOptions.getTrack2PaddingMode(), PaddingMode.PKCS5));
        this.posOptions.setIccTaglist(Util.nvl(posOptions.getIccTaglist(), Defaults.DEFAULT_ICC_TAGLIST));
        this.posOptions.setNfcTagList(Util.nvl(posOptions.getNfcTagList(), Defaults.DEFAULT_NFC_TAGLIST));
        this.posOptions.setAidTables(Util.nvl(posOptions.getAidTables(), Defaults.AID_TABLES));
        this.posOptions.setMsrBinWhitelist(Util.nvl(posOptions.getMsrBinWhitelist(), Defaults.BIN_MSR_WHITELIST));
        this.posOptions.setMsrBinWhitelistSupplier(Util.nvl(posOptions.getMsrBinWhitelistSupplier(), Defaults.BIN_MSR_WHITELIST_SUPPLIER));

        //debe ir antes que la creacion del emv kernel
        this.emv = null; //new Emv(this, posApp.getApplicationContext());

        this.pinpadNPos = new PinpadNPos(this.posOptions.getIvController());

        this.posOptions.getIvController().saveIv(this.pinpadNPos.IV_DATA, this.pinpadNPos.DEFAULT_IV);
        this.posOptions.getIvController().saveIv(this.pinpadNPos.IV_PIN, this.pinpadNPos.DEFAULT_IV);
        this.pinpadNPos.setIvController(this.posOptions.getIvController());

        //Instanciamos el posManager(N98Pos)
        posManager = NpPosManager.sharedInstance(posApp.getApplicationContext(), mNpSwipeListener);
    }

    public void beep() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    public void setPinLength(int minLen, int maxLen) {
        //this.pinpad.setPinLength(minLen, maxLen);
    }

    public void setPinLength(int lenPin) {
        //this.pinpad.setPinLength(lenPin, lenPin);
    }

    public String getSerialNumber() {
        //return this.terminal.getSerialNumber();
        return "";
    }

    public void configTerminal(
            String merchantId,
            String merchantName,
            String terminalId,
            String clFloorLimit,
            String clTransactionLimit,
            String cvmLimit
    ) {
        this.emv.initParams(
                merchantId,
                merchantName,
                terminalId,
                getSerialNumber(),
                clFloorLimit,
                clTransactionLimit,
                cvmLimit
        );
    }

    public void connectBTDevice(String macAddressN98){
        posManager.connectBluetoothDevice(macAddressN98);
    }

    public void disConnectDevice(){
        posManager.disconnectDevice();
    }

    public void configTerminal(EMVConfig config) {
        this.emv.configTerminal(config);
    }

    protected void processOnline() {
        // Log.d(Defaults.LOG_TAG, data.toString());
        int panSize = data.maskedPan.length();
        int las4index = panSize - 4;
        data.bin = data.maskedPan.substring(0, 6);
        data.maskedPan = "*******************".substring(0, las4index) + data.maskedPan.substring(las4index, panSize);
        /*
        withPinpad((p) -> {
            if (data.track2 != null) {
                //Pure unmodified track2
                if (data.track2.endsWith("F" )) {
                    data.track2 = data.track2.substring(0, data.track2.length() - 1);
                }
                // track2 clear no longer needed
                // data.track2Clear = data.track2;
            }
            data.track2 = this.posOptions.getTrack2FitMode().fit(data.track2);
            data.track2 = this.posOptions.getTrack2PaddingMode().pad(data.track2);
            data.track2 = this.pinpad.encryptHex(data.track2);
        });
        // Log.d(Defaults.LOG_TAG, data.toString());
        //TODO elevar a otro handler de nivel aun mas superior
        if (goOnline != null) {
            goOnline.accept(data);
        } else {
            raiseError("pos", "online_handler_null" );
        }
        */

    }

    public void setPinpadTimeout(int timeout) {
        //this.pinpad.setTimeout(timeout);
    }

    public void setTagList(int[] tagList) {
        this.emv.setTaglist(tagList);
    }

    public void setTagAIDs() {
        if(posManager.isConnected()) {
            for (String aid : Defaults.AIDS) {
                this.posManager.addAid(aid);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("not_set_aids", messageShow);
        }
    }

    public void clearAIDS(){
        posManager.clearAids();
    }

    private void readAppData(CardInfoEntity cardInfoEntity) {
        TransactionData data = new TransactionData();
        data.track2 = cardInfoEntity.getTrack2();
        if (cardInfoEntity.getTrack2() != null) {
            //Pure unmodified track2
            if (data.track2.endsWith("F" )) {
                data.track2 = data.track2.substring(0, data.track2.length() - 1);
            }
        }
        data.maskedPan = cardInfoEntity.getTrack2().split("[D=]")[0];
        data.track2 = this.posOptions.getTrack2FitMode().fit(data.track2);
        data.track2 = this.posOptions.getTrack2PaddingMode().pad(data.track2);
        data.bin = data.maskedPan.substring(0, 6);

        if(cardInfoEntity.getCaptureType() == "msr") {
            data.track2 = data.track2.replace('=', 'D');
            byte[] dataB = StringUtil.str2BCD(data.track2);
            data.track2 = StringUtil.byte2HexStr(dataB);
        }

        cardInfoEntity.setTrack2(data.track2);
        cardInfoEntity.setBin(data.bin);

        if(cardInfoEntity.getCaptureType() != "msr") {
            cardInfoEntity.setEmv(getEmvData(cardInfoEntity.getDataMap(), cardInfoEntity.getCaptureType() == "icc"));
        }
        //Encriptando el track2 en CBC
        String inCBC = getPosManager().EncryptDataCBC(data.track2);
        cardInfoEntity.setEncryptTrack2(inCBC);

        //data.maskedPan = Util.nvl(data.maskedPan, () -> this.readTag(0x5a));
        //data.track2Clear = Util.nvl(data.track2Clear, () -> this.readTag(0x57));
        //data.track2 = data.track2Clear;
        /*
        data.panSequenceNumber = Util.nvl(data.panSequenceNumber, () -> this.readTag(0x5f34));
        data.expiry = Util.nvl(data.expiry, () -> this.readTag(0x5f24));
        data.aid = Util.nvl(data.aid, () -> this.readTag(0x84));
        data.ecBalance = Util.nvl(data.ecBalance, () -> this.readTag(0x9f79));
        if (data.maskedPan == null) {
            data.maskedPan = data.track2.split("D")[0];
        }
        */
    }

    /**
     * Realiza una operacion atomica con el PosManager preservando el estado anterior.
     *
     * @param consumer
     */
    public void withPosManager(Consumer<NpPosManager> consumer) {
        boolean wasConnectDevice = this.getPosManager().isConnected();
        try {
            if (!wasConnectDevice) {
                //enviar mensaje de conectar al dispositivo bluetooh
            }
            consumer.accept(this.getPosManager());
        } finally {
            if (!wasConnectDevice) {

            }
        }
    }

    public void loadAidparam(List<String> lstAIDS){
        if(posManager.isConnected()) {
            for(String str : lstAIDS){
                posManager.addAid(str);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    public void loadCapkparam(List<String> lstCAPKS){
        if(posManager.isConnected()) {
            for(String str : lstCAPKS){
                posManager.addRid(str);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    public void clearAID_RID(){
        posManager.clearAids();
        posManager.clearRids();
    }

    public void clearAids(){
        posManager.clearAids();
    }

    public void clearRids(){
        posManager.clearRids();
    }

    /**
     * Inicia una transaccion.
     *
     * @param date     fecha en formato YYMM
     * @param time     hora en formato HHMMSS
     * @param tsc      contador de transaccion
     * @param amount   monto en formato ex2, por ejemplo 1000 representa 10.00
     * @param cashback indica si es una reversa
     */
    public void beginTransaction(String date, String time, String tsc, String amount, String currency, boolean cashback) {
        if(posManager.isConnected()) {
            CardReadEntity cardReadEntitys =  new CardReadEntity();
            cardReadEntitys.setSupportFallback(false);
            cardReadEntitys.setTimeout(30);
            cardReadEntitys.setAmount(amount); //"000000080000"
            //cardReadEntitys.setAmount(amount);
            //0x01 mag 0x02 icc  0x04 nfc
            //cardReadEntitys.setReadCardType(0x04);
            //cardReadEntitys.setReadCardType(0x01 | 0x02 );
            cardReadEntitys.setReadCardType(0x01 | 0x02 | 0x04);
            //cardReadEntitys.setReadCardType(0x01);
            cardReadEntitys.setCurrency(currency); // 9F1A Transaction Country Code -> 0604 default soles
            cardReadEntitys.setTerminalCoutryCode("0604");  //5f2a Terminal Country Code -> 0604 default peru
            cardReadEntitys.setTradeType(TradeType.SALE);
            posManager.readCard(cardReadEntitys);
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    /**
     * Inicia una transaccion.
     *
     * @param date   fecha en formato YYMM
     * @param time   hora en formato HHMMSS
     * @param tsc    contador de transaccion
     * @param amount monto en formato ex2, por ejemplo 1000 representa 10.00
     */
    public void beginReadCard(String date, String time, String tsc, String amount, String currency) {
        if(posManager.isConnected()) {
            CardReadEntity cardReadEntitys =  new CardReadEntity();
            cardReadEntitys.setSupportFallback(false);
            cardReadEntitys.setTimeout(30);
            cardReadEntitys.setAmount(amount); //"000000080000"
            //cardReadEntitys.setAmount(amount);
            //0x01 mag 0x02 icc  0x04 nfc
            //cardReadEntitys.setReadCardType(0x04);
            //cardReadEntitys.setReadCardType(0x01 | 0x02 );
            cardReadEntitys.setReadCardType(0x01 | 0x02 | 0x04);
            //cardReadEntitys.setReadCardType(0x01);
            cardReadEntitys.setCurrency(currency); // 9F1A Transaction Country Code -> 0604 default soles
            cardReadEntitys.setTerminalCoutryCode("0604");  //5f2a Terminal Country Code -> 0604 default peru
            cardReadEntitys.setTradeType(TradeType.GET_CARD_NUMBER);
            posManager.readCard(cardReadEntitys);
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    /**
     * Inicia una transaccion.
     *
     * @param date   fecha en formato YYMM
     * @param time   hora en formato HHMMSS
     * @param tsc    contador de transaccion
     * @param amount monto en formato ex2, por ejemplo 1000 representa 10.00
     */
    public void beginTransaction(String date, String time, String tsc, String amount, String currency) {
        this.beginTransaction(date, time, tsc, amount, currency, false);
    }

    public void cancelTransaction(){
        if(posManager.isConnected()) {
            posManager.cancelTrade();
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    public void inputAmount(){
        if(posManager.isConnected()) {
            InputInfoEntity inputInfoEntity = new InputInfoEntity();
            inputInfoEntity.setInputType(0);
            inputInfoEntity.setTimeout(30);
            posManager.getInputInfoFromKB(inputInfoEntity);
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    public void inputPin(String pan){
        if(posManager.isConnected()) {
            InputInfoEntity inputInfoEntityd = new InputInfoEntity();
            inputInfoEntityd.setInputType(1);
            inputInfoEntityd.setTimeout(35);
            inputInfoEntityd.setPan(pan);//6226220633452981    "4779042200107095"
            posManager.getInputInfoFromKB(inputInfoEntityd);
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            raiseWarning("device_not_connect", messageShow);
        }
    }

    /// para el control del pinpad
    /**
     * Accede al uso del pinpad y coordina su ingreso con funciones del pos
     *
     * @param pan
     */
    private void waitForPinpad(String pan) {
//        pinpad.open();
//
//        if (!pinpad.listenForPinBlock(pan, this::pinpadEventResolved, this.digitsListener)) {
//            onError.accept("pin", "startFailed" );
//            pinpad.close();
//        }
    }

    /**
     * Se usa para iniciar el proceso de pedido de pin.
     */
    public void callPin() {
        String pan = this.data.maskedPan;
        this.waitForPinpad(pan);
    }

    protected void requestPinToUser() {
        if (!this.isPinpadCustomUI()) {
            raiseError("pin", "custom_ui_false" );
        } else if (this.onPinRequested == null) {
            raiseError("pin", "request_handler_null" );
        } else {
            this.onPinRequested.run();
        }
    }

    /**
     * Configura el evento de escucha cuando se ha requerido el pin.
     *
     * @param onPinRequested
     */
    public void setOnPinRequested(Runnable onPinRequested) {
        this.onPinRequested = onPinRequested;
    }

    /**
     * Configura el evento de escucha cuando se ha capturado el pin.
     *
     * @param onPinCaptured
     */
    public void setOnPinCaptured(Runnable onPinCaptured) {
        this.onPinCaptured = onPinCaptured;
    }

    /**
     * Configura el evento de escucha cuando se ha generado un error.
     *
     * @param onError
     */
    public void setOnError(BiConsumer<String, String> onError) {
        this.onError = onError;
    }

    /**
     * Configura el evento de escucha cuando un evento a culminado correctamente.
     *
     * @param onSuccess
     */
    public void setOnSuccess(BiConsumer<String, ResponsePos> onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Configura el evento de escucha cuando se ha producido una alerta en un proceso.
     *
     * @param onWarning
     */
    public void setOnWarning(BiConsumer<String, String> onWarning) {
        this.onWarning = onWarning;
    }

    protected void raiseError(String source, String payload) {
        // Log.d(Defaults.LOG_TAG, "error: " + source + " " + payload);
        if (this.onError != null) onError.accept(source, payload);
    }

    protected void raiseWarning(String source, String payload) {
        // Log.d(Defaults.LOG_TAG, "warning: " + source + " " + payload);
        if (this.onWarning != null) onWarning.accept(source, payload);
    }

    protected void raiseSuccess(String source, ResponsePos payload) {
        // Log.d(Defaults.LOG_TAG, "Success: " + source + " " + payload);
        if (this.onSuccess != null) onSuccess.accept(source, payload);
    }

    protected boolean isPinpadCustomUI() {
        return pinpadCustomUI;
    }

    /**
     * Indica que se usara un customUI para controlar el background del pin.
     *
     * @param pinpadCustomUI
     */
    public void setPinpadCustomUI(boolean pinpadCustomUI) {
        this.pinpadCustomUI = pinpadCustomUI;
    }

    /**
     * Configura el escucha de la cantidad de digitos ingresados del pin.
     *
     * @param digitsListener
     */
    public void setDigitsListener(Consumer<Integer> digitsListener) {
        this.digitsListener = digitsListener;
    }

    /**
     * Envia la respuesta para poder iniciar el proceso en linea.
     *
     * @param goOnline
     */
    public void setGoOnline(Consumer<TransactionData> goOnline) {
        this.goOnline = goOnline;
    }

    public PosOptions getPosOptions() {
        return posOptions;
    }

    public NpPosManager getPosManager() {
        return this.posManager;
    }

    public String getEmvData(Map<String, String> dataMap, boolean icc) {
        String[] tags = icc ? Defaults.ICC_TAGLIST : Defaults.NFC_TAGLIST;
        String data = "";
        for (String tag : tags) {
            String value = dataMap.get(tag);
            if ("5A".equals(tag)) {
                while (value.endsWith("FF")) {
                    value = value.substring(0, value.length() - 2);
                }
            } else if ("9F40".equals(tag)) {
                if (value == null) {
                    value = "FF80F0A001";
                }
            }
            if (value == null) {
                continue;
            }
            String len = "00" + Integer.toString(value.length() / 2, 16);
            len = len.substring(len.length() - 2);
            data += (tag + len + value);
        }
        return data;
    }
}