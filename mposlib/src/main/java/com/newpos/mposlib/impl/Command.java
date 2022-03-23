package com.newpos.mposlib.impl;

import android.util.Log;

import com.newpos.mposlib.BuildConfig;
import com.newpos.mposlib.api.CommandCallback;
import com.newpos.mposlib.bluetooth.BluetoothService;
import com.newpos.mposlib.exception.SDKException;
import com.newpos.mposlib.model.AckContext;
import com.newpos.mposlib.model.CalMacResponse;
import com.newpos.mposlib.model.DeviceSN;
import com.newpos.mposlib.model.InputPinResponse;
import com.newpos.mposlib.model.Packet;
import com.newpos.mposlib.model.RequestData;
import com.newpos.mposlib.model.ResponseCode;
import com.newpos.mposlib.model.ResponseData;
import com.newpos.mposlib.model.SwipeCardResponse;
import com.newpos.mposlib.model.TerminalInfo;
import com.newpos.mposlib.thread.NamedThreadFactory;
import com.newpos.mposlib.util.ISOUtil;
import com.newpos.mposlib.util.LogUtil;
import com.newpos.mposlib.util.StringUtil;
import com.newpos.mposlib.util.TimeUtils;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.newpos.mposlib.util.StringUtil.lenToLLVAR;
import static com.newpos.mposlib.util.StringUtil.llvarToLen;


public class Command {

    private static Command I;
    public static Command I() {
        if (I == null) {
            synchronized (Command.class) {
                if (I == null) {
                    I = new Command();
                }
            }
        }
        return I;
    }

    private static volatile int packetID = 0x01;
    public static final String DISPATCH_THREAD_NAME = "command-dispatch-t";
    private ThreadPoolExecutor dispatchThread;
    private final Executor executor = getDispatchThread();
    /**
     * hash map key:Command
     */
    private Map<String, RequestTask> queue = new ConcurrentHashMap<>();
    private final Callable<ResponseData> NONE = new Callable<ResponseData>() {
        @Override
        public ResponseData call() throws Exception {
            return null;
        }
    };

    public Future<ResponseData> add(String command, RequestData request) {
        //LogUtil.i("add:" + command);
        RequestTask task = new RequestTask(command, request);
        queue.put(command, task);
        task.future = timerThread.schedule(task, task.timeout, TimeUnit.MILLISECONDS);
        return task;
    }

    public RequestTask getAndRemove(String command) {
        //LogUtil.i("getAndRemove:" + command);
        return queue.remove(command);
    }

    /**
     * disconnect clear timer
     */
    public void clear() {
        if (!queue.isEmpty()) {
            for (RequestTask task : queue.values()) {
                try {
                    ResponseData responseData = new ResponseData(SDKException.ERR_CODE_COMMUNICATE_ERROR);
                    task.setResponse(responseData);
                } catch (Throwable e) {

                }
            }
            queue.clear();
        }
    }

    /**
     * time out thread
     */
    private ScheduledExecutorService timerThread = getTimerThread();

    /**
     * get timer thread from pool
     * @return
     */
    public ScheduledExecutorService getTimerThread() {
        if (timerThread == null || timerThread.isShutdown()) {
            timerThread = new ScheduledThreadPoolExecutor(1);
        }
        return timerThread;
    }


    public final class RequestTask extends FutureTask<ResponseData> implements Runnable {
        private CommandCallback callback;
        private final int timeout;
        private final long sendTime;
        private final String command;
        private Future<?> future;

