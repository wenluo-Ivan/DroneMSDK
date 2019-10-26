package com.distance.third.dji.log;


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 日志树基类，由子类实现具体的日志功能
 * 详细内容。
 *
 * @author sky on 2018/2/26
 */
public abstract class LogTree {
    protected int priority;
    protected AtomicBoolean isReleaseCalled = new AtomicBoolean(false);
    private final boolean isAcceptCompoundMsg;

    /**
     * 构造器
     */
    public LogTree(int priority, boolean acceptCompoundMsg) {
        this.priority = priority;
        isAcceptCompoundMsg = acceptCompoundMsg;
    }

    /**
     * 是否采用默认组合好的数据
     * */
    public boolean isAcceptCompoundMsg() {
        return isAcceptCompoundMsg;
    }

    protected boolean isLoggable(final int priority) {
        return priority >= this.priority;
    }

    /**
     * 使用封装的LogData
     * @param compoundMsg 使用默认LogHelper.compoundMsg返回的日志，如果isAcceptCompoundMsg为false，参数值为null。
     * @param logData     未处理的原始数据对象
     */
    public void handleMsg(String compoundMsg, final LogData logData) {
        if (isLoggable(logData.priority)) {
            onMsg(compoundMsg, logData);
        }
    }

    /**
     * 该方法将会在子线程中调用，基于性能考虑，除了Logcat的日志外，其他的都是在子线程调用
     */
    protected void onMsg(String compoundMsg, final LogData logData) {}

    /**
     * 停止日志打印时调用，子类必须调用super.release
     */
    protected void release() {
        isReleaseCalled.set(true);
    }

    protected boolean isReleaseCalled() {
        return isReleaseCalled.get();
    }
}
