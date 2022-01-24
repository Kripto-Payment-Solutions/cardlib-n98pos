package com.newpos.mposlib.thread;

import com.newpos.mposlib.api.TimeManagerInteface;

import java.util.Timer;
import java.util.TimerTask;

public class OutTimeManage {

    private static OutTimeManage self = null;
    private TimeManagerInteface callback;
    private Timer mTimer;
    private TimerTask mTimerTask;

    private OutTimeManage() {
        this.mTimer = null;
        this.mTimerTask = null;
        this.callback = null;
        this.mTimer = new Timer();
    }

    public static OutTimeManage getInstance() {
        if (self == null) {
            synchronized (OutTimeManage.class) {
                if (self == null) {
                    self = new OutTimeManage();
                }
            }
        }
        return self;
    }

    public void setCallBackListener(TimeManagerInteface callback) {
        this.callback = callback;
    }

    public synchronized void startTimeTask(int tmo) {
        if (tmo <= 0) {
            return;
        }

        if (this.mTimerTask != null) {
            this.mTimerTask.cancel();
            this.mTimer.purge();
            this.mTimerTask = null;
        }
        this.mTimerTask = new TimerTask() {
            public void run() {
                if (callback != null) {
                    callback.onTimeOut();
                }
            }
        };
        this.mTimer.schedule(this.mTimerTask, (long) (tmo * 1000));
    }

    public void cancelTask(String errCode) {
        this.callback.onTimeOutInterrupted(errCode);
        if (this.mTimerTask != null) {
            this.mTimerTask.cancel();
            this.mTimer.purge();
            this.mTimerTask = null;
        }
    }

    public void stopTask() {
        if (this.mTimerTask != null) {
            this.mTimerTask.cancel();
            this.mTimer.purge();
            this.mTimerTask = null;
        }
    }
}