        private RequestTask(String command, RequestData request) {
            super(NONE);
            this.callback = request.getCallback();
            this.timeout = request.getTimeout();
            this.sendTime = System.currentTimeMillis();
            this.command = command;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean success = super.cancel(mayInterruptIfRunning);
            if (success) {
                if (future.cancel(true)) {
                    queue.remove(command);
                    if (callback != null) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCancelled();
                            }
                        });
                        callback = null;
                    }
                }
            }
            LogUtil.d("one request task cancelled, command=" + command + " costTime=" +
                    (System.currentTimeMillis() - sendTime));
            return success;
        }

        @Override
        public void run() {
            queue.remove(command);
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_TIME_OUT);
            setResponse(responseData);
            //setResponse(null);
        }

        public void setResponse(ResponseData response) {
            if (future != null && this.future.cancel(true)) {
                this.set(response);
                if (callback != null) {
                    callback.onResponse(response);
                }
                callback = null;
            }
        }
    }

    public ThreadPoolExecutor getDispatchThread() {
        if (dispatchThread == null || dispatchThread.isShutdown()) {
            dispatchThread = new ThreadPoolExecutor(1, 3,
                    1L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(100),
                    new NamedThreadFactory(DISPATCH_THREAD_NAME),
                    new RejectedHandler());
        }
        return dispatchThread;
    }

    private static class RejectedHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LogUtil.w("Executor a task was rejected r=" + r);
        }
    }

    /**
     * parse packet
     * @param buffer
     * @return
     */
    public static Packet parsePacket(byte[] buffer) {
//        if(BuildConfig.DEBUG) {
//            LogUtil.d("parsePacket:" + StringUtil.byte2HexStr(buffer));
//        }
        do {
            if (buffer == null) {
                //LogUtil.d("check buffer null");
                break;
            }

            int length = buffer.length;
            if (length < 11) {
               // LogUtil.d("check length fail:" +length);
                break;
            }

            // check head
            if (buffer[0] != Packet.PACKET_HEAD) {
               // LogUtil.d("check head fail:" + buffer[0]);
                break;
            }

            // check tail
            if (buffer[length - 2] != Packet.PACKET_TAIL) {
               // LogUtil.d("check tail fail:" + buffer[length - 2]);
                break;
            }

            // check LRC
            if (!StringUtil.checkCommLRC(buffer, buffer[length - 1])) {
              //  LogUtil.d("checkCommLRC fail");
                break;
            }

            //real parse
            Packet packet = new Packet();
            int index = 0;
            byte head = buffer[index++];
            byte[] lenBytes = new byte[] {buffer[index++], buffer[index++]};
            int len = Integer.valueOf(StringUtil.byte2HexStr(lenBytes)).intValue();
            byte[] command = new byte[] {buffer[index++], buffer[index++]};
            packet.setCommand(command);
            byte control = buffer[index++];
            packet.setControl(control);
            byte packetID = buffer[index++];
            packet.setPacketID(packetID);

            String respCode = StringUtil.bytes2Ascii(new byte[]{buffer[index++], buffer[index++]});
            packet.setRespCode(respCode);

            int paramLen = len - 6;
            byte[] params = new byte[paramLen];
            System.arraycopy(buffer, index, params, 0, paramLen);
            packet.setParams(params);

            return packet;
        } while(false);

        return null;

    };

    private static void packCommand(Packet packet) throws SDKException {
        if (packetID > 255) {
            packetID = 1;
        }
        packet.setPacketID((byte)packetID);
        byte[] writeData = packet.packData();
        if (packet.needAck()) {
            AckContext ackContext = new AckContext();
            AckRequestMgr.I().add(packet.packetID, ackContext);
        }

        BluetoothService.I().writeData(writeData);
    }

    /**
     * pack message and wait response
     * @param packet
     * @param timeOut
     * @throws SDKException
     */
    private static Future<ResponseData> packCommandWithResponse(Packet packet, int timeOut) throws SDKException {

        if (packetID > 255) {
            packetID = 1;
        }
        packet.setPacketID((byte)packetID);
        packetID++;

        byte[] writeData = packet.packData();
        if (LogUtil.DEBUG) {
            String str = StringUtil.byte2HexStr(writeData);
            LogUtil.i(">> " + str);
        }

        timeOut = timeOut * StringUtil.SEC_MS;
        RequestData requestData = new RequestData();
        requestData.setTimeout(timeOut);
        Future<ResponseData> future = Command.I().add(packet.getStrCommand(), requestData);

        if (packet.needAck()) {
            AckContext ackContext = new AckContext();
            AckRequestMgr.I().add(packet.packetID, ackContext);
        }

        BluetoothService.I().writeData(writeData);

        return future;
    }

    public static void resetSomeCommand() {
        Command.RequestTask task = Command.I().getAndRemove("D101");
        if (task != null) {
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_RESET);
            task.setResponse(responseData);
        }

        task = Command.I().getAndRemove("1A01");
        if (task != null) {
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_RESET);
            task.setResponse(responseData);
        }

        task = Command.I().getAndRemove("D105");
        if (task != null) {
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_RESET);
            task.setResponse(responseData);
        }

        task = Command.I().getAndRemove("1C05");
        if (task != null) {
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_RESET);
            task.setResponse(responseData);
        }

        task = Command.I().getAndRemove("1C08");
        if (task != null) {
            ResponseData responseData = new ResponseData(SDKException.ERR_CODE_RESET);
            task.setResponse(responseData);
        }
    }


    public static boolean reset() throws SDKException {
        resetSomeCommand();

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1D, 0X08});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    /**
     * set mpos time
     * @throws SDKException
     */
    public static void setDeviceTime(String sysTime) throws SDKException {
        if (sysTime != null && sysTime.length() == 14) {
            byte[] data = StringUtil.str2bytesGBK(sysTime);
            Packet packet = new Packet();
            packet.setCommand(new byte[] {0X1D, 0X04});
            packet.setParams(data);
            Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    return;
                } else {
                    return;
                }
            } else {
                return;
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }

    }

    public static byte[] getDeviceInfo() throws SDKException {

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XF1, 0X23});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams();
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }
    public String KEK_KEY=null;
    public static String getTransportSessionKey(byte[] publicKey, byte[] exp) throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X21});

        byte[] params = new byte[(publicKey.length + 5)];
        byte[] publicKeyLen = lenToLLVAR(publicKey.length);
        System.arraycopy(publicKeyLen, 0, params, 0, publicKeyLen.length);
        System.arraycopy(publicKey, 0, params, 2, publicKey.length);
        System.arraycopy(exp, 0, params, publicKey.length + 2,3);
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                LogUtil.e("success");
                try {
                    StringBuffer sb = new StringBuffer();
                    params = responseData.getPacket().getParams();
                    LogUtil.e("params ="+ISOUtil.byte2hex(params));
                    int position = 0;
                    int keyLen = llvarToLen(params[position++], params[position++]);
                    if (keyLen > 0) {
                        byte[] buf = new byte[keyLen];
                        System.arraycopy(params, position, buf, 0, buf.length);
                        position += buf.length;
                        sb.append(StringUtil.byte2HexStr(buf));
                    }

                    byte[] kcv = new byte[4];
                    System.arraycopy(params, position, kcv, 0, kcv.length);
                    LogUtil.e("kcv ="+ISOUtil.byte2hex(kcv));
                    sb.append(StringUtil.byte2HexStr(kcv));
                    return sb.toString();
                } catch (Exception e) {
                    throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
                }
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static TerminalInfo getTerminalInfo() {
        try {
            byte[] info = getDeviceInfo();
            if (info == null) {
                return null;
            }
            LogUtil.e("getDeviceInfo =" + StringUtil.byteToStr(info));
            LogUtil.e("getDeviceInfo bytesToHexString=" + StringUtil.bytesToHexString(info));
            TerminalInfo terminalInfo = new TerminalInfo();

            int icount = 0;
            byte[] buf;
            int position = 0;
            for (icount = 1; icount < 20; icount++) {
                if (position == info.length) {
                    break;
                }
                int iLen = llvarToLen(info[position++], info[position++]);
                LogUtil.e("ilen =" + iLen);
                if (iLen <= 0) {
                    break;
                }
                switch (icount) {
                    case 1: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("setStrSN =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrSNCode(StringUtil.byteToStr(buf));
                        break;
                    }
                    case 2: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("setStrProductId =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrProductId(StringUtil.byteToStr(buf));
                        break;
                    }
                    case 3: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("setStrAppVersion =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrAppVersion(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 4: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("StrHardwareVer =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrHardwareVer(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 5: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("StrChipCode =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrChipCode(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 6: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("StrTerminalNum =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrTerminalNum(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 7: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("strCompanyId =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrCompanyId(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 8: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("StrTraceNum =" + StringUtil.byteToStr(buf));
                        terminalInfo.setStrTraceNum(StringUtil.byteToStr(buf));
                    }
                    break;
                    case 9: {
                        buf = new byte[iLen];
                        System.arraycopy(info, position, buf, 0, buf.length);
                        position += buf.length;
                        LogUtil.e("strBranchnum =" + StringUtil.byteToStr(buf));
                        terminalInfo.setstrBranchnum(StringUtil.byteToStr(buf));
                    }
                    break;
                    default:
                        buf = new byte[iLen];
                        position += buf.length;
                        break;

                }
            }
            return terminalInfo;
        } catch (SDKException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化记录文件
     * @param recordName
     * @param recordLen
     * @param search1offset
     * @param search1Len
     * @param search2offset
     * @param search2Len
     * @return
     * @throws SDKException
     */
    public static boolean initRecords(byte[] recordName, byte[] recordLen, byte[] search1offset, byte[] search1Len, byte[] search2offset, byte[] search2Len) throws SDKException {
        if (recordName == null || recordLen == null || search1Len == null || search1offset == null || search2Len == null || search2offset == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (recordLen.length == 2 && search1Len.length == 2 && search1offset.length == 2 && search2offset.length == 2 && search2Len.length == 2) {
            byte[] params = new byte[(recordName.length + 12)];
            byte[] recordNameLen = lenToLLVAR(recordName.length);
            System.arraycopy(recordNameLen, 0, params, 0, recordNameLen.length);
            int pos = 0 + recordNameLen.length;
            System.arraycopy(recordName, 0, params, pos, recordName.length);
            pos += recordName.length;
            System.arraycopy(recordLen, 0, params, pos, recordLen.length);
            pos += recordLen.length;
            System.arraycopy(search1offset, 0, params, pos, search1offset.length);
            pos += search1offset.length;
            System.arraycopy(search1Len, 0, params, pos, search1Len.length);
            pos += search1Len.length;
            System.arraycopy(search2offset, 0, params, pos, search2offset.length);
            System.arraycopy(search2Len, 0, params, pos + search2offset.length, search2Len.length);

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XC1, 0X01});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    return true;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
    }

    public static boolean addRecord(byte[] recordName, byte[] content) throws SDKException {
        if (recordName == null || content == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }

        byte[] params = new byte[((recordName.length + 4) + content.length)];
        byte[] recordNameLen = lenToLLVAR(recordName.length);
        System.arraycopy(recordNameLen, 0, params, 0, recordNameLen.length);
        int pos = recordNameLen.length;
        System.arraycopy(recordName, 0, params, pos, recordName.length);
        pos += recordName.length;
        byte[] len = lenToLLVAR(content.length);
        System.arraycopy(len, 0, params, pos, len.length);
        System.arraycopy(content, 0, params, pos + len.length, content.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XC1, 0X03});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                if(responseData.getPacket().getParams()[0] == 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean updateRecord(byte[] recordName, byte[] recordNo, byte[] search1, byte[] search2,
                                byte[] content) throws SDKException {
        if (recordName == null || content == null || recordNo == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (recordNo.length != 4) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else {
            int len = 0;
            if (search1 != null) {
                len += search1.length;
            }
            if (search2 != null) {
                len += search2.length;
            }

            byte[] params = new byte[(((recordName.length + 12) + content.length) + len)];
            byte[] recordNameLen = lenToLLVAR(recordName.length);
            System.arraycopy(recordNameLen, 0, params, 0, recordNameLen.length);
            int pos = 0 + recordNameLen.length;
            System.arraycopy(recordName, 0, params, pos, recordName.length);
            pos += recordName.length;
            System.arraycopy(recordNo, 0, params, pos, recordNo.length);
            pos += recordNo.length;
            byte[] search1len;
            if (search1 != null) {
                search1len = lenToLLVAR(search1.length);
                System.arraycopy(search1len, 0, params, pos, search1len.length);
                pos += search1len.length;
                System.arraycopy(search1, 0, params, pos, search1.length);
                pos += search1.length;
            } else {
                search1len = lenToLLVAR(0);
                System.arraycopy(search1len, 0, params, pos, search1len.length);
                pos += search1len.length;
            }
            byte[] search2len;
            if (search2 != null) {
                search2len = lenToLLVAR(search2.length);
                System.arraycopy(search2len, 0, params, pos, search2len.length);
                pos += search2len.length;
                System.arraycopy(search2, 0, params, pos, search2.length);
                pos += search2.length;
            } else {
                search2len = lenToLLVAR(0);
                System.arraycopy(search2len, 0, params, pos, search2len.length);
                pos += search2len.length;
            }
            byte[] contentLen = lenToLLVAR(content.length);
            System.arraycopy(contentLen, 0, params, pos, contentLen.length);
            System.arraycopy(content, 0, params, pos + contentLen.length, content.length);


            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XC1, 0X04});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    if(responseData.getPacket().getParams()[0] == 0) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        }
    }

    /**
     * 获取存储记录
     * @param recordName 记录名
     * @param recordNo 记录编号
     * @param search1 检索字段1
     * @param search2 检索字段2
     */
    public static byte[] readRecord(byte[] recordName, byte[] recordNo, byte[] search1,
                                    byte[] search2) throws SDKException  {
        if (recordName == null || recordNo == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (recordNo.length != 4) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else {
            int len = 0;
            if (search1 != null) {
                len = 0 + search1.length;
            }
            if (search2 != null) {
                len += search2.length;
            }

            byte[] params = new byte[((recordName.length + 10) + len)];
            byte[] recordNameLen = lenToLLVAR(recordName.length);
            System.arraycopy(recordNameLen, 0, params, 0, recordNameLen.length);
            int pos = recordNameLen.length;
            System.arraycopy(recordName, 0, params, pos, recordName.length);
            pos += recordName.length;
            System.arraycopy(recordNo, 0, params, pos, recordNo.length);
            pos += recordNo.length;
            byte[] search1len;
            if (search1 != null) {
                search1len = lenToLLVAR(search1.length);
                System.arraycopy(search1len, 0, params, pos, search1len.length);
                pos += search1len.length;
                System.arraycopy(search1, 0, params, pos, search1.length);
                pos += search1.length;
            } else {
                search1len = lenToLLVAR(0);
                System.arraycopy(search1len, 0, params, pos, search1len.length);
                pos += search1len.length;
            }
            byte[] search2len;
            if (search2 != null) {
                search2len = lenToLLVAR(search2.length);
                System.arraycopy(search2len, 0, params, pos, search2len.length);
                pos += search2len.length;
                System.arraycopy(search2, 0, params, pos, search2.length);
                pos += search2.length;
            } else {
                search2len = lenToLLVAR(0);
                System.arraycopy(search2len, 0, params, pos, search2len.length);
                pos += search2len.length;
            }

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XC1, 0X05});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    byte[] result = responseData.getPacket().getParams();
                    int contentLen = llvarToLen(result[0], result[1]);
                    if (contentLen <= 0) {
                        return null;
                    }
                    byte[] content = new byte[contentLen];
                    System.arraycopy(result, 2, content, 0, content.length);
                    return content;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        }
    }

    public static byte[] getRecordNumber(byte[] recordName) throws SDKException {
        if (recordName == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(recordName.length + 2)];
        byte[] len = lenToLLVAR(recordName.length);
        System.arraycopy(len, 0, params, 0, len.length);
        System.arraycopy(recordName, 0, params, len.length, recordName.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XC1, 0X02});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                return result;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] loadMasterKey(byte KEKType, byte index, byte[] masterKey,
                                byte checkMode, byte[] checkValue) throws SDKException {
        if (masterKey == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[((masterKey.length + 7) + checkValue.length)];
        int pos = 0;
        params[pos++] = KEKType;
        params[pos++] = index;
        byte[] dataLen = lenToLLVAR(masterKey.length);
        params[pos++] = dataLen[0];
        params[pos++] = dataLen[1];
        System.arraycopy(masterKey, 0, params, pos, masterKey.length);
        pos += masterKey.length;
        params[pos++] = checkMode;
        byte[] valueLen = lenToLLVAR(checkValue.length);
        params[pos++] = valueLen[0];
        params[pos++] = valueLen[1];
        System.arraycopy(checkValue, 0, params, pos, checkValue.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X02});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                if (result[0] == 0) {
                    byte[] cv = new byte[8];
                    //System.arraycopy(result, 1, cv, 0, cv.length);
                    return cv;
                }  else {
                    return null;
                }
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] loadWorkKey(byte KeyType, byte mKeyIndex, byte wKeyIndex, byte[] wKey,
                                     byte checkMode, byte[] checkValue) throws SDKException {
        if (wKey == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        //LogUtil.d("wKey:" + StringUtil.byte2HexStr(wKey));
        //LogUtil.d( "checkValue:" + StringUtil.byte2HexStr(checkValue));
        byte[] params = new byte[((wKey.length + 8) + checkValue.length)];
        int pos = 0;
        params[pos++] = KeyType;
        params[pos++] = mKeyIndex;
        params[pos++] = wKeyIndex;

        byte[] dataLen = lenToLLVAR(wKey.length);
        params[pos++] = dataLen[0];
        params[pos++] = dataLen[1];
        System.arraycopy(wKey, 0, params, pos, wKey.length);
        pos += wKey.length;
        params[pos++] = checkMode;
        byte[] valueLen = lenToLLVAR(checkValue.length);
        params[pos++] = valueLen[0];
        params[pos++] = valueLen[1];
        System.arraycopy(checkValue, 0, params, pos, checkValue.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X05});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                if (result[0] == 0) {
                    byte[] cv = new byte[8];
                    //System.arraycopy(result, 1, cv, 0, cv.length);
                    return cv;
                }  else {
                    throw new SDKException(StringUtil.byte2HexStr(new byte[] {result[0]}));
                }
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte isWorkKeyExist(byte index, byte keyType) throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X1B});
        packet.setParams(new byte[] {index,  keyType});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                return result[1];
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] getRandom() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XF1, 0X02});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                return result;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] openCardReader(byte type, byte tmo, byte[] message, byte fallback) throws SDKException {
        if (message == null) {
            throw new SDKException(SDKException.ERR_CODE_FRAME_ERROR);
        }
        byte[] params = new byte[((message.length + 4) + 1)];
        int pos = 0;
        params[pos++] = type;
        params[pos++] = tmo;
        byte[] valueLen = lenToLLVAR(message.length);
        params[pos++] = valueLen[0];
        params[pos++] = valueLen[1];
        System.arraycopy(message, 0, params, pos, message.length);
        pos += message.length;
        params[pos++] = fallback;

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XD1, 0X01});//D101
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                return result;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] openCardReaderAndShowQRcode(byte type, byte tmo, byte[] qrcodeDec,
                                                     byte[] x, byte[] y, byte[] qrCodeHeight,
                                                     byte[] qrCodeWidth, byte[] qrcodeData) throws SDKException {
        if (x == null || y == null || qrCodeHeight == null || qrCodeWidth == null || qrcodeData == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (x.length == 2 && y.length == 2 && qrCodeHeight.length == 2 && qrCodeWidth.length == 2) {
            byte[] params;
            if (qrcodeDec != null) {
                params = new byte[((qrcodeData.length + qrcodeDec.length) + 14)];
            } else {
                params = new byte[(qrcodeData.length + 14)];
            }
            int pos = 0;
            params[pos++] = type;
            params[pos++] = tmo;
            if (qrcodeDec != null) {
                byte[] qrcodeDecLen = lenToLLVAR(qrcodeDec.length);
                System.arraycopy(qrcodeDecLen, 0, params, pos, qrcodeDecLen.length);
                pos += qrcodeDecLen.length;
                System.arraycopy(qrcodeDec, 0, params, pos, qrcodeDec.length);
                pos += qrcodeDec.length;
            } else {
                params[pos++] = (byte) 0;
                params[pos++] = (byte) 0;
            }
            System.arraycopy(x, 0, params, pos, x.length);
            pos += x.length;
            System.arraycopy(y, 0, params, pos, y.length);
            pos += y.length;
            System.arraycopy(qrCodeHeight, 0, params, pos, qrCodeHeight.length);
            pos += qrCodeHeight.length;
            System.arraycopy(qrCodeWidth, 0, params, pos, qrCodeWidth.length);
            pos += qrCodeWidth.length;
            byte[] qrcodeDataLen = lenToLLVAR(qrcodeData.length);
            System.arraycopy(qrcodeDataLen, 0, params, pos, qrcodeDataLen.length);
            System.arraycopy(qrcodeData, 0, params, pos + qrcodeDataLen.length, qrcodeData.length);

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XD1, 0X08});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    byte[] result = responseData.getPacket().getParams();
                    return result;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
    }

    public static DeviceSN getDeviceSn(byte[] cardPad) throws SDKException {

        byte[] params = new byte[(cardPad.length + 2)];
        byte[] qrcodeDecLen = lenToLLVAR(cardPad.length);
        int pos = 0;
        System.arraycopy(qrcodeDecLen, 0, params, pos, qrcodeDecLen.length);
        pos += qrcodeDecLen.length;
        System.arraycopy(cardPad, 0, params, pos, cardPad.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XF1, 0X22});
        packet.setParams(params);

        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                DeviceSN data = new DeviceSN();
                pos = 0;
                int tusnLen = llvarToLen(result[pos++], result[pos++]);
                if (tusnLen > 0) {
                    byte[] tusn = new byte[tusnLen];
                    System.arraycopy(result, pos, tusn, 0, tusnLen);
                    pos += tusnLen;
                    data.setTusn(StringUtil.byteToStr(tusn));
                }

                int encryptTusnLen = llvarToLen(result[pos++], result[pos++]);
                if (encryptTusnLen > 0) {
                    byte[] encryptTusn = new byte[encryptTusnLen];
                    System.arraycopy(result, pos, encryptTusn, 0, encryptTusnLen);
                    pos += encryptTusnLen;
                    data.setEncryptTusn(StringUtil.byte2HexStr(encryptTusn));
                }

                byte deviceType = result[pos++];
                data.setDeviceType(String.format("%02d", deviceType));
