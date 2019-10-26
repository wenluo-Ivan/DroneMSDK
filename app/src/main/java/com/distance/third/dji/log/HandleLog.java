package com.distance.third.dji.log;



/**
 * 日志处理接口
 *
 * @author sky on 2018/2/26
 */
interface HandleLog {
    /**
     * 处理纯日志
     * */
    void handleMsg(int priority, String tag, String msg);


    /**
     * 处理带throwable的日志
     * */
    void handleMsg(int priority, String tag, String msg, Throwable tr);
}
