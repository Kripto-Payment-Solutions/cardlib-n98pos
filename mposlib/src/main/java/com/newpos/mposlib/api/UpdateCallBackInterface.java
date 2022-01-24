package com.newpos.mposlib.api;

public interface UpdateCallBackInterface {

    void onDownloadComplete();

    void onDownloadFail(int error, String message);

    void onDownloadProgress(int download, int total);

}