//                byte[] random = new byte[8];
//                System.arraycopy(result, pos, random, 0, random.length);
                return data;
            } else {
                LogUtil.e("responseData ="+responseData.getRespCode());
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static SwipeCardResponse readTrackDataWithUnencrypted(byte type) throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XD1, 0X04});
        packet.setParams(new byte[]{type});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                LogUtil.e("card msg =" + StringUtil.byteToStr(result));
                LogUtil.e("card msg bytesToHexString=" + StringUtil.bytesToHexString(result));
                SwipeCardResponse data = new SwipeCardResponse();

                int pos = 0;
                int iLen ;
                int icount =0;
                for(icount =1;icount <10;icount++){
                    iLen = llvarToLen(result[pos++], result[pos++]);
                    if(iLen <=0){
                        break;
                    }
                    byte[] tempData = new byte[iLen];
                    switch(icount){
                        case 1:
                            System.arraycopy(result, pos, tempData, 0,iLen);
                            LogUtil.e("Pan: "+ISOUtil.byte2hex(tempData));
                            pos += iLen;
                            data.setPan(StringUtil.byte2HexStr(tempData).toUpperCase().replace("F", ""));
                            break;
                        case 2:
                            System.arraycopy(result, pos, tempData, 0, iLen);
                            LogUtil.e("track1: "+StringUtil.byteToStr(tempData));
                            pos += iLen;
                            if(tempData[0] !=0xff){
                                data.setOneTrack(StringUtil.byteToStr(tempData));
                            }
                            break;
                        case 3:
                            System.arraycopy(result, pos, tempData, 0, iLen);
                            pos += iLen;
                            String unEncTrack2Data =StringUtil.byteToStr(tempData);
                            LogUtil.e("track2: "+unEncTrack2Data);

                            data.setTwoTrack(unEncTrack2Data);

                            int index = unEncTrack2Data.indexOf("D");
                            if (index != -1) {
                                data.setExpiryDate(unEncTrack2Data.substring(index + 1, index + 5));
                            }
                            break;
                        case 4:
                            System.arraycopy(result, pos, tempData, 0, iLen);
                            pos += iLen;
                            String tdk3 = StringUtil.byteToStr(tempData);
                            String unEncTrack3Data =ISOUtil.byte2hex(tempData);
                            LogUtil.e("track 3 ="+unEncTrack3Data);
                            LogUtil.e("tdk3="+tdk3);
                            data.setThreeTrack(unEncTrack3Data);
                            break;
                        case 5:
                            System.arraycopy(result, pos, tempData, 0, iLen);
                            pos += iLen;
                            String servicecode = StringUtil.byteToStr(tempData);
                            data.setTrack2Servicecode(servicecode);
                            break;
                        case 6:
                        case 7:
                        case 8:
                        default:
                            break;
                    }
                    data.setCardType(SwipeCardResponse.CardType.TRACK);
                    if (pos == result.length) {
                        break;
                    }
                }
                return data;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }


    public static SwipeCardResponse readTrackDataWithEncrypted(int tmo, byte PKeyIndex, byte type,
                                                               byte[] panShieldMask,
                                                               byte encryptArithmeticMark,
                                                               byte keyIndex, byte[] key,
                                                               byte[] random, byte[] batchNo,
                                                               byte arithmeticType, byte[] extras,
                                                               String isDispCardNo) throws SDKException {
        if (panShieldMask == null || random == null || batchNo == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (panShieldMask.length == 10 && random.length == 8 && batchNo.length == 12) {
            byte[] params;
            byte[] len;
            int paramsLen = 0;
            if (key != null) {
                paramsLen = 0 + key.length;
            }
            if (extras != null && encryptArithmeticMark == (byte) 7) {
                paramsLen += extras.length;
            }
            if (encryptArithmeticMark != (byte) 7) {
                params = new byte[((((panShieldMask.length + 8) + random.length) + batchNo.length) + paramsLen)];
            } else {
                params = new byte[((((panShieldMask.length + 10) + random.length) + batchNo.length) + paramsLen)];
            }
            int pos = 0;
            params[pos++] = PKeyIndex;
            params[pos++] = type;
            System.arraycopy(panShieldMask, 0, params, pos, panShieldMask.length);
            pos += panShieldMask.length;
            params[pos++] = encryptArithmeticMark;
            params[pos++] = keyIndex;
            if (key == null) {
                len = lenToLLVAR(0);
                System.arraycopy(len, 0, params, pos, len.length);
                pos += len.length;
            } else {
                len = lenToLLVAR(key.length);
                System.arraycopy(len, 0, params, pos, len.length);
                pos += len.length;
                System.arraycopy(key, 0, params, pos, key.length);
                pos += key.length;
            }
            System.arraycopy(random, 0, params, pos, random.length);
            pos += random.length;
            System.arraycopy(batchNo, 0, params, pos, batchNo.length);
            pos += batchNo.length;
            params[pos++] = arithmeticType;
            if (extras == null || encryptArithmeticMark != (byte) 7) {

            } else {
                len = lenToLLVAR(extras.length);
                System.arraycopy(len, 0, params, pos, len.length);
                pos += len.length;
                System.arraycopy(extras, 0, params, pos, extras.length);
                pos += extras.length;
            }
            params[pos++] = isDispCardNo.equals("Y") ? (byte) 0 : (byte) 1;

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XD1, 0X05});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    byte[] result = responseData.getPacket().getParams();
                    SwipeCardResponse data = new SwipeCardResponse();
                    int p = 0 + 1;
                    byte[] pan = new byte[10];
                    System.arraycopy(result, p, pan, 0, 10);
                    p += 10;
                    data.setPan(StringUtil.byte2HexStr(pan).replace("F", ""));
                    byte[] panSha = new byte[20];
                    System.arraycopy(result, p, panSha, 0, panSha.length);
                    p += panSha.length;
                    data.setPanHash(StringUtil.byte2HexStr(panSha));
                    p++;
                    int oneLen = llvarToLen(result[p++], result[p++]);
                    data.setTrack1Length(oneLen);
                    if (oneLen > 0) {
                        byte[] one = new byte[oneLen];
                        System.arraycopy(result, p, one, 0, oneLen);
                        p += oneLen;
                        data.setOneTrack(StringUtil.byte2HexStr(one));
                    }

                    int twiceLen = llvarToLen(result[p++], result[p++]);
                    data.setTrack2Length(twiceLen);
                    if (twiceLen > 0) {
                        byte[] twice = new byte[twiceLen];
                        System.arraycopy(result, p, twice, 0, twiceLen);
                        p += twiceLen;
                        data.setTwoTrack(StringUtil.byte2HexStr(twice));
                    }
                    int threeLen =  llvarToLen(result[p++], result[p++]);
                    data.setTrack3Length(threeLen);
                    if (threeLen > 0) {
                        byte[] three = new byte[threeLen];
                        System.arraycopy(result, p, three, 0, threeLen);
                        p += threeLen;
                        data.setThreeTrack(StringUtil.byte2HexStr(three));
                    }
                    byte[] twiceExp = new byte[7];
                    System.arraycopy(result, p, twiceExp, 0, twiceExp.length);
                    p += twiceExp.length;
                    byte[] expiryDate = new byte[4];
                    System.arraycopy(twiceExp, 0, expiryDate, 0, 4);
                    data.setCardType(SwipeCardResponse.CardType.TRACK);
                    data.setServiceCode(((char) twiceExp[4]) + "");
                    if (twiceExp[4] == (byte) 50 || twiceExp[4] == (byte) 54) {
                        data.setCardType(SwipeCardResponse.CardType.IC);
                    }
                    data.setExpiryDate(StringUtil.byteToStr(expiryDate));
                    byte[] ksn = new byte[10];
                    System.arraycopy(result, p, ksn, 0, ksn.length);
                    p += ksn.length;
                    data.setKSN(StringUtil.byte2HexStr(ksn));
                    data.setRandomT(StringUtil.byte2HexStr(ksn).substring(0, 16));
                    int extraLen = llvarToLen(result[p++], result[p++]);
                    if (extraLen > 0) {
                        byte[] extra = new byte[extraLen];
                        System.arraycopy(result, p, extra, 0, extraLen);
                        pos += extraLen;
                        data.setExtras(StringUtil.byte2HexStr(extra));
                        pos += extraLen;
                    }
                    return data;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
    }

    public static byte[] executeStandardProcess(int tmo, byte[] tlv) throws SDKException {
        if (tlv == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(tlv.length + 2)];
        byte[] tlvLen = lenToLLVAR(tlv.length);
        params[0] = tlvLen[0];
        params[1] = tlvLen[1];
        System.arraycopy(tlv, 0, params, 2, tlv.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X05});
        packet.setParams(params);

        Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                int len = llvarToLen(result[0], result[1]);
                byte[] respTLV =  new byte[len];
                System.arraycopy(result, 2, respTLV, 0, respTLV.length);
                return respTLV;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean endStandardProcess() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X07});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] twiceAuthorization(byte[] respCode, byte[] tlv, int tmo) throws SDKException {
        if (tlv == null || respCode == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(tlv.length + 4)];
        params[0] = respCode[0];
        params[1] = respCode[1];

        byte[] tlvLen = lenToLLVAR(tlv.length);
        params[2] = tlvLen[0];
        params[3] = tlvLen[1];
        System.arraycopy(tlv, 0, params, 4, tlv.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X06});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                byte[] tlvDatas = new byte[llvarToLen(result[0], result[1])];
                System.arraycopy(result, 2, tlvDatas, 0, tlvDatas.length);
                return tlvDatas;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static String inputAmount(byte tmo, byte[] displayData) throws SDKException {
        byte[] params = new byte[(displayData.length + 3)];
        int pos = 0;
        params[pos++] = tmo;
        byte[] disl = lenToLLVAR(displayData.length);
        params[pos++] = disl[0];
        params[pos++] = disl[1];
        System.arraycopy(displayData, 0, params, pos, displayData.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XA1, 0X21});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                byte[] amount =  new byte[6];
                System.arraycopy(result, 0, amount, 0, amount.length);
                return StringUtil.byte2HexStr(amount);
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static InputPinResponse inputPin(byte index, byte keyType,
                                            byte[] pan, byte[] keyLenLimit,
                                            byte isUseEnterKey, byte tmo,
                                            byte[] displayData) throws SDKException {
        if (pan == null || displayData == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else {
            int disLen;
            int keyLen;
            if( keyLenLimit == null) {
                keyLen = 0;
            } else {
                keyLen = keyLenLimit.length;
            }

            if (displayData == null) {
                disLen = 0;
            } else {
                disLen = displayData.length;
            }
            byte[] params = new byte[((keyLen + 10) + disLen) + pan.length];
            int pos = 0;
            params[pos++] = index;
            params[pos++] = keyType;

            byte[] len = lenToLLVAR(pan.length);
            params[pos++] = len[0];
            params[pos++] = len[1];
            System.arraycopy(pan,0, params, pos, pan.length);
            pos += pan.length;

            len = lenToLLVAR(keyLen);
            params[pos++] = len[0];
            params[pos++] = len[1];
            if (keyLenLimit != null) {
                System.arraycopy(keyLenLimit, 0, params, pos, keyLenLimit.length);
                pos += keyLenLimit.length;
            }

            params[pos++] = isUseEnterKey;
            params[pos++] = tmo;
            byte[] disl = lenToLLVAR(disLen);
            params[pos++] = disl[0];
            params[pos++] = disl[1];
            System.arraycopy(displayData, 0, params, pos, displayData.length);

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0X1A, 0X01});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    byte[] result = responseData.getPacket().getParams();
                    InputPinResponse respond = new InputPinResponse();
                    int p = 0;
                    respond.setKeyID(result[p++]);
                    respond.setPinLen(result[p++]);
                    byte[] encryptedData = new byte[8];
                    System.arraycopy(result, p, encryptedData, 0, encryptedData.length);
                    p += encryptedData.length;
                    String pinBlock = StringUtil.byte2HexStr(encryptedData);
                    if ("FFFFFFFFFFFFFFFF".equals(pinBlock)) {
                        pinBlock = "";
                    }

                    respond.setEncryptedData(pinBlock);
//                    byte[] KSN = new byte[10];
//                    System.arraycopy(result, p, KSN, 0, KSN.length);
//                    respond.setKSN(StringUtil.byte2HexStr(KSN));

                    return respond;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        }
    }

    public static CalMacResponse calMAC(byte keyIndex, byte keyType, byte MACType,
                                        byte[] data) throws SDKException {
        if (data == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(data.length + 5)];
        int pos = 0;
        params[pos++] = keyIndex;
        params[pos++] = keyType;
        params[pos++] = MACType;
        byte[] dataLen = lenToLLVAR(data.length);
        params[pos++] = dataLen[0];
        params[pos++] = dataLen[1];
        System.arraycopy(data, 0, params, pos, data.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X04});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet,TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();

                System.arraycopy(result, 0, new byte[1], 0, 1);
                byte[] mac = new byte[8];
                System.arraycopy(result, 0 + 1, mac, 0, mac.length);
