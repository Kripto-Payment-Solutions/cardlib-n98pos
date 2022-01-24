package com.newpos.mposlib.exception;

public class ConstantStr {
    public static final String DEFAULT_DATEPATTERN = "yyyyMMddHHmmss";

    public static class Tips {
        public static final String INPUT_AMOUNT = "请输入金额:";
        public static final String INPUT_PIN = "请输入密码:";
        public static final String PLS_SCAN = "\n请扫码";
        public static final String READ_CARD_TIPS = "请刷卡/插卡/挥卡";
    }

    public static class Error {
        public static final String NOT_OPEN_FILE = "文件未找到或打开失败";
        public static final String ENTER_UPDATE_FAIL = "进入升级模式失败";
        public static final String LOW_BATTERY = "电量不足";
    }
}
