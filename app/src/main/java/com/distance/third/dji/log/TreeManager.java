package com.distance.third.dji.log;


import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志树管理类
 *
 * @author sky on 2018/2/26
 */
final class TreeManager implements HandleLog, LogTreeManager {

    private static final int THREAD_CLOSED = -2;
    private static final int REQUEST_THREAD_CLOSE = -1;
    private static final int THREAD_RUNNING = 0;

    /**
     * 注意该对象没有使用volatile修饰这意味着在移除或添加logcat数时,
     * 可能导致变量的更改没有及时同步到调用handleMsg方法的线程上。
     * 考虑到volatile带来的内存损失，这是可以接受的
     */
    private LogcatTree logcatTree;
    private final CopyOnWriteArrayList<LogTree> trees = new CopyOnWriteArrayList<>();
    private final Queue<LogData> msgQueue = new ConcurrentLinkedQueue<>();

    /**
     * 在内存中日志队列的最大日志条数
     */
    private int maxLogCountInQueue;
    private AtomicInteger logCountInQueue = new AtomicInteger(0);

    /**
     * -2(THREAD_CLOSED)，表示未启动或者已经关闭
     * -1(REQUEST_THREAD_CLOSE)，表示请求关闭
     * 0(THREAD_RUNNING)， 表示已经正常启动
     */
    private AtomicInteger dispatcherThreadState = new AtomicInteger(THREAD_CLOSED);

    /**
     * 消息分发线程
     */
    private Thread msgDispatcherThread;

    /**
     * 构造器
     */
    TreeManager(int maxLogCountInQueue) {
        this.maxLogCountInQueue = maxLogCountInQueue;
        setLogcatTreeEmpty();
    }

    @Override
    public void handleMsg(int priority, String tag, String msg) {
        logcatTree.handleMsg(priority, tag, msg);
        checkAndOfferMsgToMsgQueue(priority, tag, msg, null);
    }

    @Override
    public void handleMsg(int priority, String tag, String msg, Throwable tr) {
        logcatTree.handleMsg(priority, tag, msg, tr);
        checkAndOfferMsgToMsgQueue(priority, tag, msg, tr);
    }

    @Override
    public synchronized boolean addLogTree(LogTree logTree) {
        return !(logTree == null) && addTree(logTree);
    }

    @Override
    public synchronized boolean addLogTrees(LogTree... logTrees) {
        if (logTrees == null || logTrees.length == 0) {
            return false;
        }

        for (LogTree logTree : logTrees) {
            addTree(logTree);
        }

        return true;
    }

    @Override
    public synchronized boolean removeLogTree(LogTree logTree) {
        if (logTree == null) {
            return false;
        }

        logTree.release();

        boolean removed = removeTree(logTree);

        if (trees.isEmpty()) {
            waitForMsgDispatcherThreadClosed();
        }

        return removed;
    }

    /**
     * 当clear方法调用时，其他add remove 方法直接返回false。
     */
    @Override
    public synchronized boolean clearTrees() {
        release();
        setLogcatTreeEmpty();
        trees.clear();
        waitForMsgDispatcherThreadClosed();
        return true;
    }

    private void checkAndOfferMsgToMsgQueue(int priority, String tag, String msg, Throwable throwable) {
        if (trees.isEmpty() || logCountInQueue.get() > maxLogCountInQueue) {
            return;
        }

        // System.currentTimeMillis()可能需要发费3-4us, Process.myTid可能需要5-6us
        LogData logData = new LogData(System.currentTimeMillis(), priority, tag, msg, throwable, Process.myTid());
        logCountInQueue.incrementAndGet();

        // LinkedList.add大概需要7-8us
        // 单线程时执行ConcurrentLinkedQueue.offer需要12-16us
        msgQueue.offer(logData);
    }

    private void createDispatcherThreadAndStartIfNeed() {
        if (!trees.isEmpty() && msgDispatcherThread == null) {
            msgDispatcherThread = new MsgDispatcherThread();
            msgDispatcherThread.start();
            dispatcherThreadState.set(THREAD_RUNNING);
        }
    }

    /**
     * 获取内存缓存最新日志，注意必要添加LogCacheTree，否则返回空列表
     */
    List<byte[]> getMemoryCachedMsg() {
        for (LogTree tree : trees) {
            if (tree instanceof LogCacheTree) {
                LogCacheTree logCacheTree = ((LogCacheTree) tree);


                List<LogData> tempQueue = new ArrayList<>(msgQueue);
                List<byte[]> cacheMsg = logCacheTree.getMemoryCachedMsg();

                // 将消息队列中的消息全部写入到后面
                for (LogData logData : tempQueue) {
                    cacheMsg.add(LogHelper.compoundMsg(logData).getBytes());
                }

                return cacheMsg;
            }
        }
        return new ArrayList<>();
    }

    private boolean removeTree(LogTree logTree) {
        if (logTree instanceof LogcatTree) {
            setLogcatTreeEmpty();
            return true;
        }
        return trees.remove(logTree);
    }

    private void setLogcatTreeEmpty() {
        logcatTree = LogcatTree.EmptyLogcatTree.EMPTY_LOGCAT_TREE;
    }

    private void release() {
        if (logcatTree != null) {
            logcatTree.release();
        }

        for (LogTree logTree : trees) {
            if (logTree != null) {
                logTree.release();
            }
        }
    }

    private boolean addTree(LogTree logTree) {
        if (logTree instanceof LogcatTree) {
            logcatTree = (LogcatTree) logTree;
            return true;
        }

        boolean added = trees.add(logTree);
        createDispatcherThreadAndStartIfNeed();

        return added;
    }

    private void waitForMsgDispatcherThreadClosed() {
        if (!isThreadStateRunning()) {
            return;
        }

        // 设置为-1，请求关闭线程，一直等待关闭后才退出
        dispatcherThreadState.set(REQUEST_THREAD_CLOSE);
        for (; ; ) {
            if (isThreadStateClosed()) {
                msgDispatcherThread = null;
                return;
            }
        }
    }

    private boolean isThreadStateRunning() {
        return dispatcherThreadState.get() == THREAD_RUNNING;
    }

    private boolean isThreadStateClosed() {
        return dispatcherThreadState.get() == THREAD_CLOSED;
    }

    private class MsgDispatcherThread extends Thread {

        @Override
        public void run() {
            try {
                for (; isThreadStateRunning(); ) {
                    // poll大约8-16us
                    LogData logData = msgQueue.poll();

                    if (sleepIfMsgNull(logData)) {
                        continue;
                    }
                    logCountInQueue.decrementAndGet();

                    dispatchMsg(logData);
                }
            } catch (Exception exception) {
                Log.w("MsgDispatcherThread", "run" + exception.getMessage());
            } finally {
                dispatcherThreadState.set(THREAD_CLOSED);
            }
        }

        private boolean sleepIfMsgNull(LogData logData) {
            if (logData == null) {
                try {
                    Thread.sleep(10);
                    return true;
                } catch (InterruptedException exception) {
                    Logger.warn("TreeManager", exception);
                    Thread.currentThread().interrupt();
                    return true;
                }
            }
            return false;
        }

        private void dispatchMsg(final LogData logData) {
            String compoundMsg = null;
            for (LogTree logTree : trees) {
                if (logTree.isAcceptCompoundMsg()) {
                    if (compoundMsg == null) {
                        // 耗时，大约发费1800us
                        compoundMsg = LogHelper.compoundMsg(logData);
                    }
                    logTree.handleMsg(compoundMsg, logData);
                } else {
                    logTree.handleMsg(null, logData);
                }
            }
        }
    }
}
