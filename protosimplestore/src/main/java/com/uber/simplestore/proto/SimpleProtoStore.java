package com.uber.simplestore.proto;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.uber.simplestore.SimpleStore;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

public interface SimpleProtoStore extends SimpleStore {
  @CheckReturnValue
  <T extends MessageLite> ListenableFuture<T> get(String key, Parser<T> parser);

  @CheckReturnValue
  <T extends MessageLite> ListenableFuture<T> put(String key, @Nullable T value);
}
