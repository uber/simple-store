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
package com.uber.simplestore;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.Closeable;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/** Fast, reliable storage. */
public interface SimpleStore extends Closeable {

  /**
   * Retrieve a byte[]-backed String.
   *
   * @param key to fetch from
   */
  @CheckReturnValue
  ListenableFuture<String> getString(String key);

  /**
   * Stores a String as a byte[].
   *
   * @param key to store to
   * @param value to write
   */
  @CheckReturnValue
  ListenableFuture<String> putString(String key, @Nullable String value);

  /**
   * Retrieve a byte[] from disk.
   *
   * @param key to read from
   * @return value if present, empty array if absent
   */
  @CheckReturnValue
  ListenableFuture<byte[]> get(String key);

  /**
   * Stores a byte[] on disk.
   *
   * @param key to store to
   * @param value to store
   */
  @CheckReturnValue
  ListenableFuture<byte[]> put(String key, @Nullable byte[] value);

  /**
   * Removes a key from memory & disk.
   *
   * @param key to remove
   * @return when complete
   */
  @CheckReturnValue
  ListenableFuture<Void> remove(String key);

  /**
   * Determine if a key exists in storage.
   *
   * @param key to check
   * @return if key is set
   */
  @CheckReturnValue
  ListenableFuture<Boolean> contains(String key);

  /** Delete all keys in this direct namespace. */
  @CheckReturnValue
  ListenableFuture<Void> clear();

  /**
   * Recursively delete all keys in this scope and child scopes. Fails all outstanding operations on
   * the stores.
   */
  @Beta
  ListenableFuture<Void> deleteAllNow();

  /** Fails all outstanding operations then releases the memory cache. */
  @Override
  void close();
}
