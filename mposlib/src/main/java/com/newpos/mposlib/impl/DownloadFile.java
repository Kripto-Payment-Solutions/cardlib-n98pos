package com.newpos.mposlib.impl;

import com.newpos.mposlib.api.UpdateCallBackInterface;
import com.newpos.mposlib.exception.SDKException;
import com.newpos.mposlib.util.LogUtil;
import com.newpos.mposlib.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class DownloadFile {


    private boolean isDownload = false;
    private boolean isExist = false;
    private int step;
    private int lastPro = 0;
    private int loadLen = 1000;//1000;//450;
    private int expectedOffset = 0;

    public DownloadFile() {

    }

    public void updateApp(final InputStream in, final byte type, final UpdateCallBackInterface updateCallback) {
        if (!isDownload) {
            try {
                if (in != null) {
                    byte[] dataToUpdate = getUpdateData(in);
                    isExist = false;
                    step = 1;
                    lastPro = 0;
                    expectedOffset = 0;
                    isDownload = true;
                    do {
                        downloadData(type, dataToUpdate, updateCallback);
                        if (step > 3) {
                            break;
                        }
                    } while (!isExist);
                    isDownload = false;
                } else if (updateCallback != null) {

                    updateCallback.onDownloadFail(01, "Input stream cannot be empty");
//                    updateCallback.onDownloadFail(01, "输入流不能为空");
                }
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
                isDownload = false;
                if (updateCallback != null) {
                    updateCallback.onDownloadFail(02, e.getMessage());
                }
            }
        }
    }


    private byte[] getUpdateData(InputStream in) {
        try {
            byte[] dataToUpdate = new byte[1024000];
            int readLen = 0;
            int totalRead = 0;
            while (readLen != -1) {
                byte[] readBytes = new byte[1024];
                readLen = in.read(readBytes, 0, 1024);
                if (readLen > 0) {
                    System.arraycopy(readBytes, 0, dataToUpdate, totalRead, readLen);
                    totalRead += readLen;
                }
            }
            byte[] total = new byte[totalRead];
            System.arraycopy(dataToUpdate, 0, total, 0, totalRead);
            dataToUpdate = total;
            in.close();
            return dataToUpdate;
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void downloadData(byte type, byte[] dataToUpdate, UpdateCallBackInterface callback) throws SDKException {
        byte[] result = null;
        switch (this.step) {
            case 1:
                result = Command.updateApp(type, (byte) 1, null, null, null);
                break;
            case 2:
                byte[] paramUpdate;
                byte[] offset = StringUtil.intToBytes2(this.expectedOffset);
                byte[] dataLen = StringUtil.intToBytes2(dataToUpdate.length);
                if (this.expectedOffset + this.loadLen > dataToUpdate.length) {
                    int len = dataToUpdate.length % this.loadLen;
                    paramUpdate = new byte[len];
                    System.arraycopy(dataToUpdate, this.expectedOffset, paramUpdate, 0, len);
                    this.expectedOffset += len;
                } else {
                    paramUpdate = new byte[this.loadLen];
                    System.arraycopy(dataToUpdate, this.expectedOffset, paramUpdate, 0, this.loadLen);
                    this.expectedOffset += this.loadLen;
                }
                result = Command.updateApp(type, (byte) 2, offset, paramUpdate, dataLen);
                break;
            case 3:
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA-1");
                } catch (NoSuchAlgorithmException e) {
                    if (LogUtil.DEBUG) {
                        e.printStackTrace();
                    }
                }
                md.update(dataToUpdate);
                result = Command.updateApp(type, (byte) 3, null, md.digest(), null);
                break;
        }
        if (result == null) {
            this.isExist = true;
        } else {
            dealResult(dataToUpdate, result, callback);
        }
    }

    private void dealResult(byte[] dataToUpdate, byte[] result, UpdateCallBackInterface callback) {
        byte type = result[0];
        if (!SDKException.CODE_SUCCESS.equals(new String(new byte[]{result[1], result[2]}))) {
            if (callback != null) {
                callback.onDownloadProgress(0, 0);
            }
            this.isExist = true;
        } else if (type != this.step) {
            int pro = this.expectedOffset / dataToUpdate.length;
            if (callback != null) {
                callback.onDownloadProgress(this.expectedOffset, dataToUpdate.length);
            }
        } else {
            if (this.step != 2) {
                this.step++;
            } else if (this.expectedOffset == dataToUpdate.length) {
                this.step++;
            } else {
                int progress = (this.expectedOffset * 100) / dataToUpdate.length;
                if (progress > this.lastPro) {
                    if (callback != null) {
                        callback.onDownloadProgress(this.expectedOffset, dataToUpdate.length);
                    }
                    this.lastPro = progress;
                }
            }
            if (this.step == 3 && callback != null) {
                callback.onDownloadComplete();
            }
        }
    }

}
