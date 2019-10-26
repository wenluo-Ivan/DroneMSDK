package com.distance.third.dji.log;

import android.annotation.SuppressLint;
import android.util.Log;

/**
 * logcat日志打印树
 *
 * @author sky on 2018/2/26
 */
public class LogcatTree extends LogTree implements HandleLog {

    /**
     * 构造器
     *
     * @param priority 允许输出的日志优先级
     */
    public LogcatTree(int priority) {
        super(priority, false);
    }

    @Override
    public final void handleMsg(String compoundMsg, LogData logData) {
        throw new UnsupportedOperationException("call handleMsg(int, String, String, Throwable) instead!!");
    }

    @SuppressLint("LogTagMismatch")
    @Override
    public void handleMsg(int priority, String tag, String msg, Throwable tr) {
        if (isLoggable(priority)) {
            Log.println(priority, tag, msg + '\n' + LogHelper.getStackTraceString(tr));
        }
    }

    @SuppressLint("LogTagMismatch")
    @Override
    public void handleMsg(int priority, String tag, String msg) {
        if (isLoggable(priority)) {
            Log.println(priority, tag, msg);
        }
    }

    public static final class EmptyLogcatTree extends LogcatTree {
        /**
         * 空Logcat对象
         */
        public static final EmptyLogcatTree EMPTY_LOGCAT_TREE = new EmptyLogcatTree(Logger.ASSERT);

        /**
         * 构造器
         *
         * @param priority 允许输出的日志优先级
         */
        private EmptyLogcatTree(int priority) {
            super(priority);
        }

        @Override
        public void handleMsg(int priority, String tag, String msg) {
            // empty
        }

        @Override
        public void handleMsg(int priority, String tag, String msg, Throwable tr) {
            // empty
        }
    }
}
