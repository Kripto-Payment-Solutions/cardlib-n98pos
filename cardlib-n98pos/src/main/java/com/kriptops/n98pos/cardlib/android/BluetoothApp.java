package com.kriptops.n98pos.cardlib.android;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kriptops.n98pos.cardlib.R;
import com.kriptops.n98pos.cardlib.constant.Constant;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.NpPosManager;
import java.lang.ref.WeakReference;

public class BluetoothApp implements INpSwipeListener {
    protected ClientMessengerHandler mHandler;
    private Context context;
    private NpPosManager posManager;
    private AppCompatActivity appActivity;

    protected void handleMessageClient(Message msg){}

    public BluetoothApp(Context context, AppCompatActivity appActivity) {
        this.context = context;
        this.appActivity = appActivity;
        posManager = NpPosManager.sharedInstance(this.context, this);
        mHandler = new ClientMessengerHandler(this);
    }

    public void connectDevice(String macAddressN98){
        posManager.connectBluetoothDevice(macAddressN98);
    }

    public void disConnectDevice(){
        posManager.disconnectDevice();
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
                Toast.makeText(appActivity, context.getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
                //BluetoothActivity.this.finish();
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
                Toast.makeText(appActivity, context.getText(R.string.device_disconnect), Toast.LENGTH_SHORT).show();
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

    }

    protected static class ClientMessengerHandler extends Handler{

        private WeakReference<BluetoothApp> mActivity;

        public ClientMessengerHandler(BluetoothApp activity){
            mActivity = new WeakReference<BluetoothApp>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            System.out.println("[BluetoothActivity][ClientMessengerHandler] handleMessage: " + msg.toString());
            BluetoothApp activity = mActivity.get();
            if(activity != null){
                activity.handleMessageClient(msg);
            }
        }
    }
}
