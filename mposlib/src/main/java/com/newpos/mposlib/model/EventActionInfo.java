package com.newpos.mposlib.model;

import android.os.Bundle;

public class EventActionInfo {
    /**
     * 信息类型的唯一标识
     */
    private String action;

    /**
     * 基本的文本信息
     */
    public String stringValue;
    /**
     * int型变量
     */
    public int intValue;
    /**
     * 任意类型变量
     */
    public Object obj;
    /**
     * 传递Bundle对象
     */
    public Bundle bundle;

    private EventActionInfo() {
    }

    public EventActionInfo(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}

