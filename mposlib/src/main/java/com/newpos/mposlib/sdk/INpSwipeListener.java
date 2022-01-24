package com.newpos.mposlib.sdk;

import android.bluetooth.BluetoothDevice;


public interface INpSwipeListener {
    /**
     * 回调扫描到的蓝牙信息
     * @param devInfo
     */
    void onScannerResult(BluetoothDevice devInfo);

    /**
     * 蓝牙连接成功
     */
    void onDeviceConnected();

    /**
     * 蓝牙连接断开
     */
    void onDeviceDisConnected();

    /**
     * 回调获取设备信息结果
     * @param info 刷卡器信息
     */
    void onGetDeviceInfo(DeviceInfoEntity info);

    /**
     * 回调获取传输密钥结果
     * @param encryTransportKey
     */
    void onGetTransportSessionKey(String encryTransportKey);

    /**
     * 回调更新主密钥结果
     */
    void onUpdateMasterKeySuccess();

    /**
     * 回调更新工作密钥结果
     */
    void onUpdateWorkingKeySuccess();

    /**
     * 回调加载 AID 结果
     */
    void onAddAidSuccess();

    /**
     * 回调加载 RID 结果
     */
    void onAddRidSuccess();
    //清除AID参数
    void onClearAids();
    //清除公钥参数
    void onClearRids();

    /**
     * 回调获取卡号结果
     * @param cardNum 卡号
     */
    void onGetCardNumber(String cardNum);

    /**
     * 获取电量状态
     * result: true 电量充足 false 电量不足
     * @param result
     */
    void onGetDeviceBattery(boolean result);

    /**
     * 检测到 ic 卡插入
     */
    void onDetachedIC();

    /**
     * 获取刷卡信息
     * @param cardInfoEntity
     */
    void onGetReadCardInfo(CardInfoEntity cardInfoEntity);

    /**
     * 获取输入的金额和密码信息
     * @param inputInfo
     */
    void onGetReadInputInfo(String inputInfo);

    /**
     * 获取二次授权结果
     * @param result
     */
    void onGetICCardWriteback(boolean result);

    /**
     * 取消交易回调
     */
    void onCancelReadCard();

    /**
     * 获取计算 Mac 信息
     * @param encryMacData
     */
    void onGetCalcMacResult(String encryMacData);

    /**
     * 升级固件过程百分比
     * @param percent
     */
    void onUpdateFirmwareProcess(float percent);

    /**
     * 固件升级成功回调
     */
    void onUpdateFirmwareSuccess();

    /**
     * 生成二维码结果回调
     */
    void onGenerateQRCodeSuccess();

    /**
     * 存储交易数据结果回调
     */
    void onSetTransactionInfoSuccess();

    /**
     * 获取终端存储的交易数据
     * @param transactionInfo
     */
    void onGetTransactionInfoSuccess(String transactionInfo);

    /**
     * 显示文本回调
     */
    void onDisplayTextOnScreenSuccess();

    /**
     * 错误回调
     * @param error
     * @param message
     */
    void onReceiveErrorCode(int error, String message);
}
