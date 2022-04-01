package com.kriptops.n98pos.cardlib;

import static com.cloudpos.jniinterface.EMVJNIInterface.emv_get_tag_data;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.cloudpos.AlgorithmConstants;
import com.cloudpos.DeviceException;
import com.cloudpos.OperationResult;
import com.cloudpos.jniinterface.EMVJNIInterface;
import com.cloudpos.pinpad.KeyInfo;
import com.cloudpos.pinpad.PINPadDevice;
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
import com.kriptops.n98pos.cardlib.model.ResponsePos;
import com.kriptops.n98pos.cardlib.tools.Util;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.CardReadEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.EMVOnlineData;
import com.newpos.mposlib.sdk.INpPosControler;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.InputInfoEntity;
import com.newpos.mposlib.sdk.NpPosManager;
import com.newpos.mposlib.util.ISOUtil;

import java.lang.ref.WeakReference;
import java.security.PrivateKey;
import java.util.Currency;
import java.util.List;

import javax.crypto.Cipher;

public class Pos {

    private final Terminal terminal;

    //TODO migrate to new infrastructure
    private final Emv emv;
    private final Pinpad pinpad;
    private final PinpadNPos pinpadNPos;
    private final Msr msr;

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

                    Toast.makeText(context, messageShow, Toast.LENGTH_SHORT).show();
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

