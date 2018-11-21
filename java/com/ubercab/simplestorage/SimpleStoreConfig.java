package com.ubercab.simplestorage;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SimpleStoreConfig {
    private static int threadCount = 0;

    private static Executor ioExecutor = Executors.newCachedThreadPool(r -> new Thread(r, "SimpleStoreIO-"+ threadCount++));

    public static Executor getIOExecutor() {
        return ioExecutor;
    }
}
