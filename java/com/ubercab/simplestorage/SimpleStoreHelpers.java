package com.ubercab.simplestorage;

import javax.annotation.Nullable;

/**
 * Useful wrappers for common storage operations.
 */
public final class SimpleStoreHelpers {

    /**
     * Prefetch specified keys into the memory cache.
     * @param store to warm
     * @param keys to fetch
     */
    public static void prefetch(SimpleStore store, String... keys) {
        for(String key : keys) {
            store.get(key, noopCallback, SimpleStoreConfig.getComputationExecutor());
        }
    }

    private static final SimpleStore.Callback<byte[]> noopCallback = new SimpleStore.Callback<byte[]>() {

        @Override
        public void onSuccess(@Nullable byte[] value) {

        }

        @Override
        public void onError(Throwable error) {

        }
    };
}
