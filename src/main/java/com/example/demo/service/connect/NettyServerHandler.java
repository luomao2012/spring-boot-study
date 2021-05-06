package com.example.demo.service.connect;

import org.springframework.stereotype.Component;

/**
 * @author Marion
 * @date 2021/5/2
 */
public class NettyServerHandler {
    public static long readerIdleTime;

    private static Object INSTANCE = new Object();

    public static long getReaderIdleTime() {
        return readerIdleTime;
    }

    public static void setReaderIdleTime(long readerIdleTime) {
        NettyServerHandler.readerIdleTime = readerIdleTime;
    }
}
