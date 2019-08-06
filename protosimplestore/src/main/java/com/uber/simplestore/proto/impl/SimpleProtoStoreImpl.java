/*
 * Copyright (C) 2019. Uber Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uber.simplestore.proto.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.proto.SimpleProtoStore;
import javax.annotation.Nullable;

@SuppressWarnings("UnstableApiUsage")
public final class SimpleProtoStoreImpl implements SimpleProtoStore {
  private final SimpleStore simpleStore;
  private final NamespaceConfig config;

  SimpleProtoStoreImpl(SimpleStore simpleStore, NamespaceConfig config) {
    this.simpleStore = simpleStore;
    this.config = config;
  }

  @Override
  public <T extends MessageLite> ListenableFuture<T> get(String key, Parser<T> parser) {
    return Futures.transformAsync(
        simpleStore.get(key),
        (bytes) -> {
          T parsed;
          if (bytes == null || bytes.length == 0) {
            try {
              parsed = parser.parseFrom(ByteString.EMPTY);
            } catch (InvalidProtocolBufferException e) {
              // Has required fields, so we will pass this error forward.
              return Futures.immediateFailedFuture(e);
            }
          } else {
            try {
              parsed = parser.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
              if (config.equals(NamespaceConfig.CACHE)) {
                // A cache is allowed to be cleared whenever and we will try and give you a default
                // instance instead.
                return Futures.immediateFuture(parser.parseFrom(ByteString.EMPTY));
              } else {
                return Futures.immediateFailedFuture(e);
              }
            }
          }
          return Futures.immediateFuture(parsed);
        },
        SimpleStoreConfig.getComputationExecutor());
  }

  @Override
  public <T extends MessageLite> ListenableFuture<T> put(String key, @Nullable T value) {
    ListenableFuture<byte[]> proto =
        Futures.submitAsync(
            () -> {
              byte[] bytes = null;
              if (value != null && !value.equals(value.getDefaultInstanceForType())) {
                bytes = value.toByteArray();
              }
              return Futures.immediateFuture(bytes);
            },
            SimpleStoreConfig.getComputationExecutor());
    return Futures.transformAsync(
        proto,
        p ->
            Futures.transform(
                simpleStore.put(key, p), o -> value, SimpleStoreConfig.getComputationExecutor()),
        SimpleStoreConfig.getComputationExecutor());
  }

  @Override
  public ListenableFuture<Boolean> contains(String key) {
    return Futures.transform(
        get(key),
        value -> value != null && value.length > 0,
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
  public ListenableFuture<Void> remove(String key) {
    return simpleStore.remove(key);
  }

  @Override
  public ListenableFuture<Void> clear() {
    return simpleStore.clear();
  }

  @Override
  public ListenableFuture<Void> deleteAllNow() {
    return simpleStore.deleteAllNow();
  }

  @Override
  public void close() {
    simpleStore.close();
  }
}
