package com.newpos.mposlib.model;


import com.newpos.mposlib.util.StringUtil;

import java.util.Locale;

/**
 * communication message packet
 */
public class Packet {

    public static final byte ACK_NONE = 0x00;
    public static final byte ACK_AUTO = 0x01;//两端都要回复ACK
    public static final byte ACK_BIZ = 0x02;//由客户端业务自己确认消息是否到达

    public final static byte PACKET_HEAD = 0X02;
    public final static byte PACKET_TAIL = 0X03;

    /**
     * control bit
     */
    private byte control = 0x2F;

    /**
     * packet id 1~255
     */
    public byte packetID;

    /**
     * command
     */
    private byte[] command;

    /**
     * variable params
     */
    private byte[] params;

    private String respCode;

    private String strCommand;

    public String getStrCommand() {
        if (command != null) {
            if(strCommand == null) {
                strCommand = StringUtil.byte2HexStr(command);
            }
        }
        return strCommand;
    }

    public boolean needAck() {
        if ((control & 0x40) == 0x40) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isAck() {
        if (((control >> 4) & 0x01) == 0x01) {
            return true;
        } else {
            return false;
        }
    }

    public String getRespCode() {
        return this.respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }

    public byte getControl() {
        return control;
    }

    public void setControl(byte control) {
        this.control = control;
    }

    public byte getPacketID() {
        return packetID;
    }

    public void setPacketID(byte packetID) {
        this.packetID = packetID;
    }

    public byte[] getCommand() {
        return command;
    }

    public void setCommand(byte[] command) {
        this.command = command;
    }

    public byte[] getParams() {
        return params;
    }

    public void setParams(byte[] params) {
        this.params = params;
    }

    public byte[] packData() {
        int paramLen = 0;
        if (params != null && params.length > 0) {
            paramLen = params.length;
        }
        byte[] requestData = new byte[(9 + paramLen)];
        int index = 0;

        // STX
        requestData[0] = PACKET_HEAD;
        index++;
        byte[] dataLen = StringUtil.str2BCD(String.format(Locale.US, "%04d", new Object[]{Integer.valueOf(paramLen + 4)}));

        // 数据长度BCD
        requestData[index++] = dataLen[0];
        requestData[index++] = dataLen[1];

        // Command
        requestData[index++] = command[0];
        requestData[index++] = command[1];
        requestData[index++] = control;

        requestData[index++] = packetID;

        // 参数内容
        if (paramLen > 0) {
            System.arraycopy(params, 0, requestData, index, paramLen);
            index = paramLen + index;
        }

        // END flag
        requestData[index++] = PACKET_TAIL;

        // LRC校验值
        requestData[index] = StringUtil.calcLRC(requestData, 1, requestData.length - 1);


        return requestData;
    }
}
