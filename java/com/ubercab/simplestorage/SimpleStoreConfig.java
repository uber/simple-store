package com.ubercab.simplestorage;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

public class SimpleStoreConfig {
    private static int threadCount = 0;
    private static final Executor DEFAULT_IO_EXECUTOR = Executors.newCachedThreadPool(r -> new Thread(r, "SimpleStoreIO-"+ threadCount++));

    @Nullable
    private static Executor ioExecutor;

    public static Executor getIOExecutor() {
        if (ioExecutor == null) {
            ioExecutor = DEFAULT_IO_EXECUTOR;
        }
        return ioExecutor;
    }

    public static void setIOExecutor(Executor executor) {
        ioExecutor = executor;
    }
}