                    Toast.makeText(context, messageShow, Toast.LENGTH_SHORT).show();
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
            raiseSuccess("onUpdateWorkingKeySuccess", response);
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
                    Toast.makeText(posApp.getApplicationContext(), "[onReceiveErrorCode]: " + message, Toast.LENGTH_SHORT).show();
                    raiseError("onReceiveErrorCode", message);
                }
            });
        }
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

        this.pinpadNPos = new PinpadNPos(this.posOptions.getIvController());

        this.posOptions.getIvController().saveIv(this.pinpadNPos.IV_DATA, this.pinpadNPos.DEFAULT_IV);
        this.posOptions.getIvController().saveIv(this.pinpadNPos.IV_PIN, this.pinpadNPos.DEFAULT_IV);
        this.pinpadNPos.setIvController(this.posOptions.getIvController());

        this.pinpad = new Pinpad(this.terminal.getPinpad().getDevice(), this.posOptions.getIvController());
        //this.withPinpad(this::configPinpad);

        //carga los AID y CAPK por defecto
        //this.setTagList(Defaults.TAG_LIST);
        this.pinpad.setTimeout(Defaults.PINPAD_REQUEST_TIMEOUT);
        this.setPinpadCustomUI(false);

        //Instanciamos el posManager(N98Pos)
        posManager = NpPosManager.sharedInstance(posApp.getApplicationContext(), mNpSwipeListener);
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

    public void setTagAIDs() {
        if(posManager.isConnected()) {
            for (String aid : Defaults.AIDS) {
                this.posManager.addAid(aid);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
            raiseWarning("not_set_aids", messageShow);
        }
    }

    public void clearAIDS(){
        posManager.clearAids();
    }

    private void configPinpad(Pinpad pinpad) {
        pinpad.setGUIConfiguration("sound", "true" );
    }

    public Pinpad getPinpad() {
        return this.pinpad;
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
        //data.track2 = this.pinpad.encryptHex(data.track2);   //encryta el track2 con el pinpad

        cardInfoEntity.setTrack2(data.track2);
        cardInfoEntity.setBin(data.bin);

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

    protected String readTag(int tag) {
        //if (isTagPresent(tag)) {
            byte[] buffer = new byte[1024];
            int read_length = emv_get_tag_data(tag, buffer, buffer.length);
            if (read_length < 0) return null;
            if (read_length == 0) return "";
            byte[] tagData = new byte[read_length];
            System.arraycopy(buffer, 0, tagData, 0, read_length);
            return Util.toHexString(tagData);
        //}
        //return null;
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

    public void loadAidparam(List<String> lstAIDS){
        if(posManager.isConnected()) {
            for(String str : lstAIDS){
                posManager.addAid(str);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
            raiseWarning("device_not_connect", messageShow);
        }

        /*
        //VISA EMV
        //A0000000031010
        posManager.addAid("00A6DF80800001009C01009F0607A0000000031010DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");
        //A0000000032010
        posManager.addAid("00A6DF80800001009C01009F0607A0000000032010DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");
        //A0000000033010
        posManager.addAid("00A6DF80800001009C01009F0607A0000000033010DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");

        //MASTERCARD EMV
        //A0000000042010
        posManager.addAid("00A6DF80800001009C01009F0607A0000000042010DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");
        //A0000000043060
        posManager.addAid("00A6DF80800001009C01009F0607A0000000043060DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");
        //A0000000041010
        posManager.addAid("00A6DF80800001009C01009F0607A0000000041010DF8080600101DF80800281879F01061234567890129F090200029F160F3132333435363738393031323334359F150212349F4E0D54657374204D65726368616E749F1C0846726F6E743132339F1A0208409F3501229F3303E0F8E89F4005F000F0F001DF80806106000000100000DF808020050000000000DF808021050000000000DF8080220500000000009F1B0400000000");
        //VISA PAYWAVE
        //A0000000031010
        posManager.addAid("00AFDF80800001039C01009F0607A0000000031010DF8080600101DF80800281909F1A0208629F1B04000000009F3501229F090201569F3303E0F8C89F660436004000DF808061030080F8DF8080280101DF8080290100DF808026079F370400000000DF8080270A9F1A0295059A039C0100DF808020000000000000DF80802105DC4000F800DF80802205DC4000F800DF80802A06000100000000DF80802B06000000000000DF80802C06000000005000");
        //A000000003
        //posManager.addAid("00A2DF80800001009C01009F0605A000000003DF8080600101DF80800281859F01063030303030309F090201569F160F3030303030303030303030303030309F150200019F4E0942616E6420436172649F1C08504F5330303030319F1A020840DF808061030000F89F66043600C000DF8080280100DF80802901009F1B04000075309F1E085465726D696E616C9F3303E0F8C89F1B04000000105F2A0200329f1a020032");

        //MasterCard PAYPASS
        //A0000000041010
        posManager.addAid("00D0DF80800001029C01009F0607A0000000041010DF8080600101DF80800281B19f37047961ab19DF811F01c8DF81170160DF81180160DF811901089f3501229f09020002DF80802A06000020000000DF80802B06000010001000DF80802C06000000010000DF812506000099999999DF812306000000000000DF812406000099999999DF812005F45084800CDF8121050000000000DF812205F45084800CDF8126060000000100009F6604268000009F1B04000000009f1a0203929F1D082C00800000000000DF811B01B09F3303E0F8C8");
        */

    }

    public void loadCapkparam(List<String> lstCAPKS){
        if(posManager.isConnected()) {
            for(String str : lstCAPKS){
                posManager.addRid(str);
            }
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
            raiseWarning("device_not_connect", messageShow);
        }

        /*
        //A000000003
        // 0x08
        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");
        //0x09
        posManager.addRid("0112DF80801005A0000000039F220109DF80801181F89D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC4926D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC5521ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED56481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C026DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41DF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");
        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");
        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");

        posManager.addRid("00CADF80801005A0000000039F220108DF80801181B0D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A51DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE38F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7A21287752682F15832A678D6E1ED0BDF8080120103");
        */
    }

    public void clearAID_RID(){
        posManager.clearAids();
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
        /*
        emv.reset();
        data = new TransactionData();
        emv.beginTransaction(
                date,
                time,
                tsc,
                amount,
                cashback
        );
        */

        //this.clearAIDS();
        //this.setTagAIDs();
        if(posManager.isConnected()) {
            CardReadEntity cardReadEntitys =  new CardReadEntity();
            cardReadEntitys.setSupportFallback(false);
            cardReadEntitys.setTimeout(30);
            cardReadEntitys.setAmount(amount); //"000000080000"
            //cardReadEntitys.setAmount(amount);
            //0x01 mag 0x02 icc  0x04 nfc
            cardReadEntitys.setReadCardType(0x01 | 0x02 | 0x04);
            cardReadEntitys.setCurrency(currency);
            cardReadEntitys.setTerminalCoutryCode("0604");
            //cardReadEntitys.setReadCardType(0x04);
            //cardReadEntitys.setReadCardType(0x01 | 0x02 );
            cardReadEntitys.setTradeType(0);
            posManager.readCard(cardReadEntitys);
        }else{
            Context context = posApp.getApplicationContext();
            String messageShow = context.getText(R.string.device_not_connect).toString();
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this.posApp.getApplicationContext(), messageShow, Toast.LENGTH_SHORT).show();
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

    //    protected void raiseSuccess(String source, String payload) {
    //        // Log.d(Defaults.LOG_TAG, "Success: " + source + " " + payload);
    //        if (this.onSuccess != null) onSuccess.accept(source, payload);
    //    }

    protected void raiseSuccess(String source, ResponsePos payload) {
        // Log.d(Defaults.LOG_TAG, "Success: " + source + " " + payload);
        if (this.onSuccess != null) onSuccess.accept(source, payload);
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
}