//                byte[] ksn = new byte[10];
//                System.arraycopy(result, mac.length + 1, ksn, 0, ksn.length);
                CalMacResponse calMacRespond = new CalMacResponse();
                calMacRespond.setMAC(new String(mac));
                //calMacRespond.setKSN(StringUtil.byte2HexStr(ksn));
                return calMacRespond;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] executeQPBOCStandardProcess(int tmo, byte[] tlv) throws SDKException {
        if (tlv == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(tlv.length + 2)];
        byte[] tlvLen = lenToLLVAR(tlv.length);
        params[0] = tlvLen[0];
        params[1] = tlvLen[1];
        System.arraycopy(tlv, 0, params, 2, tlv.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X08});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, tmo + TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                byte[] _tlv = new byte[llvarToLen(result[0], result[1])];
                System.arraycopy(result, 2, _tlv, 0, _tlv.length);
                return _tlv;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static String getDataDES(byte index, byte encType, byte mode, byte[] data, byte[] checkValue) throws SDKException {
        if (data == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
        byte[] params = new byte[(data.length + 5)];
        int pos = 0;
        params[pos++] = index;
        params[pos++] = encType;
        params[pos++] = mode;
        byte[] dataLen = lenToLLVAR(data.length);
        params[pos++] = dataLen[0];
        params[pos++] = dataLen[1];
        System.arraycopy(data, 0, params, pos, data.length);

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1A, 0X03});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                byte[] result = responseData.getPacket().getParams();
                if (result != null && result[0] == (byte)0) {
                    byte[] enc = new byte[llvarToLen(result[1], result[2])];
                    System.arraycopy(result, 3, enc, 0, enc.length);
                    return StringUtil.byte2HexStr(enc);
                }
                throw new SDKException(String.valueOf(result[0]));
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }


    public static boolean operatePubicKey(byte type, byte[] pkData) throws SDKException {
        byte[] params;
        if (pkData != null) {
            params = new byte[(pkData.length + 3)];
            params[0] = type;
            byte[] len = lenToLLVAR(pkData.length);
            params[1] = len[0];
            params[2] = len[1];
            System.arraycopy(pkData, 0, params, 3, pkData.length);
        } else {
            params = new byte[]{type};
        }

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X01});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean operateAID(byte type, byte[] AIDData) throws SDKException {
        byte[] params;
        byte[] cmd = new byte[]{(byte) 28, (byte) 2};
        if (AIDData != null) {
            params = new byte[(AIDData.length + 3)];
            params[0] = type;
            byte[] len = lenToLLVAR(AIDData.length);
            params[1] = len[0];
            params[2] = len[1];
            System.arraycopy(AIDData, 0, params, 3, AIDData.length);
        } else {
            params = new byte[]{type};
        }
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X02});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] checkEmvFile() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1C, 0X10});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams();
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean setCursorPosition(int x, int y) throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XA1, 0X04});
        byte[] params =  new byte[]{(byte) ((x >> 8) & 255), (byte) (x & 255), (byte) ((y >> 8) & 255), (byte) (y & 255)};
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean showStrScreen(byte[] msg, int showTime) throws SDKException {
        byte[] params = new byte[(msg.length + 3)];
        byte[] msgLen = lenToLLVAR(msg.length);
        int i = 0;
        params[i++] = msgLen[0];
        params[i++] = msgLen[1];
        System.arraycopy(msg, 0, params, i, msg.length);
        i += msg.length;
        params[i] = (byte) showTime;

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XA1, 0X0E});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, showTime + TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return true;
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static boolean showQRCode(byte[] x, byte[] y, byte[] qrCodeHeight, byte[] qrCodeWidth,
                              byte time, byte[] qrcodeDec, byte[] qrcodeData) throws SDKException {
        if (x == null || y == null || qrCodeHeight == null || qrCodeWidth == null || qrcodeData == null) {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        } else if (x.length == 2 && y.length == 2 && qrCodeHeight.length == 2 && qrCodeWidth.length == 2) {
            byte[] params;
            if (qrcodeDec != null) {
                params = new byte[((qrcodeData.length + qrcodeDec.length) + 13)];
            } else {
                params = new byte[(qrcodeData.length + 13)];
            }
            System.arraycopy(x, 0, params, 0, x.length);
            int pos = 0 + x.length;
            System.arraycopy(y, 0, params, pos, y.length);
            pos += y.length;
            System.arraycopy(qrCodeHeight, 0, params, pos, qrCodeHeight.length);
            pos += qrCodeHeight.length;
            System.arraycopy(qrCodeWidth, 0, params, pos, qrCodeWidth.length);
            pos += qrCodeWidth.length;
            params[pos++] = time;
            if (qrcodeDec != null) {
                byte[] qrcodeDecLen = lenToLLVAR(qrcodeDec.length);
                System.arraycopy(qrcodeDecLen, 0, params, pos, qrcodeDecLen.length);
                pos += qrcodeDecLen.length;
                System.arraycopy(qrcodeDec, 0, params, pos, qrcodeDec.length);
                pos += qrcodeDec.length;
            } else {
                params[pos++] = (byte) 0;
                params[pos++] = (byte) 0;
            }
            byte[] qrcodeDataLen = lenToLLVAR(qrcodeData.length);
            System.arraycopy(qrcodeDataLen, 0, params, pos, qrcodeDataLen.length);
            pos +=  qrcodeDataLen.length;
            System.arraycopy(qrcodeData, 0, params, pos, qrcodeData.length);

            Packet packet = new Packet();
            packet.setCommand(new byte[] {(byte)0XA1, 0X11});
            packet.setParams(params);
            Future<ResponseData> future = packCommandWithResponse(packet, time + TimeUtils.TIME_NORMAL);
            ResponseData responseData = null;
            try {
                responseData = future.get();
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            if (responseData != null) {
                if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                    return true;
                } else {
                    throw new SDKException(responseData.getRespCode());
                }
            } else {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_PARAM_ERROR);
        }
    }

    public static byte[] checkIsIcCard() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0XE1, 0X01});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams();
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] getBatteryAndState() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1D, 0X0C});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams();
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte interUpdate() throws SDKException {
        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1D, 0X0D});
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_NORMAL);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams()[0];
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }
    }

    public static byte[] updateApp(byte type, byte status, byte[] offset, byte[] data, byte[] dataLen) throws SDKException {
        byte[] params;
        if (data == null) {
            params = new byte[12];
        } else {
            params = new byte[(data.length + 12)];
        }
        int pos = 0;
        params[pos++] = type;
        params[pos++] = status;
        if (data != null) {
            byte[] tlvLen = lenToLLVAR(data.length);
            params[pos++] = tlvLen[0];
            params[pos++] = tlvLen[1];
            System.arraycopy(data, 0, params, pos, data.length);
            pos += data.length;
        }
        if (offset != null) {
            System.arraycopy(offset, 0, params, pos, offset.length);
            pos += offset.length;
        }
        if (dataLen != null) {
            System.arraycopy(dataLen, 0, params, pos, dataLen.length);
            pos += dataLen.length;
        }

        Packet packet = new Packet();
        packet.setCommand(new byte[] {(byte)0X1D, 0X09});
        packet.setParams(params);
        Future<ResponseData> future = packCommandWithResponse(packet, TimeUtils.TIME_LONG);
        ResponseData responseData = null;
        try {
            responseData = future.get();
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (responseData != null) {
            if (ResponseCode.SUCCESS.equals(responseData.getRespCode())) {
                return responseData.getPacket().getParams();
            } else {
                throw new SDKException(responseData.getRespCode());
            }
        } else {
            throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
        }

    }

}
