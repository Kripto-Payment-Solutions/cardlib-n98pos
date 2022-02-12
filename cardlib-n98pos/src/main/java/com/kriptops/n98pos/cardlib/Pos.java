package com.kriptops.n98pos.cardlib;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cloudpos.DeviceException;
import com.cloudpos.OperationResult;
import com.cloudpos.jniinterface.EMVJNIInterface;
import com.cloudpos.pinpad.PINPadOperationResult;
import com.cloudpos.printer.PrinterDevice;
import com.kriptops.n98pos.cardlib.android.BluetoothApp;
import com.kriptops.n98pos.cardlib.android.NpSwipe;
import com.kriptops.n98pos.cardlib.android.PosApp;
import com.kriptops.n98pos.cardlib.bridge.Terminal;
import com.kriptops.n98pos.cardlib.constant.Constant;
import com.kriptops.n98pos.cardlib.crypto.FitMode;
import com.kriptops.n98pos.cardlib.crypto.PaddingMode;
import com.kriptops.n98pos.cardlib.db.MapIVController;
import com.kriptops.n98pos.cardlib.func.BiConsumer;
import com.kriptops.n98pos.cardlib.func.Consumer;
import com.kriptops.n98pos.cardlib.tools.Util;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.CardReadEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.EMVOnlineData;
import com.newpos.mposlib.sdk.INpPosControler;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.InputInfoEntity;
import com.newpos.mposlib.sdk.NpPosManager;

import java.lang.ref.WeakReference;

public class Pos implements INpSwipeListener {

    private final Terminal terminal;

    //TODO migrate to new infrastructure
    private final Emv emv;
    private final Pinpad pinpad;
    private final Msr msr;

    private final PosOptions posOptions;

    private boolean pinpadCustomUI;
    private Runnable onPinRequested;
    private Runnable onPinCaptured;
    private BiConsumer<String, String> onError;
    private BiConsumer<String, String> onWarning;
    private Consumer<Integer> digitsListener;
    private Consumer<TransactionData> goOnline;
    protected TransactionData data;
    private ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    protected ClientMessengerHandler mHandler;
    protected void handleMessageClient(Message msg){}

