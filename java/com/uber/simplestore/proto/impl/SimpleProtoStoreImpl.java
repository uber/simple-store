package com.uber.simplestore.proto.impl;

import android.content.Context;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.impl.SimpleStoreFactory;
import com.uber.simplestore.proto.SimpleProtoStore;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SimpleProtoStoreImpl implements SimpleProtoStore {
    private final SimpleStore simpleStore;

    private SimpleProtoStoreImpl(SimpleStore simpleStore) {
        this.simpleStore = simpleStore;
    }

    public static SimpleProtoStoreImpl create(Context context, String scope, ScopeConfig config) {
        return new SimpleProtoStoreImpl(SimpleStoreFactory.create(context, scope, config));
    }

    @Override
    public <T> void get(String key, Parser<T> parser, SimpleStore.Callback<T> callback, Executor executor) {
        simpleStore.get(key, new SimpleStore.Callback<byte[]>() {
            @Override
            public void onSuccess(@Nullable byte[] value) {
                if (value == null) {
                    executor.execute(() -> callback.onSuccess(null));
                    return;
                }
                try {
                    T parsed = parser.parseFrom(value);
                    executor.execute(() -> callback.onSuccess(parsed));
                } catch (InvalidProtocolBufferException e) {
                    executor.execute(() -> callback.onError(e));
                }
            }

            @Override
            public void onError(Throwable error) {
                executor.execute(() -> callback.onError(error));

            }
        }, SimpleStoreConfig.getComputationExecutor());
    }

    @Override
    public <T extends MessageLite> void put(String key, @Nullable T value, SimpleStore.Callback<T> callback, Executor executor) {
        byte[] bytes = null;
        if (value != null) {
            bytes = value.toByteArray();
        }
        simpleStore.put(key, bytes, new SimpleStore.Callback<byte[]>() {
            @Override
            public void onSuccess(@Nullable byte[] byteValue) {
                executor.execute(() -> callback.onSuccess(value));
            }

            @Override
            public void onError(Throwable error) {
                executor.execute(() -> callback.onError(error));
            }
        }, SimpleStoreConfig.getComputationExecutor());
    }

    @Override
    public void getString(String key, @Nonnull Callback<String> callback, @Nonnull Executor executor) {
        simpleStore.getString(key, callback, executor);
    }

    @Override
    public void putString(String key, @Nullable String value, @Nonnull Callback<String> callback, @Nonnull Executor executor) {
        simpleStore.putString(key, value, callback, executor);
    }

    @Override
    public void get(String key, @Nonnull Callback<byte[]> callback, @Nonnull Executor executor) {
        simpleStore.get(key, callback, executor);
    }

    @Override
    public void put(String key, @Nullable byte[] value, @Nonnull Callback<byte[]> callback, @Nonnull Executor executor) {
        simpleStore.put(key, value, callback, executor);
    }

    @Override
    public void close() {
        simpleStore.close();
    }

    @Override
    public void deleteAll(@Nonnull SimpleStore.Callback<Void> callback, @Nonnull Executor executor) {
        simpleStore.deleteAll(callback, executor);
    }

}
