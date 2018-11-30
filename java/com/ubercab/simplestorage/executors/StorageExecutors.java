package com.ubercab.simplestorage.executors;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Like MoreExecutors, but no Guava.
 */
public final class StorageExecutors {

    private static final Executor MAIN_EXECUTOR = new MainThreadExecutor();

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private static int ioThreadCount = 0;
    private static final Executor IO_EXECUTOR = Executors.newCachedThreadPool(r -> new Thread(r, "SimpleStoreIO-"+ ioThreadCount++));

    private static int compThreadCount = 0;
    private static final Executor COMPUTATION_EXECUTOR = Executors.newFixedThreadPool(2, r -> new Thread(r, "SimpleStoreComp-" + compThreadCount++));

    public static Executor mainExecutor() {
        return MAIN_EXECUTOR;
    }

    public static Executor directExecutor() {
        return DIRECT_EXECUTOR;
    }

    public static Executor computationExecutor() {
        return COMPUTATION_EXECUTOR;
    }

    public static Executor ioExecutor() {
        return IO_EXECUTOR;
    }

    static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }
}
