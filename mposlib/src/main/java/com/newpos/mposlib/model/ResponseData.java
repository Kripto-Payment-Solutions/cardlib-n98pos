package com.newpos.mposlib.model;


public class ResponseData {

    private String respCode;

    /**
     * response packet
     */
    private Packet packet;
    public ResponseData(String respCode) {
        this.respCode = respCode;
    }

    public ResponseData(Packet packet) {
        this.packet = packet;
        respCode = packet.getRespCode();
    }

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }

    public String getRespCode() {
        return respCode;
    }

    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }


}
