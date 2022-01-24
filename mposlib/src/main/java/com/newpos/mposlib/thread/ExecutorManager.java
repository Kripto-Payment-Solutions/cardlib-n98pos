package com.newpos.mposlib.thread;

import com.newpos.mposlib.util.LogUtil;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorManager {
    public static final String THREAD_NAME_PREFIX = "mpos-";
    public static final String WRITE_THREAD_NAME = THREAD_NAME_PREFIX + "write-t";
    public static final String READ_THREAD_NAME = THREAD_NAME_PREFIX + "read-t";
    public static final String DISPATCH_THREAD_NAME = THREAD_NAME_PREFIX + "dispatch-t";
    public static final String START_THREAD_NAME = THREAD_NAME_PREFIX + "start-t";
    public static final String TIMER_THREAD_NAME = THREAD_NAME_PREFIX + "timer-t";
    public static final ExecutorManager INSTANCE = new ExecutorManager();
    private ThreadPoolExecutor writeThread;
    private ThreadPoolExecutor dispatchThread;
    private ScheduledExecutorService timerThread;

    public ThreadPoolExecutor getWriteThread() {
        if (writeThread == null || writeThread.isShutdown()) {
            writeThread = new ThreadPoolExecutor(2, 6,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(100),
                    new NamedThreadFactory(WRITE_THREAD_NAME),
                    new RejectedHandler());
        }
        return writeThread;
    }

    public ThreadPoolExecutor getDispatchThread() {
        if (dispatchThread == null || dispatchThread.isShutdown()) {
            dispatchThread = new ThreadPoolExecutor(2, 4,
                    10L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(100),
                    new NamedThreadFactory(DISPATCH_THREAD_NAME),
                    new RejectedHandler());
        }
        return dispatchThread;
    }

    public ScheduledExecutorService getTimerThread() {
        if (timerThread == null || timerThread.isShutdown()) {
            timerThread = new ScheduledThreadPoolExecutor(1,
                    new NamedThreadFactory(TIMER_THREAD_NAME),
                    new RejectedHandler());
        }
        return timerThread;
    }

    public synchronized void shutdown() {
        if (writeThread != null) {
            writeThread.shutdownNow();
            writeThread = null;
        }
        if (dispatchThread != null) {
            dispatchThread.shutdownNow();
            dispatchThread = null;
        }
        if (timerThread != null) {
            timerThread.shutdownNow();
            timerThread = null;
        }
    }

    public static boolean isMposThread() {
        return Thread.currentThread().getName().startsWith(THREAD_NAME_PREFIX);
    }

    private static class RejectedHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LogUtil.w("Executor a task was rejected r=" + r);
        }
    }
}
