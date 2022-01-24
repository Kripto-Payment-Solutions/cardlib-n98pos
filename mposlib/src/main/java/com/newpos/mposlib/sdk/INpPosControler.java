package com.newpos.mposlib.sdk;

public interface INpPosControler {

    /**
     * 扫描外部所有蓝牙设备， 包括传统蓝牙设备， Android 支持蓝牙 4.0 设备扫描
     * @param timeOut 设置扫描超时时间,单位秒
     */
    void scanBlueDevice(int timeOut);

    /**
     * 立即停止扫描外部蓝牙设备
     */
    void stopScan();

    /**
     * 根据设备的 mac 地址连接设备
     * @param macAddr
     */
    void connectBluetoothDevice(String macAddr);

    /**
     * 判断手机与外设的连接状态
     * @return
     */
    boolean isConnected();

    /**
     * 将与手机连接的设备断开
     */
    void disconnectDevice();

    /**
     * 获取当前连接的设备信息
     */
    void getDeviceInfo();

    /**
     * 根据传入的公钥明文， 返回传输会话密钥密文
     * @param pubkey  RSA_1024公钥模
     */
    void getTransportSessionKey(String pubkey);

    /**
     *  更新主密钥
     * @param masterKey　主密钥密文 + KCV
     */
    void updateMasterKey(String masterKey);

    /**
     * 更新工作密钥
     * @param pinKey PIN密钥密文 + KCV
     * @param macKey MAC密钥密文 + KCV
     * @param trackKey 磁道密钥密文 + KCV
     */
    void updateWorkingKey(String pinKey, String macKey, String trackKey);

    /**
     * 清除所有AID
     */
    void clearAids();

    /**
     * 添加 AID 参数
     * @param aid
     */
    void addAid(String aid);

    /**
     * 清除所有公钥参数
     */
    void clearRids();

    /**
     * 添加 RID 参数
     * @param rid
     */
    void addRid(String rid);

    /**
     * 获取银行卡卡号
     * @param timeout
     */
    void getCardNumber(int timeout);

    /**
     * 获取设备当前电量状态
     */
    void getCurrentBatteryStatus();

    /**
     * 刷卡， 获取银行卡刷卡信息
     * @param cardReadEntity
     */
    void readCard(CardReadEntity cardReadEntity);

    /**
     * 输入交易金额和密码， 获取输入后的结果
     * @param entityInfoEntity
     */
    void getInputInfoFromKB(InputInfoEntity entityInfoEntity);

    /**
     * IC 数据回写， 二次认证
     * @param onLineData
     */
    void icCardWriteback(EMVOnlineData onLineData);

    /**
     * 取消交易
     */
    void cancelTrade();

    /**
     * 计算mac
     * @param macData
     */
    void calculateMac(String macData);

    /**
     * 升级固件
     * @param filePath
     */
    void updateFirmware(String filePath);

    /**
     * 在带键盘的设备屏幕上根据字符串生成一个二维码
     * @param keepShowTime 显示时间，单位秒
     * @param content      显示的文本
     */
    void generateQRCode(int keepShowTime, String content);

    /**
     * 在终端上保存的交易信息(批次号/流水号)
     * @param transactionInfo
     */
    void setTransactionInfo(String transactionInfo);

    /**
     * 获取终端上保存的交易信息(批次号/流水号)
     */
    void getTransactionInfo();

    /**
     * 显示文本
     * @param keepShowTime 显示时间，单位秒
     * @param content      显示的文本
     */
    void displayTextOnScreen(int keepShowTime, String content);
}
