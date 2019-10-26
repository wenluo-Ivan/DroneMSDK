package com.distance.third.dji.util;

import android.os.Handler;
import android.os.Looper;


/**
 * 功能：将消息发送到主线程
 * Created by 万广义 on 2019/1/18.
 */
public class HandlerUtils extends Handler {

    private static Handler handler;

    static {
        handler = new HandlerUtils(Looper.getMainLooper());
    }

    private HandlerUtils(Looper looper) {
        super(looper);
    }


    public static boolean postToMain(Runnable runnable) {
        return handler.post(runnable);
    }

    public static boolean postDelayedToMain(Runnable runnable, long delayMillis) {
        return handler.postDelayed(runnable, delayMillis);
    }

}
