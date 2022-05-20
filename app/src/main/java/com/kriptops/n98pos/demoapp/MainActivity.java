package com.kriptops.n98pos.demoapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kriptops.n98pos.cardlib.Defaults;
import com.kriptops.n98pos.cardlib.Pos;
import com.kriptops.n98pos.cardlib.TransactionData;
import com.kriptops.n98pos.cardlib.model.ResponsePos;
import com.kriptops.n98pos.cardlib.tools.Util;
import com.kriptops.n98pos.cardlib.android.PosApp;
import com.kriptops.n98pos.demoapp.utils.LogUtil;
import com.kriptops.n98pos.demoapp.utils.RSAUtil;
import com.kriptops.n98pos.demoapp.utils.StringUtil;
import com.kriptops.n98pos.demoapp.utils.TDesUtil;
import com.kriptops.n98pos.demoapp.view.ActivityCollector;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.NpPosManager;
import com.newpos.mposlib.util.ISOUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private EditText track2;
    private EditText inputAmount;
    private EditText pan;
    private EditText dateTime;
    private TextView log;

    private String KEK = "";

    private Button btnConnectDevice;

    private boolean lConnectDevice = false;

    private static final int requestCode = 1;

    private static final String macAdrressN98 = "18:B6:F7:0C:7B:CA";

    private NpPosManager posManager;
    private TestNpManager testNpPosManager;

    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

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
        this.masterKey.setText("A283C38D7D7366C6DEFD9B6FFBF45783");
        this.pinKey = this.findViewById(R.id.txt_llave_pin);
        this.dataKey = this.findViewById(R.id.txt_llave_datos);
        this.macKey = this.findViewById(R.id.txt_llave_mac);
        this.plainText = this.findViewById(R.id.txt_texto_plano);
        this.encriptedText = this.findViewById(R.id.txt_texto_cifrado_hex);
        this.log = this.findViewById(R.id.txt_log);
        this.track2 = this.findViewById(R.id.txt_track2);
        this.dateTime = this.findViewById(R.id.txt_time);

        this.btnConnectDevice = this.findViewById(R.id.btn_connect_device);

        requestBtPermission(this, requestCode, getApplicationContext().getString(R.string.request_permission));

        getPos().setOnError(this::onError);
        getPos().setOnSuccess(this::onSuccess);

        this.pan = this.findViewById(R.id.txt_pan);
        this.pan.setText("4779042200107095");

        //String currentDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String currentDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        this.dateTime.setText(currentDate);
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
    //@RequiresApi(api = Build.VERSION_CODES.S)
    private void requestBtPermission(Activity activity, int requestCode, String showText) {

        if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
                Intent getpermission = new Intent();
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(getpermission);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);

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
            //mackey      = "23BC72AC94C03BD34D9E6BCB1AB3F950"; //clear key
            mackey      = "00000000000000000000000000000000"; //clear key
            trackkey    = "C305C4F9B5B84E6CE0A3789FF822101E"; //clear key

            pinkey      = "e534fa1f6992b8a3626aa953e7984473";
            trackkey    = "69600df994eca70ea0c5c2bd26bbc6e9";

            pinkey      = "7d3becc607c9d70413afe62c88ec000f";
            trackkey    = "9affd7970d1540822ffa05b6eaaae949";

            byte[] IV           = ISOUtil.hex2byte("0000000000000000");

            IV                  = ISOUtil.hex2byte("0000000000000000");

            byte[] encryptPIN   = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(pinkey));
            byte[] PINkcv       = TDesUtil.encryptECB(ISOUtil.hex2byte(pinkey), IV);
            byte[] encryptMAC   = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(mackey));
            byte[] MACkcv       = TDesUtil.encryptECB(ISOUtil.hex2byte(mackey), IV);
            byte[] encryptTrack = TDesUtil.encryptECB(ISOUtil.hex2byte(masterKey), ISOUtil.hex2byte(trackkey));
            byte[] TRACKkcv     = TDesUtil.encryptECB(ISOUtil.hex2byte(trackkey), IV);

            boolean[] response = new boolean[1];

            getPos().withPosManager(npPosManager -> {

                npPosManager.updateWorkingKey(
                        ISOUtil.byte2hex(encryptPIN),null, ISOUtil.byte2hex(encryptTrack)
                );

                response[0] = npPosManager.isUpdateWorkKeys();

                /*
                response[0] = npPosManager.updateKeys(
                        encryptPIN,
                        PINkcv,
                        encryptMAC,
                        MACkcv,
                        encryptTrack,
                        TRACKkcv
                );
                */
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

    public void btn_serial_number(View btn){
        if(getPos().getPosManager().isConnected()) {
            getPos().getPosManager().getDeviceInfo();
        }else{
            this.runOnUiThread(() -> {
                Toast.makeText(this, "Device not connected to bluetooth", Toast.LENGTH_LONG).show();
            });
        }
    }

    public void btn_connect_blue(View btn){
        if(!lConnectDevice){
            getPos().connectBTDevice(macAdrressN98);
        }else{
            getPos().disConnectDevice();
        }

        //BluetoothActivity.actionStart(MainActivity.this,"This");
    }

    public void btn_input_amount(View btn){
        getPos().inputAmount();
    }

    public void btn_input_pin(View btn){
        getPos().inputPin(this.pan.getText().toString());
    }

    public void btn_load_param_aid(View btn){
        getPos().loadAidparam(Defaults.AIDS);
    }

    public void btn_load_param_capk(View btn){
        getPos().loadCapkparam(Defaults.CAPKS);
    }

    public void btn_clear_aids(View btn){
        getPos().clearAids();
    }

    public void btn_clear_capks(View btn){
        getPos().clearRids();
    }

    public void btn_read_card(View btn){
        getPos().beginReadCard( // ete metodo se llama en cada transaccion
                "220401", // fecha en formato
                "030800",
                "00000001",
                "0000000000000",
                "0604"
                //,false //agregar para hacer el cashback
        );
    }

    public void btn_encriptar(View btn) {
        // Log.d(Defaults.LOG_TAG, "Cifrar");
        //este primer paso es necesario porque yo tengo data ascii y no hex string
        //getPos().withPinpad(this::encrypt);
    }

    public void btn_encrypt_data(View btn){
        String track2 = this.track2.getText().toString();
        String inCBC = getPos().getPosManager().EncryptDataCBC(track2);
        String inECB = getPos().getPosManager().EncryptDataECB(track2);
        this.track2.setText("CBC: " + inCBC + "\nECB: " + inECB);
    }

    public void btn_set_time(View btn){
        String dateNow = this.dateTime.getText().toString();
        LogUtil.d("DateNow for Device", dateNow);
        getPos().getPosManager().setupSystemDate(dateNow); // testeamos asi igaul genera error
    }

    public void btn_do_trade(View view) {
        //this.log.setText("Present Card");
        getPos().beginTransaction( // ete metodo se llama en cada transaccion
                "220401", // fecha en formato
                "030800",
                "00000001",
                "00000000850000",
                "0604"
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
                        getPos().getPosManager().getTransportSessionKey(ISOUtil.byte2hex(module,1,module.length-1));//128 ->256

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                LogUtil.d("onDeviceConnected","onDeviceConnected()");
                                Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
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

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onDeviceDisConnected","onDeviceDisConnected()");
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
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

                case "onGetDeviceInfo":
                    DeviceInfoEntity deviceInfo = (DeviceInfoEntity) responsePos.getObjResp();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onGetDeviceInfo",deviceInfo.toString());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + deviceInfo.getKsn(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "onGetCardNumber":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onGetCardNumber",responsePos.getCardNum());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " +responsePos.getCardNum(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "onGetReadCardInfo":
                    CardInfoEntity cardInfoEntity = (CardInfoEntity) responsePos.getObjResp();

                    System.out.println("***********cardInfoEntity*************");
                    System.out.print(cardInfoEntity);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onGetDeviceInfo",cardInfoEntity.getCardNumber());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + cardInfoEntity.getCardNumber(), Toast.LENGTH_SHORT).show();
                            log.setText(cardInfoEntity.getCardNumber());
                            pan.setText(cardInfoEntity.getCardNumber());
                            log.setText(cardInfoEntity.toString());
                            track2.setText(cardInfoEntity.getTrack2());
                        }
                    });
                    break;

                case "onGetReadInputInfo":

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onGetReadInputInfo",responsePos.getInputInfo());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getInputInfo(), Toast.LENGTH_SHORT).show();
                            log.setText(responsePos.getInputInfo());

                        }
                    });

                    break;

                case "onCancelReadCard":

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onCancelReadCard",responsePos.getMessage());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                            log.setText(responsePos.getMessage());
                        }
                    });

                    break;

                case "onGetEncryptData":
                    this.track2.setText(responsePos.getEncryptData());
                    break;

                case "onSetTransactionInfoSuccess":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onSetTransactionInfoSuccess",responsePos.getMessage());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "onAddAidSuccess":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onAddAidSuccess",responsePos.getMessage());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                            log.setText(responsePos.getMessage());
                        }
                    });
                    break;

                case "onAddRidSuccess":
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.d("onAddRidSuccess",responsePos.getMessage());
                            Toast.makeText(getApplicationContext(), "[MainActivity]: " + responsePos.getMessage(), Toast.LENGTH_SHORT).show();
                            log.setText(responsePos.getMessage());
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
        //getPos().continueAfterPin();
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