package com.newpos.mposlib.api;

public abstract interface TimeManagerInteface
{
    public abstract void onTimeOut();

    public abstract void onTimeOutInterrupted(String paramString);
}