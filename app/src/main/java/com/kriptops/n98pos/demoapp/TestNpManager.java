package com.kriptops.n98pos.demoapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kriptops.n98pos.cardlib.android.BluetoothApp;
import com.kriptops.n98pos.cardlib.constant.Constant;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.NpPosManager;
import com.newpos.mposlib.util.ISOUtil;

public class TestNpManager implements INpSwipeListener {

    private Context context;
    private NpPosManager posManager;
    private AppCompatActivity appActivity;

    public TestNpManager(Context context, AppCompatActivity appActivity) {
        this.context = context;
        this.appActivity = appActivity;
        posManager = NpPosManager.sharedInstance(this.context, this);
    }

    public boolean updateKeys(byte []encryptPIN, byte []PINkcv,
                              byte []encryptMAC, byte []MACkcv,
                              byte []encryptTrack, byte []TRACKkcv){
        try {
            posManager.updateWorkingKey(ISOUtil.byte2hex(encryptPIN)+ISOUtil.byte2hex(PINkcv,0,4),
                    ISOUtil.byte2hex(encryptMAC)+ISOUtil.byte2hex(MACkcv,0,4),
                    ISOUtil.byte2hex(encryptTrack)+ISOUtil.byte2hex(TRACKkcv,0,4));

            return posManager.isUpdateWorkKeys();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onScannerResult(BluetoothDevice devInfo) {

    }

    @Override
    public void onDeviceConnected() {

    }

    @Override
    public void onDeviceDisConnected() {

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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d("TestNpManager", "onDeviceDisConnected");
                Toast.makeText(appActivity, "CAR: " + context.getText(com.kriptops.n98pos.cardlib.R.string.device_update_work_key_error_desc), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 数据加密回调
     * **/
    @Override
    public void onGetEncryptData(String eData){};

    /**
     * 显示信息
     * **/
    @Override
    public void onDispMsgOnScreen(String disp){};
}
