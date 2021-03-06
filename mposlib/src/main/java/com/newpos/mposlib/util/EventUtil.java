package com.newpos.mposlib.util;

import org.greenrobot.eventbus.EventBus;

/**
 * Eventbus工具类
 */
public class EventUtil {
    //注册事件
    public static void register(Object context) {
        if (!EventBus.getDefault().isRegistered(context)) {
            EventBus.getDefault().register(context);
        }
    }

    //解除
    public static void unregister(Object context) {
        if (EventBus.getDefault().isRegistered(context)) {
            EventBus.getDefault().unregister(context);
        }
    }

    //发送消息
    public static void post(Object object) {
        EventBus.getDefault().post(object);
    }

}