    private NpPosManager posManager;
    private PosApp posApp;

//    private INpPosControler mINpPosManager = new INpPosControler() {
//        @Override
//        public void scanBlueDevice(int timeOut) {
//
//        }
//
//        @Override
//        public void stopScan() {
//
//        }
//
//        @Override
//        public void connectBluetoothDevice(String macAddr) {
//
//        }
//
//        @Override
//        public boolean isConnected() {
//            return false;
//        }
//
//        @Override
//        public void disconnectDevice() {
//
//        }
//
//        @Override
//        public void getDeviceInfo() {
//
//        }
//
//        @Override
//        public void getTransportSessionKey(String pubkey) {
//
//        }
//
//        @Override
//        public void updateMasterKey(String masterKey) {
//
//        }
//
//        @Override
//        public void updateWorkingKey(String pinKey, String macKey, String trackKey) {
//
//        }
//
//        @Override
//        public void clearAids() {
//
//        }
//
//        @Override
//        public void addAid(String aid) {
//
//        }
//
//        @Override
//        public void clearRids() {
//
//        }
//
//        @Override
//        public void addRid(String rid) {
//
//        }
//
//        @Override
//        public void getCardNumber(int timeout) {
//
//        }
//
//        @Override
//        public void getCurrentBatteryStatus() {
//
//        }
//
//        @Override
//        public void readCard(CardReadEntity cardReadEntity) {
//
//        }
//
//        @Override
//        public void getInputInfoFromKB(InputInfoEntity entityInfoEntity) {
//
//        }
//
//        @Override
//        public void icCardWriteback(EMVOnlineData onLineData) {
//
//        }
//
//        @Override
//        public void cancelTrade() {
//
//        }
//
//        @Override
//        public void calculateMac(String macData) {
//
//        }
//
//        @Override
//        public void updateFirmware(String filePath) {
//
//        }
//
//        @Override
//        public void generateQRCode(int keepShowTime, String content) {
//
//        }
//
//        @Override
//        public void setTransactionInfo(String transactionInfo) {
//
//        }
//
//        @Override
//        public void getTransactionInfo() {
//
//        }
//
//        @Override
//        public void displayTextOnScreen(int keepShowTime, String content) {
//
//        }
//    };

//    private INpSwipeListener mNpSwipeListener = new NpSwipe() {
//        @Override
//        public void onScannerResult(BluetoothDevice devInfo) {
//
//        }
//
//        @Override
//        public void onDeviceConnected() {
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    //Toast.makeText(BluetoothActivity.this, context.getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
//                    Message msg = new Message();
//                    msg.what = Constant.OPERATTON_RESULT_BLUETOOTH;
//                    msg.obj = "onDeviceConnected";
//                    mHandler.sendMessage(msg);
//                    Log.d("BluetoothApp", "onDeviceConnected");
//                    Context context = posApp.getApplicationContext();
//                    Toast.makeText(context, context.getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        @Override
//        public void onDeviceDisConnected() {
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    Message msg = new Message();
//                    msg.what = Constant.OPERATTON_RESULT_BLUETOOTH;
//                    msg.obj = "onDeviceDisConnected";
//                    mHandler.sendMessage(msg);
//                    Log.d("BluetoothApp", "onDeviceDisConnected");
//                    Context context = posApp.getApplicationContext();
//                    Toast.makeText(context, context.getText(R.string.device_disconnect), Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        @Override
//        public void onGetDeviceInfo(DeviceInfoEntity info) {
//
//        }
//
//        @Override
//        public void onGetTransportSessionKey(String encryTransportKey) {
//
//        }
//
//        @Override
//        public void onUpdateMasterKeySuccess() {
//
//        }
//
//        @Override
//        public void onUpdateWorkingKeySuccess() {
//
//        }
//
//        @Override
//        public void onAddAidSuccess() {
//
//        }
//
//        @Override
//        public void onAddRidSuccess() {
//
//        }
//
//        @Override
//        public void onClearAids() {
//
//        }
//
//        @Override
//        public void onClearRids() {
//
//        }
//
//        @Override
//        public void onGetCardNumber(String cardNum) {
//
//        }
//
//        @Override
//        public void onGetDeviceBattery(boolean result) {
//
//        }
//
//        @Override
//        public void onDetachedIC() {
//
//        }
//
//        @Override
//        public void onGetReadCardInfo(CardInfoEntity cardInfoEntity) {
//
//        }
//
//        @Override
//        public void onGetReadInputInfo(String inputInfo) {
//
//        }
//
//        @Override
//        public void onGetICCardWriteback(boolean result) {
//
//        }
//
//        @Override
//        public void onCancelReadCard() {
//
//        }
//
//        @Override
//        public void onGetCalcMacResult(String encryMacData) {
//
//        }
//
//        @Override
//        public void onUpdateFirmwareProcess(float percent) {
//
//        }
//
//        @Override
//        public void onUpdateFirmwareSuccess() {
//
//        }
//
//        @Override
//        public void onGenerateQRCodeSuccess() {
//
//        }
//
//        @Override
//        public void onSetTransactionInfoSuccess() {
//
//        }
//
//        @Override
//        public void onGetTransactionInfoSuccess(String transactionInfo) {
//
//        }
//
//        @Override
//        public void onDisplayTextOnScreenSuccess() {
//
//        }
//
//        @Override
//        public void onReceiveErrorCode(int error, String message) {
//            System.out.println("onReceiverErrorCode");
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    Log.d("Pos","onReceiveErrorCode()");
//                    Toast.makeText(posApp.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    };

    public Pos(PosApp posApp) {
        this(posApp, new PosOptions());
    }

