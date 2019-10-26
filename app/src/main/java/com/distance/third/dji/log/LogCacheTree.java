package com.distance.third.dji.log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 日志缓存树，可以缓存最新大小的日志到内存或者到本地
 * 详细内容。
 *
 * @author sky on 2018/2/26
 */
public final class LogCacheTree extends LogTree {

    private long curWriteFileLength = 0;
    private LogCacheConfig logFileConfig;

    private FileOutputStream fos = null;

    private int curMsgCacheSize = 0;
    private Queue<byte[]> msgCacheQueue;


    /**
     * 构造器
     *  @param priority      日志输出优先级
     * @param logFileConfig 指定日志备份文件，当前写文件路径和单个文件最大字节数
     */
    public LogCacheTree(int priority, LogCacheConfig logFileConfig) {
        super(priority, true);

        this.logFileConfig = logFileConfig;

        createDirIfNeed();
        checkAndCreateCurWriteFileIfNeed();
        createMsgCacheQueueIfNeed();
    }

    @Override
    protected void onMsg(String compoundMsg, final LogData logData) {
        if (isReleaseCalled()) {
            return;
        }

        onMsgAndCheckFileLength(compoundMsg);
    }

    /**
     * 调用该方法，会等待子线程释放完成后，才返回。
     */
    @Override
    protected void release() {
        super.release();
        closeAndSetMsgStreamNull();
    }

    private void createMsgCacheQueueIfNeed() {
        if (isMsgMemoryCacheDisable()) {
            return;
        }

        msgCacheQueue = new ConcurrentLinkedQueue<>();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createDirIfNeed() {
        if (isFileCacheDisable()) {
            return;
        }

        File dir = new File(logFileConfig.logFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("logFileDir " + logFileConfig.logFileDir + " must be a dir");
        }
    }

    private boolean isMsgMemoryCacheDisable() {
        return logFileConfig.maxLogMemoryCacheSize < 1;
    }

    private boolean isFileCacheDisable() {
        return logFileConfig.logFileDir == null;
    }

    private void onMsgAndCheckFileLength(String compoundMsg) {
        if (isFileCacheDisable() && isMsgMemoryCacheDisable()) {
            return;
        }

        // 大约30us
        byte[] msgBytes = compoundMsg.getBytes();
        long length = msgBytes.length;

        // 写入到文件大概是100us
        writeLogToFileIfNeed(msgBytes, length);

        // 大约33us
        writeLogToMemoryCacheIfNeed(msgBytes, length);
    }

    private void writeLogToMemoryCacheIfNeed(byte[] msgBytes, long length) {
        if (isMsgMemoryCacheDisable()) {
            return;
        }

        // 循环确定是否队列的数据已经满了，如果没有满则直接添加，否则移除队头数据，知道满足要求
        for (; ; ) {
            if (curMsgCacheSize + length > logFileConfig.maxLogMemoryCacheSize) {
                byte[] bytes = msgCacheQueue.poll();
                if (bytes != null) {
                    curMsgCacheSize -= bytes.length;
                    continue;
                }
                // this could not happen
                break;
            }

            msgCacheQueue.offer(msgBytes);
            curMsgCacheSize += length;
            break;
        }
    }

    /**
     * 获取缓存的最新的在内存中的日志，返回一个日志列表，日志从前往后
     */
    List<byte[]> getMemoryCachedMsg() {
        if (msgCacheQueue == null) {
            return new LinkedList<>();
        }

        return new LinkedList<>(msgCacheQueue);
    }

    private void writeLogToFileIfNeed(byte[] msgBytes, long length) {
        if (isFileCacheDisable()) {
            return;
        }

        if (fos == null) {
            return;
        }

        try {
            fos.write(msgBytes);
            curWriteFileLength += length;
            if (isCurFileSizeExceed()) {
                checkAndCreateCurWriteFileIfNeed();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private boolean isCurFileSizeExceed() {
        return curWriteFileLength >= logFileConfig.maxLogFileLength;
    }

    private void closeAndSetMsgStreamNull() {
        try {
            OutputStream tempOs = fos;
            fos = null;
            if (tempOs != null) {
                tempOs.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 校正当前要写的文件：
     * 1、判断本地文件“curWriteFile”文件是否存在，不存在则创建文件
     * 2、若存在则判断其大小是否大于等于MAX_FILE_LENGTH，则将fileLength设置为该文件的长度
     * 3、若满足，则将文件重命名为“backupFile”，创建新的文件
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkAndCreateCurWriteFileIfNeed() {
        if (isFileCacheDisable()) {
            return;
        }

        File file = new File(logFileConfig.getCurWriteFilePath());
        try {
            if (!file.exists()) {
                curWriteFileLength = 0;
                return;
            }

            if (!isCurFileSizeExceed()) {
                curWriteFileLength = file.length();
                return;
            }

            // 先关闭针对当前文件的写流
            closeAndSetMsgStreamNull();
            File backupFile = new File(logFileConfig.getBackupFilePath());
            backupFile.delete();
            file.renameTo(backupFile);
            curWriteFileLength = 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            curWriteFileLength = 0;
        } finally {
            createMsgStreamIfNull();
        }
    }

    private void createMsgStreamIfNull() {
        if (fos != null) {
            return;
        }

        try {
            fos = new FileOutputStream(logFileConfig.getCurWriteFilePath(), true);
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 一句话注释。
     * 详细内容。
     *
     * @author sky on 2018/3/27
     */
    public static class LogCacheConfig {
        /**
         * 日志文件的存放路径，如果为空，则表示不缓存到本地磁盘中
         */
        public String logFileDir;
        /**
         * fileName
         */
        public String backupFile;
        /**
         * 备份文件的名称
         */
        public String curWriteFile;

        /**
         * 保存到本地日志文件中的最大长度。
         */
        public long maxLogFileLength;

        /**
         * 缓存到最新内存中的日志大小，如果<=0，则表示不缓存在内存中
         */
        public int maxLogMemoryCacheSize;

        String getCurWriteFilePath() {
            return logFileDir + File.separator + curWriteFile;
        }

        String getBackupFilePath() {
            return logFileDir + File.separator + backupFile;
        }
    }
}
