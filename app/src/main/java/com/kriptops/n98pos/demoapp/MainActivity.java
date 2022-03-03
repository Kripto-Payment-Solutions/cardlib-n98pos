package com.kriptops.n98pos.demoapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kriptops.n98pos.cardlib.EMVConfig;
import com.kriptops.n98pos.cardlib.Pinpad;
import com.kriptops.n98pos.cardlib.Pos;
import com.kriptops.n98pos.cardlib.Printer;
import com.kriptops.n98pos.cardlib.TransactionData;
import com.kriptops.n98pos.cardlib.android.BluetoothApp;
import com.kriptops.n98pos.cardlib.constant.Constant;
import com.kriptops.n98pos.cardlib.model.ResponsePos;
import com.kriptops.n98pos.cardlib.tools.Util;
import com.kriptops.n98pos.cardlib.android.PosApp;
import com.kriptops.n98pos.demoapp.utils.LogUtil;
import com.kriptops.n98pos.demoapp.utils.RSAUtil;
import com.kriptops.n98pos.demoapp.utils.StringUtil;
import com.kriptops.n98pos.demoapp.utils.TDesUtil;
import com.kriptops.n98pos.demoapp.view.ActivityCollector;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.CardReadEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.NpPosManager;
import com.newpos.mposlib.util.ISOUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity{

    private EditText masterKey;
    private EditText pinKey;
    private EditText dataKey; //trackkey
    private EditText macKey;
    private EditText plainText;
    private EditText encriptedText;
    private TextView log;

    private String KEK = "";

    private Button btnConnectDevice;

    private boolean lConnectDevice = false;

    private static final int requestCode = 1;

    private static final String macAdrressN98 = "18:B6:F7:0C:7B:CA";

    private NpPosManager posManager;
    private TestNpManager testNpPosManager;

    //@Override
    public Pos getPos() {
        return ((PosApp) this.getApplication()).getPos();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCollector.addActivity(this);
        this.masterKey = this.findViewById(R.id.txt_llave_master);
        // Setear con la configuracion de la MK de pruebas asignada
        //this.masterKey.setText("A283C38D7D7366C6DEFD9B6FFBF45783");
        this.masterKey.setText("7C539EFD77EA6BAB51314BB1BF681C60");
        this.pinKey = this.findViewById(R.id.txt_llave_pin);
        this.dataKey = this.findViewById(R.id.txt_llave_datos);
        this.macKey = this.findViewById(R.id.txt_llave_mac);
        this.plainText = this.findViewById(R.id.txt_texto_plano);
        this.encriptedText = this.findViewById(R.id.txt_texto_cifrado_hex);
        this.log = this.findViewById(R.id.txt_log);

        this.btnConnectDevice = this.findViewById(R.id.btn_connect_device);

        requestBtPermission(this, requestCode, getApplicationContext().getString(R.string.request_permission));

        getPos().setOnError(this::onError);
        getPos().setOnSuccess(this::onSuccess);

        //posManager = NpPosManager.sharedInstance(getApplicationContext(), this);
        //testNpPosManager = new TestNpManager(getApplicationContext(), this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Android 6.0上动态申请蓝牙权限 -- Solicitar dinámicamente permisos de Bluetooth en
     *
     * @param activity    当前activity -- Actual Activity
     * @param requestCode 请求码 -- código de solicitud
     * @param showText    若弹Toast提示，需要显示的信息  -- Si se muestra el indicador Toast, la información que se mostrará
     */
    private void requestBtPermission(Activity activity, int requestCode, String showText) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, requestCode);
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(activity, showText, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void clearLog(View log) {
        this.log.setText("");
    }

    public void btn_generar_llaves(View btn) {
        // Log.d(Defaults.LOG_TAG, "Generar llaves");
        byte[] data = new byte[16];
        Random r = new Random();

        r.nextBytes(data);
        pinKey.setText(Util.toHexString(data));
        // Log.d(Defaults.LOG_TAG, "llave de pin " + pinKey.getText());

        r.nextBytes(data);
        dataKey.setText(Util.toHexString(data));
        // Log.d(Defaults.LOG_TAG, "llave de datos " + dataKey.getText());

        r.nextBytes(data);
        macKey.setText(Util.toHexString(data));

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    public void btn_inyectar_llaves(View btn) {

        if(getPos().getPosManager().isConnected()) {

            // Log.d(Defaults.LOG_TAG, "Inyectar llaves");
            String masterKey = this.masterKey.getText().toString();
            String pinkey = pinKey.getText().toString();
            String mackey = macKey.getText().toString();
            String trackkey = dataKey.getText().toString();

            //salida de la call a init en OT
//            String ewkPinHex = protectKey(masterKey, pinKey.getText().toString());
//            // Log.d(Defaults.LOG_TAG, "llave de pin " + ewkPinHex);
//            String ewkDataHex = protectKey(masterKey, dataKey.getText().toString());
//            // Log.d(Defaults.LOG_TAG, "llave de datos(track) " + ewkDataHex);
//            String ewkMacHex = protectKey(masterKey, macKey.getText().toString());
//            // Log.d(Defaults.LOG_TAG, "llave de mac " + ewkDataHex);

            pinkey      = "6CEBADBA69612480FC2FC331D291B0F1"; //clear key
            mackey      = "23BC72AC94C03BD34D9E6BCB1AB3F950"; //clear key
            trackkey    = "C305C4F9B5B84E6CE0A3789FF822101E"; //clear key

            byte[] IV           = ISOUtil.hex2byte("0000000000000000");
            byte[] encryptPIN   = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(pinkey));
            byte[] PINkcv       = TDesUtil.encryptECB(ISOUtil.hex2byte(pinkey), IV);
            byte[] encryptMAC   = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(mackey));
            byte[] MACkcv       = TDesUtil.encryptECB(ISOUtil.hex2byte(mackey), IV);
            byte[] encryptTrack = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(trackkey));
            byte[] TRACKkcv     = TDesUtil.encryptECB(ISOUtil.hex2byte(trackkey), IV);

            boolean[] response = new boolean[1];

            getPos().withPosManager(npPosManager -> {
                response[0] = npPosManager.updateKeys(
                        encryptPIN,
                        PINkcv,
                        encryptMAC,
                        MACkcv,
                        encryptTrack,
                        TRACKkcv
                );
            });

            this.runOnUiThread(() -> {
                if (response[0]) {
                    Toast.makeText(this, "Llaves actualizadas", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No se puede actualizar llaves", Toast.LENGTH_LONG).show();
                }
            });
        }else{
            this.runOnUiThread(() -> {
                Toast.makeText(this, "Device not connected to bluetooth", Toast.LENGTH_LONG).show();
            });
        }
    }

    public void updateKeys(Pinpad pinpad) {
    }

    public void btn_connect_blue(View btn){
        if(!lConnectDevice){
            getPos().connectBTDevice(macAdrressN98);
        }else{
            getPos().disConnectDevice();
        }

        //BluetoothActivity.actionStart(MainActivity.this,"This");
    }

    public void btn_encriptar(View btn) {
        // Log.d(Defaults.LOG_TAG, "Cifrar");
        //este primer paso es necesario porque yo tengo data ascii y no hex string
        getPos().withPinpad(this::encrypt);
    }

    public void encrypt(Pinpad pinpad) {
        String plainText = this.plainText.getText().toString();
        String plainHex = Util.toHexString(plainText.getBytes(), true);
        // Log.d(Defaults.LOG_TAG, "Encriptando: " + plainHex);
        String encrypted = getPos().getPinpad().encryptHex(plainHex);
        this.encriptedText.setText(encrypted);
    }

    public void btn_imprimir_ticket(View btn) {
        // Log.d(Defaults.LOG_TAG, "Imprimir Ticket");
        getPos().withPrinter(this::print);
    }

    public void print(Printer printer) {
        for (Printer.FontSize size : Printer.FontSize.values())
            for (Printer.Align align : Printer.Align.values()) {
                printer.println(size.name() + " " + align.name(), size, align);
            }
        printer.feedLine();
        printer.feedLine();
        printer.feedLine();
        printer.feedLine();
    }

    public void btn_do_trade(View view) {
        this.log.setText("Present Card");

//        CardReadEntity cardReadEntitys =  new CardReadEntity();
//        cardReadEntitys.setSupportFallback(false);
//        cardReadEntitys.setTimeout(30);
//        cardReadEntitys.setAmount("000000005501");
//        cardReadEntitys.setTradeType(0);
//        getPos().getPosManager().readCard(cardReadEntitys);

//        /*getPos().configTerminal( // este metodo se puede llamar una sola vez
//                "PK000001", // tag 9F16 identidad del comercio
//                "PRUEBA KRIPTO", // tag 9F4E nombre del comercio
//                "00000001", // tag 9F1C identidad del terminal dentro del comercio (no es el serial number)
//                "000000000000", // floor limit contactless
//                "000000015000", // transaction limit contactless
//                "000000015000" // cvm limit (desde que monto pasan de ser quick a full)
//        );*/
//        EMVConfig config = new EMVConfig();
//        config.currencyCode = "0604";
//        config.currencyExponent = "02";
//        config.merchantIdentifier = "12345678";
//        config.terminalCountryCode = "0604";
//        config.terminalIdentification = "1234";
//        config.terminalCapabilities = "E0F8C8";
//        config.terminalType = "21";
//        config.additionalTerminalCapabilities = "FF80F0A001";
//        config.merchantNameAndLocation = "COMERCIO DE PRUEBA";
//        config.ttq1 = "36";
//        config.contactlessFloorLimit = "000000000000";
//        config.contactlessCvmLimit = "000000015000";
//        config.contactlessTransactionLimit = "009999999999";
//        config.statusCheckSupport = "00";
//        getPos().configTerminal(config);
//
//        getPos().setPinpadCustomUI(true); // cambia la pantalla de fondo cuando se solicita el uso del pinpad
//        getPos().setOnPinRequested(this::onPinRequested);
//        getPos().setDigitsListener(this::onPinDigit);
//        getPos().setOnPinCaptured(this::onPinCaptured);
//
//        getPos().setTagList(new int[]{
//                0x5f2a,
//                0x82,
//                0x95,
//                0x9a,
//                0x9c,
//                0x9f02,
//                0x9f03,
//                0x9f10,
//                0x9f1a,
//                0x9f26,
//                0x9f27,
//                0x9f33,
//                0x9f34,
//                0x9f35,
//                0x9f36,
//                0x9f37,
//                0x9f40
//        });
//        getPos().setOnError(this::onError);
//        getPos().setGoOnline(this::online);
//        getPos().setOnSuccess(this::onSuccess);
        getPos().beginTransaction( // ete metodo se llama en cada transaccion
                "210820", // fecha en formato
                "030800",
                "00000001",
                "100"
                //,false //agregar para hacer el cashback
        );
    }

    private void online(TransactionData data) {
        // Log.d(Defaults.LOG_TAG, "online message " + data);
        this.runOnUiThread(() -> {
            //enviar a autorizar
            this.log.setText("Online Message " + data);
        });
    }

    private void onError(String source, String code) {
        // Log.d(Defaults.LOG_TAG, "Controlar el error de lectura de datos");
        this.runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "[MainActivity]: " + code, Toast.LENGTH_SHORT).show();
            this.log.setText("Error " + source + " " + code);
        });
    }

    private void onSuccess(String source, ResponsePos responsePos) {
    //private void onSuccess(String source, String code) {
        // Log.d(Defaults.LOG_TAG, "Controlar el error de lectura de datos");
        this.runOnUiThread(() -> {

            switch(source.toString()){
                case "onDeviceConnected":
                    btnConnectDevice.setText("Disconnect Device");
                    lConnectDevice = true;

                    //ACTUALIZAR MASTER KEY
                    //*********************
                    //Generamos las llaves publicas y privadas
                    //Se transmite el módulo de clave pública, el exponente es fijo
                    RSAUtil.generateRSAKeyPair();
                    try {
                        RSAPublicKey publickey  = (RSAPublicKey)RSAUtil.getPublicKey(RSAUtil.PUBLIC_KEY_PATH);
                        byte[] module           = publickey.getModulus().toByteArray();
                        LogUtil.e("module ="+ISOUtil.byte2hex(module));
                        //getPos().getPosManager().getTransportSessionKey(ISOUtil.byte2hex(module,1,module.length-1));//128 ->256
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                LogUtil.d("[error ]onDeviceConnected[generateRSAKeyPair]",e.getMessage());
                                Toast.makeText(getApplicationContext(), "[error ]onDeviceConnected[generateRSAKeyPair]: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    break;
                case "onDeviceDisConnected":
                    btnConnectDevice.setText("Connect Device");
                    lConnectDevice = false;
                    break;

                case "onGetTransportSessionKey":
                    boolean lunpack = kek_unpack(responsePos.getEncryTransportKey());

                    if(lunpack){
                        //Actualizamos la master key
                        LogUtil.e("update master key kek =" + KEK);
                        String masterkey = this.masterKey.getText().toString();
                        byte []encrypt  = TDesUtil.encryptECB(ISOUtil.hex2byte(KEK),ISOUtil.hex2byte(masterkey));
                        LogUtil.e("update master key encrypt =" + ISOUtil.byte2hex(encrypt));
                        byte []IV       = ISOUtil.hex2byte("0000000000000000");
                        byte []kcv      = TDesUtil.encryptECB(ISOUtil.hex2byte(masterkey), IV);
                        LogUtil.e("update master key kcv =" + ISOUtil.byte2hex(kcv));
                        getPos().getPosManager().updateMasterKey(ISOUtil.byte2hex(encrypt)+ISOUtil.byte2hex(kcv,0,4));

                    }
                    break;

                case "onUpdateMasterKeySuccess":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onUpdateMasterKeySuccess","onUpdateMasterKeySuccess()");
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "onUpdateWorkingKeySuccess":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onUpdateWorkingKeySuccess","onUpdateWorkingKeySuccess()");
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
        });
    }

    private void onPinRequested() {
        // hacer con las graficas lo que se quiera luego enlazar el pin
        // el emv thread esta fuera del main looper hay que llamar prepare para acceder a los contextos graficos o entrar al main looper
        this.runOnUiThread(() -> {
            this.log.setText("requiriendo el pin");
        });
        //esta parte inica el proceso de llamada del pin
        getPos().callPin();
    }

    private void onPinCaptured() {
        // hacer con las graficas lo que se quiera luego enlazar el pin
        // el emv thread esta fuera del main looper hay que llamar prepare para acceder a los contextos graficos o entrar al main looper
        this.runOnUiThread(() -> {
            this.log.setText("pin leido seguir el flujo");
        });
        getPos().continueAfterPin();
    }

    private void onPinDigit(Integer pinDigits) {
        // Log.d(Defaults.LOG_TAG, "cantidad de digitos del pin " + pinDigits);
        this.runOnUiThread(() -> {
            if (pinDigits > 0) {
                this.log.setText("requiriendo el pin " + "********".substring(0, pinDigits));
            } else {
                this.log.setText("requiriendo el pin");
            }
        });
    }

    //TOOL para cifrar en 3DESede ECB NoPadding
    public byte[] protectKey(byte[] suppliedKey, byte[] data) {
        byte[] keyMaterial = new byte[24];
        System.arraycopy(suppliedKey, 0, keyMaterial, 0, 16);
        System.arraycopy(suppliedKey, 0, keyMaterial, 16, 8);
        try {
            SecretKeySpec key = new SecretKeySpec(keyMaterial, "DESede");
            Cipher cip = Cipher.getInstance("DESede/ECB/NoPadding");
            cip.init(Cipher.ENCRYPT_MODE, key);
            return cip.doFinal(data);
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | BadPaddingException
                | IllegalBlockSizeException
                | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public String protectKey(String suppliedKey, String data) {
        return Util.toHexString(
                protectKey(
                        Util.toByteArray(suppliedKey),
                        Util.toByteArray(data)
                ));
    }

    public boolean kek_unpack(final String s){
        try {
            Log.d("s:" , s);
            Log.d("KCV =" , s.substring(s.length()-8));
            String kcv = s.substring(s.length()-8);
            PrivateKey privateKey = RSAUtil.getPrivateKey(RSAUtil.PRIVATE_KEY_PATH);

            Cipher cipher = Cipher.getInstance("RSA");//PKCS1Padding
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            Log.d("string ==" , s.substring(0,s.length()-8 ));
            byte[] bytes = cipher.doFinal(StringUtil.hexStr2Bytes(s.substring(0,s.length()-8 )));//256 ->512
            LogUtil.d("decrypt result==" , ISOUtil.byte2hex(bytes));
            //final String kek = StringUtil.byte2HexStr(bytes);
            //final String kek =s.substring(s.length()-32-8,s.length()-8);
            byte []kekbyte=new byte[16];
            byte []IV=ISOUtil.hex2byte("0000000000000000");
            System.arraycopy(bytes,bytes.length-16,kekbyte,0,16);
            LogUtil.d("kek:" + ISOUtil.byte2hex(kekbyte));
            byte[]calckcv=TDesUtil.encryptECB(kekbyte,IV);
            LogUtil.d("calckcv:" + ISOUtil.byte2hex(calckcv));
            LogUtil.d("KCV:" +kcv);
            if(!ISOUtil.memcmp(calckcv,0,ISOUtil.hex2byte(kcv),0,4)){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.kek_unpack_fail), Toast.LENGTH_SHORT).show();
                    }

                });
                return false;
            }
            KEK= ISOUtil.byte2hex(kekbyte);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.kek_unpack_success), Toast.LENGTH_SHORT).show();
                }
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.kek_unpack_fail), Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
    }
}