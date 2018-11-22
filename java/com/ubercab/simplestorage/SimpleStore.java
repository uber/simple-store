package com.ubercab.simplestorage;

import java.io.Closeable;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public interface SimpleStore extends Closeable {

    SimpleStore scope(String name);
    SimpleStore scope(String name, ScopeConfig config);

    void getString(String key, @Nonnull Callback<String> cb, @Nonnull Executor executor);
    void putString(String key, @Nullable String value, @Nonnull Callback<String> cb, @Nonnull Executor executor);

    void get(String key, @Nonnull Callback<byte[]> cb, @Nonnull Executor executor);
    void put(String key, @Nullable byte[] value, @Nonnull Callback<byte[]> cb, @Nonnull Executor executor);

    void deleteAll(Callback<Void> cb, Executor executor);

    void close();

    interface Callback<T> {
        void onSuccess(T value);

        void onError(Throwable t);
    }
}
