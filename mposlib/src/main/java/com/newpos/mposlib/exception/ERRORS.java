package com.newpos.mposlib.exception;

public class ERRORS {

    public static final int DEVICE_SCAN_ERROR = 1;
    public static final String DEVICE_SCAN_ERROR_DESC = "扫描设备错误";
    public static final int DEVICE_CONNECT_ERROR = 2;
    public static final String DEVICE_CONNECT_ERROR_DESC = "连接设备错误";
    public static final int DEVICE_DISCONNECT_ERROR = 3;
    public static final String DEVICE_DISCONNECT_ERROR_DESC = "断开设备错误";
    public static final int DEVICE_EXECPTION_DISCONNECT_ERROR = 4;
    public static final String DEVICE_EXECPTION_DISCONNECT_ERROR_DESC = "异常断开设备错误";
    public static final int DEVICE_GET_INFO_ERROR = 5;
    public static final String DEVICE_GET_INFO_ERROR_DESC = "获取设备信息错误";
    public static final int DEVICE_GET_TRANSPORT_SESSION_KEY_ERROR = 6;
    public static final String DEVICE_GET_TRANSPORT_SESSION_KEY_ERROR_DESC = "获取传输密钥错误";
    public static final int DEVICE_UPDATE_MASTER_KEY_ERROR = 7;
    public static final String DEVICE_UPDATE_MASTER_KEY_ERROR_DESC = "更新主密钥错误";
    public static final int DEVICE_UPDATE_WORK_KEY_ERROR = 8;
    public static final String DEVICE_UPDATE_WORK_KEY_ERROR_DESC = "更新工作密钥错误";
    public static final int DEVICE_ADD_AID_ERROR = 9;
    public static final String DEVICE_ADD_AID_ERROR_DESC = "添加AID错误";
    public static final int DEVICE_ADD_RID_ERROR = 10;
    public static final String DEVICE_ADD_RID_ERROR_DESC = "添加RID错误";
    public static final int DEVICE_GET_CARD_NO_ERROR = 11;
    public static final String DEVICE_GET_CARD_NO_ERROR_DESC = "获取卡号错误";
    public static final int DEVICE_GET_BATTERY_ERROR = 12;
    public static final String DEVICE_GET_BATTERY_ERROR_DESC = "获取电量状态错误";
    public static final int DEVICE_READ_CARD_ERROR = 13;
    public static final String DEVICE_READ_CARD_ERROR_DESC = "刷卡错误";

    public static final String DEVICE_GET_CARD_NO_CANCEL = "取消读卡";
    public static final String DEVICE_GET_CARD_NO_FAIL = "读卡失败";
    public static final String DEVICE_GET_CARD_NO_TIMEOUT = "读卡超时";

    public static final String DEVICE_READ_CARD_CANCEL = "取消刷卡";
    public static final String DEVICE_READ_CARD_FAIL = "刷卡失败";
    public static final String DEVICE_READ_CARD_TIMEOUT = "刷卡超时";

    public static final int DEVICE_INPUT_INFO_ERROR = 14;
    public static final String DEVICE_INPUT_INFO_ERROR_DESC = "输入信息错误";
    public static final String DEVICE_GET_AMOUNT_CANCEL = "取消输入金额";
    public static final String DEVICE_GET_AMOUNT_TIMEOUT = "输入金额超时";
    public static final int DEVICE_GET_PIN_ERROR = 15;
    public static final String DEVICE_GET_PIN_ERROR_DESC = "读取密码错误";
    public static final String DEVICE_GET_PIN_CANCEL = "取消输入密码";
    public static final String DEVICE_GET_PIN_TIMEOUT = "输入密码超时";
    public static final int DEVICE_IC_CARD_ONLINE_PROCESS_ERROR = 16;
    public static final String DEVICE_IC_CARD_ONLINE_PROCESS_ERROR_DESC = "二次授权错误";
    public static final int DEVICE_CANCEL_ERORR = 17;
    public static final String DEVICE_CANCEL_ERORR_DESC = "取消交易错误";
    public static final int DEVICE_CALC_MAC_ERROR = 18;
    public static final String DEVICE_CALC_MAC_ERROR_DESC = "计算Mac错误";
    public static final int DEVICE_UPDATE_FIRMWARE_ERROR = 19;
    public static final String DEVICE_UPDATE_FIRMWARE_ERROR_DESC = "固件升级错误";
    public static final int DEVICE_GENERATE_QRCODE_ERROR = 20;
    public static final String DEVICE_GENERATE_QRCODE_ERROR_DESC = "二维码生成错误";
    public static final int DEVICE_SAVE_TRADE_DATA_ERROR = 21;
    public static final String DEVICE_SAVE_TRADE_DATA_ERROR_DESC = "存储交易数据错误";
    public static final int DEVICE_GET_TRADE_DATA_ERROR = 22;
    public static final String DEVICE_GET_TRADE_DATA_ERROR_DESC = "获取交易数据错误";
    public static final int DEVICE_SHOW_TEXT_ERROR = 23;
    public static final String DEVICE_SHOW_TEXT_ERROR_DESC = "显示文本错误";

    public static final int DELETE_ALL_AID_ERROR = 24;
    public static final String DELETE_ALL_AID_ERROR_DESC = "删除AID参数错误";

    public static final int DELETE_ALL_RID_ERROR = 25;
    public static final String DELETE_ALL_RID_ERROR_DESC = "删除公钥参数错误";

    public static final String SDK_AID_FORMAT_ERROR_DESC = "AID内容错误";
    public static final String SDK_MASTER_KEY_FORMAT_ERROR_DESC = "主密钥格式错误";
    public static final String SDK_OPER_CANCEL_DESC = "操作取消";
    public static final String SDK_OPER_TIMEOUT_DESC = "操作超时";
    public static final String SDK_RID_FORMAT_ERROR_DESC = "RID内容错误";
    public static final String SDK_UPDATE_MAC_KEY_ERROR_DESC = "更新MAC密钥失败";
    public static final String SDK_UPDATE_PIN_KEY_ERROR_DESC = "更新PIN密钥失败";
    public static final String SDK_UPDATE_TRACK_KEY_ERROR_DESC = "更新TRACK密钥失败";
    public static final String SDK_WORK_KEY_FORMAT_ERROR_DESC = "工作密钥格式错误";

    // LCD tips


}
