package com.distance.third.dji.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 李文烙
 * @date 2019/10/26
 * @Desc 文件读写工具类
 */
public class FileUtils {

    /**
     * 该方法主要目的是写入无人机回调数据到本地，形成h264文件
     * 写入数据到本地
     * @param file 需要写入的文件路径
     * @param conent byte数据
     */
    public static void writeToLocal(String file, byte[] conent) {
        FileOutputStream out = null;
        InputStream is = null;
        try {
            File h264File = new File(file);
            if (!h264File.exists()) {
                h264File.createNewFile();
            }
            is = new ByteArrayInputStream(conent);
            out = new FileOutputStream(h264File, true);
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = is.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
