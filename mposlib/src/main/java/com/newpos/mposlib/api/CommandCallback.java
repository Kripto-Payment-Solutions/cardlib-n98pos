package com.newpos.mposlib.api;


import com.newpos.mposlib.model.ResponseData;

public interface CommandCallback {

    void onResponse(ResponseData response);

    void onCancelled();
}
