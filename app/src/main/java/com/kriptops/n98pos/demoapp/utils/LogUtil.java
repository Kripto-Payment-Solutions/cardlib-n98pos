package com.kriptops.n98pos.demoapp.utils;


import android.text.TextUtils;
import android.util.Log;

/**
 * Log utility class.
 */

public class LogUtil {
    //TODO release set false
    public final static boolean DEBUG = true;
    private final static String customTagPrefix = "";
    /**
     * types for log level.
     */
    public enum Level {
        D,//Debug
        I,//Info
        W,//Waring
        E;//Error
    }

    /**
     * debug level for SDK Log printer.
     */
    public static void d(String tag, String msg) {
        if (DEBUG) {
            Log.d(tag, msg);
        }
    }

    /**
     * debug level for SDK Log printer.
     */
    public static void d(String content) {
        if (!DEBUG) return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.d(tag, content);
    }

    /**
     * info level for SDK Log printer.
     */
    public static void i(String tag, String msg) {
        if (DEBUG) {
            Log.i(tag, msg);
        }
    }

    /**
     * info level for SDK Log printer.
     */
    public static void i(String content) {
        if (!DEBUG) return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.i(tag, content);
    }

    /**
     * warning level for SDK Log printer.
     */
    public static void w(String tag, String msg) {
        if (!DEBUG) return;
        Log.w(tag, msg);
    }

    /**
     * info level for SDK Log printer.
     */
    public static void w(String content) {
        if (!DEBUG) return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.w(tag, content);
    }

    /**
     * error level for SDK Log printer.
     */
    public static void e(String tag, String msg) {
        if (!DEBUG) return;
        Log.e(tag, msg);
    }

    /**
     * info level for SDK Log printer.
     */
    public static void e(String content) {
        if (!DEBUG) return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content);
    }

    private static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    private static String generateTag(StackTraceElement caller) {
        String tag = "%s.%s(L:%d)";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(), caller.getLineNumber());
        tag = TextUtils.isEmpty(customTagPrefix) ? tag : customTagPrefix + ":" + tag;
        return tag;
    }
}
