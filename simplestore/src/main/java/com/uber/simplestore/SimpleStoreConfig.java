package com.uber.simplestore;

import com.uber.simplestore.executors.StorageExecutors;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Configure executors used by SimpleStore.
 *
 * <p>Set may only be called once, and should be called before any use of stores.
 */
public final class SimpleStoreConfig {

  private static final Object writeLock = new Object();

  @Nullable private static Executor ioExecutor;

  @Nullable private static Executor computationExecutor;

  public static Executor getIOExecutor() {
    if (ioExecutor == null) {
      synchronized (writeLock) {
        if (ioExecutor == null) {
          ioExecutor = StorageExecutors.ioExecutor();
        }
      }
    }
    return ioExecutor;
  }

  /**
   * Override the executor used for IO operations.
   *
   * @param executor to set, null unsets.
   */
  public static void setIOExecutor(@Nullable Executor executor) {
    synchronized (writeLock) {
      ioExecutor = executor;
    }
  }

  public static Executor getComputationExecutor() {
    if (computationExecutor == null) {
      synchronized (writeLock) {
        if (computationExecutor == null) {
          computationExecutor = StorageExecutors.computationExecutor();
        }
      }
    }
    return computationExecutor;
  }

  /**
   * Override the executor used for computation.
   *
   * @param executor to set, null unsets.
   */
  public static void setComputationExecutor(@Nullable Executor executor) {
    synchronized (writeLock) {
      computationExecutor = executor;
    }
  }

  private SimpleStoreConfig() {}
}
