package com.newpos.mposlib.util;


import android.content.Context;

public class ContextUtils {
    private static Context mContext;

    public static void init(Context context) {
        mContext = context;
    }

    public static Context getContext() {
        synchronized (ContextUtils.class) {
            if (mContext == null) {
                throw new NullPointerException("Context not is null");
            }
        }
        return mContext;
    }


}
