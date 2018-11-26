package com.ubercab.simplestorage;

import java.io.Closeable;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fast, reliable storage.
 */
public interface SimpleStore extends Closeable {

    /**
     * Enter a nested scope.
     *
     * @param name of scope to enter
     * @param config to apply
     * @return store for nested scope
     */
    SimpleStore scope(String name, ScopeConfig config);

    /**
     * Convenience method for #{@link #scope(String, ScopeConfig)} with #{@link ScopeConfig#DEFAULT}
     */
    SimpleStore scope(String name);

    /**
     * Retrieve a byte[]-backed String.
     * @param key to fetch from
     * @param callback receives parsed String
     * @param executor to run callback on
     */
    void getString(String key, @Nonnull Callback<String> callback, @Nonnull Executor executor);

    /**
     * Stores a String as a byte[].
     * @param key to store to
     * @param value to write
     * @param callback receives status and saved value
     * @param executor to run callback on
     */
    void putString(String key, @Nullable String value, @Nonnull Callback<String> callback, @Nonnull Executor executor);

    /**
     * Retrieve a byte[] from disk.
     * @param key to store to
     * @param callback receives full byte[]
     * @param executor to run callback on
     */
    void get(String key, @Nonnull Callback<byte[]> callback, @Nonnull Executor executor);

    /**
     * Stores a byte[] on disk.
     * @param key to store to
     * @param value to store
     * @param callback receives status and saved value
     * @param executor to run callback on
     */
    void put(String key, @Nullable byte[] value, @Nonnull Callback<byte[]> callback, @Nonnull Executor executor);

    /**
     * Delete all keys in this direct scope.
     * @param callback when complete
     * @param executor to run callback on
     */
    void deleteAll(@Nonnull Callback<Void> callback, @Nonnull Executor executor);

    /**
     * Fails any outstanding operations and releases the memory cache.
     */
    void close();

    /**
     * Called exactly once running on the specified executor.
     *
     * @param <T>
     */
    interface Callback<T> {
        /**
         * Operation succeeded.
         * @param value on disk
         */
        void onSuccess(@Nullable T value);

        /**
         * Operation failed.
         */
        void onError(Throwable error);
    }
}
