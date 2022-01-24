package com.newpos.mposlib.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {

    public final static int TIME_ONE = 1;
    public final static int TIME_SHORT = 3;
    public final static int TIME_NORMAL = 5;
    public final static int TIME_LONG = 10;

    /**
     * wait time
     * @param time second unit
     * @return
     */
    public static int getWaitTime(int time) {
        return (5 + time);
    }

    /**
     * "YYMMDD" "180703"
     * @return
     */
    public static String getCurrentDate() {
//        Date d = new Date();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String dateNowStr = sdf.format(d);
//        StringBuffer sb = new StringBuffer();
//        sb.append(dateNowStr.substring(2, 4));
//        sb.append(dateNowStr.substring(5, 7));
//        sb.append(dateNowStr.substring(8, 10));
//        return sb.toString();

        SimpleDateFormat simpleDate = new SimpleDateFormat("yyMMdd");
        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        simpleDate.setTimeZone(timeZone);
        return simpleDate.format(new Date());
    }
}
