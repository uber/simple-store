package com.uber.simplestore;

import com.uber.simplestore.executors.StorageExecutors;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

public final class SimpleStoreConfig {

    @Nullable
    private static Executor ioExecutor;

    @Nullable
    private static Executor computationExecutor;

    public static Executor getIOExecutor() {
        if (ioExecutor == null) {
            ioExecutor = StorageExecutors.ioExecutor();
        }
        return ioExecutor;
    }

    /**
     * Override the executor used for IO operations.
     * @param executor to set, null unsets.
     */
    public static void setIOExecutor(@Nullable Executor executor) {
        ioExecutor = executor;
    }

    public static Executor getComputationExecutor() {
        if (computationExecutor == null) {
            computationExecutor = StorageExecutors.computationExecutor();
        }
        return computationExecutor;
    }

    /**
     * Override the executor used for computation.
     * @param executor to set, null unsets.
     */
    public static void setComputationExecutor(@Nullable Executor executor) {
        computationExecutor = executor;
    }
}
