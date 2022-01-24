package com.newpos.mposlib.model;

import com.newpos.mposlib.api.CommandCallback;

public class RequestData {

    private byte[] body;
    private CommandCallback callback;
    private int timeout;


    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public CommandCallback getCallback() {
        return callback;
    }

    public void setCallback(CommandCallback callback) {
        this.callback = callback;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
