package com.newpos.mposlib.impl;

import com.newpos.mposlib.api.Connection;
import com.newpos.mposlib.model.AckCallback;
import com.newpos.mposlib.model.AckContext;
import com.newpos.mposlib.model.Packet;
import com.newpos.mposlib.util.LogUtil;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * AckRequestMgr
 */
public final class AckRequestMgr {

    private static AckRequestMgr I;

    private final Map<Integer, RequestTask> queue = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timer = getTimerThread();
    private final Callable<Boolean> NONE = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            return Boolean.FALSE;
        }
    };

    private Connection connection;


    public static AckRequestMgr I() {
        if (I == null) {
            synchronized (AckRequestMgr.class) {
                if (I == null) {
                    I = new AckRequestMgr();
                }
            }
        }
        return I;
    }

    private AckRequestMgr() {
    }

    public Future<Boolean> add(int packetID, AckContext context) {
        if (context.ackModel == Packet.ACK_NONE) {
            return null;
        }
        if (context.callback == null) {
            return null;
        }
        return addTask(new RequestTask(packetID, context));
    }

    public RequestTask getAndRemove(int packetID) {
        return queue.remove(packetID);
    }


    /**
     * disconnect clear timer
     */
    public void clear() {
        for (RequestTask task : queue.values()) {
            try {
                task.future.cancel(true);
            } catch (Exception e) {
            }
        }
    }

    private RequestTask addTask(RequestTask task) {
        queue.put(task.packetID, task);
        task.future = timer.schedule(task, task.timeout, TimeUnit.MILLISECONDS);
        return task;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * time out thread
     */
    private ScheduledExecutorService timerThread;

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

    public final class RequestTask extends FutureTask<Boolean> implements Runnable {
        private final int timeout;
        private final long sendTime;
        private final int packetID;
        private AckCallback callback;
        private Packet request;
        private Future<?> future;
        private int retryCount;

        private RequestTask(AckCallback callback, int timeout, int packetID, Packet request, int retryCount) {
            super(NONE);
            this.callback = callback;
            this.timeout = timeout;
            this.sendTime = System.currentTimeMillis();
            this.packetID = packetID;
            this.request = request;
            this.retryCount = retryCount;
        }

        private RequestTask(int packetID, AckContext context) {
            this(context.callback, context.timeout, packetID, context.request, context.retryCount);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {
            queue.remove(packetID);
            timeout();
        }

        public void timeout() {
            call(null);
        }

        public void success(Packet packet) {
            call(packet);
        }

        private void call(Packet response) {
            if (this.future.cancel(true)) {
                boolean success = response != null;
                this.set(success);
                if (callback != null) {
                    if (success) {
                        LogUtil.d("receive one ack response, packetID=" + packetID + " costTime=" +
                                (System.currentTimeMillis() - sendTime) + " request=" + request +
                                        " response=" + response);
                        callback.onSuccess(response);
                    } else if (request != null && retryCount > 0) {
                        LogUtil.d("one ack request timeout, retry=" + retryCount
                                + " packetID=" + packetID + " costTime=" +(System.currentTimeMillis() - sendTime)
                                + " request=" + request);
                        addTask(copy(retryCount - 1));
                        connection.send(request);
                    } else {
                        LogUtil.d("one ack request timeout, packetID=" + packetID + " costTime=" +(System.currentTimeMillis() - sendTime)
                                + " request=" + request);
                        callback.onTimeout(request);
                    }
                }
                callback = null;
                request = null;
            }
        }

        private RequestTask copy(int retryCount) {
            return new RequestTask(callback, timeout, packetID, request, retryCount);
        }
    }
}
