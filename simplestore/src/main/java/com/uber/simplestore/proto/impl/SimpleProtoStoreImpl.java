package com.uber.simplestore.proto.impl;

import android.content.Context;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
    public <T extends MessageLite> ListenableFuture<T> get(String key, Parser<T> parser) {
        return Futures.transformAsync(simpleStore.get(key), (bytes) -> {
            T parsed = parser.parseFrom(bytes);
            return Futures.immediateFuture(parsed);
        }, SimpleStoreConfig.getComputationExecutor());
    }

    @Override
    public <T extends MessageLite> ListenableFuture<T> put(String key, @Nullable T value) {
        ListenableFuture<byte[]> proto = Futures.submitAsync(() -> {
            byte[] bytes = null;
            if (value != null) {
                bytes = value.toByteArray();
            }
            return Futures.immediateFuture(bytes);
        }, SimpleStoreConfig.getComputationExecutor());
        return Futures.transformAsync(proto,
                p -> Futures.transform(simpleStore.put(key, p), o -> value,
                        SimpleStoreConfig.getComputationExecutor()),
                SimpleStoreConfig.getComputationExecutor());
    }

    @Override
    public ListenableFuture<String> getString(String key) {
        return simpleStore.getString(key);
    }

    @Override
    public ListenableFuture<String> putString(String key, @Nullable String value) {
        return simpleStore.putString(key, value);
    }

    @Override
    public ListenableFuture<byte[]> get(String key) {
        return simpleStore.get(key);
    }

    @Override
    public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
        return simpleStore.put(key, value);
    }

    @Override
    public ListenableFuture<Void> deleteAll() {
        return simpleStore.deleteAll();
    }

    @Override
    public void close() {
        simpleStore.close();
    }

}