    public Pos(PosApp posApp, PosOptions posOptions) {
        if (posOptions == null) {
            throw new IllegalArgumentException("posOptions is null" );
        }

        this.posApp = posApp;
        // inicializa el manejador de vectores de inicializacion
        // construye el bridge al terminal
        this.terminal = new Terminal();
        terminal.init(posApp.getApplicationContext());

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
        this.msr = new Msr(this.terminal.getMsr().getDevice());
        this.emv = null; //new Emv(this, posApp.getApplicationContext());
        this.pinpad = new Pinpad(this.terminal.getPinpad().getDevice(), this.posOptions.getIvController());
        //this.withPinpad(this::configPinpad);

        //carga los AID y CAPK por defecto
        //this.setTagList(Defaults.TAG_LIST);
        this.pinpad.setTimeout(Defaults.PINPAD_REQUEST_TIMEOUT);
        this.setPinpadCustomUI(false);

        //Instanciamos el posManager(N98Pos)
        posManager = NpPosManager.sharedInstance(posApp.getApplicationContext(), this);
    }

    public void beep() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    public void setPinLength(int minLen, int maxLen) {
        this.pinpad.setPinLength(minLen, maxLen);
    }

    public void setPinLength(int lenPin) {
        this.pinpad.setPinLength(lenPin, lenPin);
    }

