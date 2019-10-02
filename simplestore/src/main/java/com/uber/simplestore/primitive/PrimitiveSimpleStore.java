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
package com.uber.simplestore.primitive;

import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import javax.annotation.CheckReturnValue;

/**
 * Store primitives on disk.
 *
 * <p>All methods never return null in the ListenableFuture, #{@link SimpleStore#contains(String)}
 * should be used for optionality. If the value is not set, the 0-byte primitive will be returned.
 */
public interface PrimitiveSimpleStore extends SimpleStore {

  @CheckReturnValue
  ListenableFuture<Integer> getInt(String key);

  @CheckReturnValue
  ListenableFuture<Integer> put(String key, int value);

  @CheckReturnValue
  ListenableFuture<Long> getLong(String key);

  @CheckReturnValue
  ListenableFuture<Long> put(String key, long value);

  @CheckReturnValue
  ListenableFuture<Boolean> getBoolean(String key);

  @CheckReturnValue
  ListenableFuture<Boolean> put(String key, boolean value);

  @CheckReturnValue
  ListenableFuture<Double> getDouble(String key);

  @CheckReturnValue
  ListenableFuture<Double> put(String key, double value);

  /**
   * Retrieves a #{@link java.nio.charset.StandardCharsets#UTF_16BE} string.
   *
   * @param key to fetch from
   * @return value if present, otherwise ""
   */
  @CheckReturnValue
  @Override
  ListenableFuture<String> getString(String key);

  /**
   * Store string as #{@link java.nio.charset.StandardCharsets#UTF_16BE}.
   *
   * <p>Putting "" will remove the value from disk.
   *
   * @param key name
   * @param value to store
   * @return stored value
   */
  @CheckReturnValue
  ListenableFuture<String> put(String key, String value);

  @CheckReturnValue
  @Override
  ListenableFuture<Void> remove(String key);
}
