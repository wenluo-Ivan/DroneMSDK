package com.distance.third.dji.log;

import android.os.Process;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 一句话注释。
 * 详细内容。
 *
 * @author sky on 2018/2/26
 */
final class LogHelper {

    public static final char LINE_BREAK = '\n';

    private static final char SPIRIT = '/';

    private static final char LEFT_BRACKET = '(';

    private static final char RIGHT_BRACKET = ')';

    private static final char SPACE = ' ';

    private static final char CONNECT = '-';

    private static final char COLON = ':';

    /**
     * 2018-02-26 16:53:25.123
     */
    private static final String DATA_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS";

    /**
     * 获取堆栈，使用系统自带的获取堆栈的方法，效率更高
     */
    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }

    /**
     * 获取当前时间的格式化字符串
     */
    public static String formatCurTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat(DATA_FORMAT, Locale.getDefault());
        return format.format(date);
    }

    /**
     * 获取日志级别对应的字符
     */
    public static String getPriorityString(int priority) {
        switch (priority) {
            case Logger.VERBOSE:
                return "V";
            case Logger.DEBUG:
                return "D";
            case Logger.INFO:
                return "I";
            case Logger.WARN:
                return "W";
            case Logger.ERROR:
                return "E";
            case Logger.ASSERT:
                return "A";
            default:
                return "unknown";

        }
    }

    /**
     * 设计日志的头部,组装日志样式："2018-02-26 16:53:25.123 D/Tag(pid-tid)："
     */
    public static StringBuilder getLogPrefix(StringBuilder sb, LogData logData) {
        return sb.append(formatCurTime(logData.time))
                 .append(SPACE)
                 .append(getPriorityString(logData.priority))
                 .append(SPIRIT)
                 .append(logData.tag)
                 .append(LEFT_BRACKET)
                 .append(Process.myPid())
                 .append(CONNECT)
                 .append(logData.threadId)
                 .append(RIGHT_BRACKET)
                 .append(COLON)
                 .append(SPACE);

    }

    /**
     * 合成日志，样式为："2018-02-26 16:53:25.123 D/Tag(pid-tid)：msg + \n + getStackTraceString"
     *
     * <p><strong>该方法非常耗时，大约为1800us</strong>
     * */
    public static String compoundMsg(final LogData logData) {
        StringBuilder msgBuilder = new StringBuilder(256);

        LogHelper.getLogPrefix(msgBuilder, logData);
        msgBuilder.append(logData.msg)
                  .append(LogHelper.LINE_BREAK);
        if (logData.tr != null) {
            msgBuilder.append(LogHelper.getStackTraceString(logData.tr));
            msgBuilder.append(LogHelper.LINE_BREAK);
        }
        return msgBuilder.toString();
    }
}
