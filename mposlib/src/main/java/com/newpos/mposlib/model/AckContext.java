package com.newpos.mposlib.model;

public class AckContext {
    public AckCallback callback;
    public int ackModel = Packet.ACK_NONE;
    public int timeout = 1000;
    public Packet request;
    public int retryCount;

    public static AckContext build(AckCallback callback) {
        AckContext context = new AckContext();
        context.setCallback(callback);
        return context;
    }

    public AckCallback getCallback() {
        return callback;
    }

    public AckContext setCallback(AckCallback callback) {
        this.callback = callback;
        return this;
    }

    public int getAckModel() {
        return ackModel;
    }

    public AckContext setAckModel(int ackModel) {
        this.ackModel = ackModel;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public AckContext setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Packet getRequest() {
        return request;
    }

    public AckContext setRequest(Packet request) {
        this.request = request;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public AckContext setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }
}
