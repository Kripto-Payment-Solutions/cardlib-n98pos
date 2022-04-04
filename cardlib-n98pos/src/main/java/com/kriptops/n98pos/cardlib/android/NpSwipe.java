package com.kriptops.n98pos.cardlib.android;

import android.bluetooth.BluetoothDevice;

import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;

public class NpSwipe implements INpSwipeListener {
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