    public String getSerialNumber() {
        return this.terminal.getSerialNumber();
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

    public void connectDevice(String macAddressN98){
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

    }

    public void setPinpadTimeout(int timeout) {
        this.pinpad.setTimeout(timeout);
    }

    public void setTagList(int[] tagList) {
        this.emv.setTaglist(tagList);
    }

    private void configPinpad(Pinpad pinpad) {
        pinpad.setGUIConfiguration("sound", "true" );
    }

    public Pinpad getPinpad() {
        return this.pinpad;
    }

    /**
     * Realiza una operacion atomica con el pinpad preservando el estado anterior.
     * Si el pinpad se encuentra abierto mantiene la sesion de trabajo abierta.
     *
     * @param consumer
     */
    public void withPinpad(Consumer<Pinpad> consumer) {
        boolean wasOpen = this.pinpad.isOpen();
        try {
            if (!wasOpen) this.getPinpad().open();
            consumer.accept(this.getPinpad());
        } finally {
            if (!wasOpen) this.getPinpad().close();
        }
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

    /**
     * Realiza una operacion atomica de impresion.
     *
     * @param consumer
     */
    public void withPrinter(Consumer<Printer> consumer) {
        PrinterDevice device = this.terminal.getPrinter().getDevice();
        try {
            device.open();
        } catch (DeviceException e) {
            //TODO convertir en excepciones nombradas y republicar
            // Log.d(Defaults.LOG_TAG, "No se puede abrir la impresora", e);
            throw new RuntimeException(e);
        }
        consumer.accept(new Printer(device));
        try {
            device.close();
        } catch (DeviceException e) {
            // Log.d(Defaults.LOG_TAG, "No se puede cerrar la impresora", e);
        }
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
    public void beginTransaction(String date, String time, String tsc, String amount, boolean cashback) {
        emv.reset();
        data = new TransactionData();
        emv.beginTransaction(
                date,
                time,
                tsc,
                amount,
                cashback
        );
    }

    /**
     * Inicia una transaccion.
     *
     * @param date   fecha en formato YYMM
     * @param time   hora en formato HHMMSS
     * @param tsc    contador de transaccion
     * @param amount monto en formato ex2, por ejemplo 1000 representa 10.00
     */
    public void beginTransaction(String date, String time, String tsc, String amount) {
        this.beginTransaction(date, time, tsc, amount, false);
    }


    /// para el control del pinpad

    /**
     * Accede al uso del pinpad y coordina su ingreso con funciones del pos
     *
     * @param pan
     */
    private void waitForPinpad(String pan) {
        pinpad.open();

        if (!pinpad.listenForPinBlock(pan, this::pinpadEventResolved, this.digitsListener)) {
            onError.accept("pin", "startFailed" );
            pinpad.close();
        }
    }

    private void pinpadEventResolved(OperationResult operationResult) {
        int code = operationResult.getResultCode();
        switch (code) {
            case OperationResult.SUCCESS:
                // cuando ha logrado tener el pinblock
                PINPadOperationResult pinPadOperationResult = (PINPadOperationResult) operationResult;
                byte[] data = pinPadOperationResult.getEncryptedPINBlock();
                String pinblock = Util.toHexString(data);
                this.data.pinblock = pinblock;
                if (this.pinpadCustomUI) {
                    if (this.onPinCaptured != null) {
                        this.onPinCaptured.run();
                    } else {
                        raiseError("pin", "request_handler_null" );
                    }
                } else {
                    continueAfterPin();
                }
                break;
            case OperationResult.CANCEL:
                raiseError("pin", "cancel" );
                break;
            case OperationResult.ERR_TIMEOUT:
                raiseError("pin", "timeout" );
                break;
            default:
                raiseError("pin", "" + code);
                break;
        }
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
     * Se usa para continuar procesando la transaccion despues de pedir el pin.
     */
    public void continueAfterPin() {
        if ("msr".equals(data.captureType)) {
            processOnline();
        } else {
            EMVJNIInterface.emv_set_online_pin_entered(1);
            this.emv.next();
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

    protected void raiseError(String source, String payload) {
        // Log.d(Defaults.LOG_TAG, "error: " + source + " " + payload);
        if (this.onError != null) onError.accept(source, payload);
    }

    public void setOnWarning(BiConsumer<String, String> onWarning) {
        this.onWarning = onWarning;
    }

    protected void raiseWarning(String source, String payload) {
        // Log.d(Defaults.LOG_TAG, "warning: " + source + " " + payload);
        if (this.onWarning != null) onWarning.accept(source, payload);
    }

    protected boolean isPinpadCustomUI() {
        return pinpadCustomUI;
    }

    protected Msr getMsr() {
        return msr;
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

    protected static class ClientMessengerHandler extends Handler{

        private WeakReference<Pos> mActivity;

        public ClientMessengerHandler(Pos activity){
            mActivity = new WeakReference<Pos>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            System.out.println("[Pos][ClientMessengerHandler] handleMessage: " + msg.toString());
            Pos activity = mActivity.get();
            if(activity != null){
                activity.handleMessageClient(msg);
            }
        }
    }

    @Override
    public void onScannerResult(BluetoothDevice devInfo) {

    }

    @Override
    public void onDeviceConnected() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //Toast.makeText(BluetoothActivity.this, context.getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
                Message msg = new Message();
                msg.what = Constant.OPERATTON_RESULT_BLUETOOTH;
                msg.obj = "onDeviceConnected";
                mHandler.sendMessage(msg);
                Log.d("BluetoothApp", "onDeviceConnected");
                Context context = posApp.getApplicationContext();
                Toast.makeText(context, context.getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeviceDisConnected() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = Constant.OPERATTON_RESULT_BLUETOOTH;
                msg.obj = "onDeviceDisConnected";
                mHandler.sendMessage(msg);
                Log.d("BluetoothApp", "onDeviceDisConnected");
                Context context = posApp.getApplicationContext();
                Toast.makeText(context, context.getText(R.string.device_disconnect), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onGetDeviceInfo(DeviceInfoEntity info) {

    }

    @Override
    public void onGetTransportSessionKey(String encryTransportKey) {

    }

    @Override
    public void onUpdateMasterKeySuccess() {

    }

    @Override
    public void onUpdateWorkingKeySuccess() {

    }

    @Override
    public void onAddAidSuccess() {

    }

    @Override
    public void onAddRidSuccess() {

    }

    @Override
    public void onClearAids() {

    }

    @Override
    public void onClearRids() {

    }

    @Override
    public void onGetCardNumber(String cardNum) {

    }

    @Override
    public void onGetDeviceBattery(boolean result) {

    }

    @Override
    public void onDetachedIC() {

    }

    @Override
    public void onGetReadCardInfo(CardInfoEntity cardInfoEntity) {

    }

    @Override
    public void onGetReadInputInfo(String inputInfo) {

    }

    @Override
    public void onGetICCardWriteback(boolean result) {

    }

    @Override
    public void onCancelReadCard() {

    }

    @Override
    public void onGetCalcMacResult(String encryMacData) {

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

    }

    @Override
    public void onGetTransactionInfoSuccess(String transactionInfo) {

    }

    @Override
    public void onDisplayTextOnScreenSuccess() {

    }

    @Override
    public void onReceiveErrorCode(int error, String message) {
        System.out.println("onReceiverErrorCode");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("Pos","onReceiveErrorCode()");
                Toast.makeText(posApp.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
