package com.uber.simplestore;

import com.google.common.util.concurrent.ListenableFuture;

/** Useful wrappers for common storage operations. */
public final class SimpleStoreHelpers {

  /**
   * Prefetch specified keys into the memory cache.
   *
   * @param store to warm
   * @param keys to fetch
   */
  public static void prefetch(SimpleStore store, String... keys) {
    for (String key : keys) {
      ListenableFuture<byte[]> ignored = store.get(key);
    }
  }
}
