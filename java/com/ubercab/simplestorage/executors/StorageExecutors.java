package com.ubercab.simplestorage.executors;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public final class StorageExecutors {

    private static final Executor MAIN_EXECUTOR = new MainThreadExecutor();

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    public static Executor mainExecutor() {
        return MAIN_EXECUTOR;
    }

    public static Executor directExecutor() {
        return DIRECT_EXECUTOR;
    }

    public static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }
}
