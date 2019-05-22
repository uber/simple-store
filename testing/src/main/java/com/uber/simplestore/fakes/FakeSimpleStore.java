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
package com.uber.simplestore.fakes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.StoreClosedException;
import java.nio.charset.Charset;
import java.util.HashMap;
import javax.annotation.Nullable;

public final class FakeSimpleStore implements SimpleStore {

  private boolean closed = false;
  private final HashMap<String, byte[]> data;
  @Nullable private Throwable failureType;

  public FakeSimpleStore() {
    this.data = new HashMap<>();
  }

  @VisibleForTesting
  public void setFailureType(Throwable e) {
    this.failureType = e;
  }

  @Override
  public ListenableFuture<String> getString(String key) {
    String value;
    if (data.containsKey(key)) {
      value = new String(data.get(key), Charset.defaultCharset());
    } else {
      value = null;
    }
    return returnOrFail(value);
  }

  @Override
  public ListenableFuture<String> putString(String key, @Nullable String value) {
    if (value != null) {
      data.put(key, value.getBytes(Charset.defaultCharset()));
    } else {
      data.remove(key);
    }
    return returnOrFail(value);
  }

  @Override
  public ListenableFuture<byte[]> get(String key) {
    return returnOrFail(data.get(key));
  }

  @Override
  public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
    if (value != null) {
      data.put(key, value);
    } else {
      data.remove(key);
    }
    return returnOrFail(value);
  }

  @Override
  public ListenableFuture<Boolean> contains(String key) {
    return returnOrFail(data.containsKey(key));
  }

  @Override
  public ListenableFuture<Void> deleteAll() {
    data.clear();
    return returnOrFail(null);
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public ListenableFuture<Void> remove(String key) {
    data.remove(key);
    return returnOrFail(null);
  }

  private <T> ListenableFuture<T> returnOrFail(@Nullable T value) {
    if (closed) {
      throw new StoreClosedException();
    }
    if (failureType != null) {
      return Futures.immediateFailedFuture(failureType);
    }
    return Futures.immediateFuture(value);
  }
}
