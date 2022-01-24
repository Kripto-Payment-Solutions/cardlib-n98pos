package com.newpos.mposlib.exception;

import com.newpos.mposlib.R;
import com.newpos.mposlib.util.ContextUtils;

public class SDKException extends Exception {
    public static final String CODE_SUCCESS = "00";
    public static final String ERR_CODE_CMD_NONSUPPORT = "01";
    public static final String ERR_CODE_COMMUNICATE_ERROR = "99";
    public static final String ERR_CODE_CTLSSLIMIT_PARAM_ERROR = "17";
    public static final String ERR_CODE_CUS_STATUS = "08";
    public static final String ERR_CODE_ENCRYPT_SN_FAIL = "14";
    public static final String ERR_CODE_FRAME_ERROR = "04";
    public static final String ERR_CODE_IC_FAIL = "13";
    public static final String ERR_CODE_IC_FAILBACK = "16";
    public static final String ERR_CODE_INSPECTION_SIGN_ERROR = "21";
    public static final String ERR_CODE_LRC_ERROR = "05";
    public static final String ERR_CODE_OTHER = "06";
    public static final String ERR_CODE_PARAM_ERROR = "02";
    public static final String ERR_CODE_RESET = "11";
    public static final String ERR_CODE_SN_KEY_NOT_EXIST = "15";
    public static final String ERR_CODE_TIME_OUT = "07";
    public static final String ERR_CODE_USER_CANCEL = "09";
    public static final String ERR_CODE_USE_IC = "12";
    public static final String ERR_CODE_VARIABLE_LENGHT = "03";
    public static final String SDK_ERR_CODE_PARAM_ERROR = "92";
    private static final long serialVersionUID = 1L;
    private String errCode;

    public SDKException(String paramString) {
        super(setErrMsg(paramString));
        this.errCode = paramString;
    }

    private static String setErrMsg(String paramString) {
        switch (Integer.valueOf(paramString).intValue()) {
            default:
                return "";
            case 0:
//                return "成功";
                return ContextUtils.getContext().getString(R.string.success);
            case 1:
//                return "指令码不支持";
                return ContextUtils.getContext().getString(R.string.err_code_cmd_nonsupport);
            case 2:
//                return "参数错误";
                return ContextUtils.getContext().getString(R.string.err_code_param_error);
            case 3:
//                return "可变数据域长度错误";
                return ContextUtils.getContext().getString(R.string.err_code_variable_lenght);
            case 4:
//                return "帧格式错误";
                return ContextUtils.getContext().getString(R.string.err_code_frame_error);

            case 5:
//                return "LRC校验失败";
                return ContextUtils.getContext().getString(R.string.err_code_lrc_error);

            case 6:
//                return "其他";
                return ContextUtils.getContext().getString(R.string.other);

            case 7:
//                return "超时，请重试";
                return ContextUtils.getContext().getString(R.string.err_code_time_out);

            case 8:
//                return "返回当前状态";
                return ContextUtils.getContext().getString(R.string.err_code_cus_status);

            case 9:
            case 10:
//                return "用户取消";
                return ContextUtils.getContext().getString(R.string.err_code_user_cancel);

            case 12:
//                return "当前系统状态不可用";
                return ContextUtils.getContext().getString(R.string.err_code_use_ic);

            case 13:
//                return "IC卡已拔出，执行失败";
                return ContextUtils.getContext().getString(R.string.err_code_ic_fail);

            case 14:
//                return "加密sn失败";
                return ContextUtils.getContext().getString(R.string.err_code_encrypt_sn_fail);

            case 15:
//                return "SN密钥不存在";
                return ContextUtils.getContext().getString(R.string.err_code_sn_key_not_exist);

            case 16:
//                return "IC卡操作错误";
                return ContextUtils.getContext().getString(R.string.err_code_ic_failback);

            case 17:
//                return "非接限额设置失败";
                return ContextUtils.getContext().getString(R.string.err_code_ctlsslimit_param_error);

            case 21:
//                return "文件验签失败";
                return ContextUtils.getContext().getString(R.string.err_code_inspection_sign_error);

            case 92:
//                return "参数错误";
                return ContextUtils.getContext().getString(R.string.sdk_err_code_param_error);

            case 99:
//                return "通讯异常";
                return ContextUtils.getContext().getString(R.string.err_code_communicate_error);
        }

    }

    public String getErrCode() {
        return this.errCode;
    }
}