package com.newpos.mposlib.model;

public interface AckCallback {
    void onSuccess(Packet response);

    void onTimeout(Packet request);
}
