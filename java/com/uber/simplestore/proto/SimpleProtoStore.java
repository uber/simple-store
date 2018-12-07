package com.uber.simplestore.proto;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.uber.simplestore.SimpleStore;

import java.util.concurrent.Executor;

import javax.annotation.Nullable;

public interface SimpleProtoStore extends SimpleStore {
    <T> void get(String key, Parser<T> parser, SimpleStore.Callback<T> callback, Executor executor);

    <T extends MessageLite> void put(String key, @Nullable T value, SimpleStore.Callback<T> callback, Executor executor);
}
