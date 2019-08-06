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
package com.uber.simplestore.impl;

import android.content.Context;
import android.util.Log;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.StoreClosedException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

/** Asynchronous storage implementation. */
@SuppressWarnings("UnstableApiUsage")
final class SimpleStoreImpl implements SimpleStore {
  private static final int OPEN = 0;
  private static final int CLOSED = 1;
  private static final int TOMBSTONED = 2;
  private static final byte[] EMPTY_BYTES = new byte[0];
  private static final Charset STRING_ENCODING = StandardCharsets.UTF_16BE;

  private final Context context;
  private final String namespace;
  @Nullable private File namespacedDirectory;

  AtomicInteger available = new AtomicInteger(OPEN);

  // Only touch from the serial executor.
  private final Map<String, byte[]> cache = new HashMap<>();
  private final Executor orderedIoExecutor =
      MoreExecutors.newSequentialExecutor(SimpleStoreConfig.getIOExecutor());
  @Nullable private Exception flush;

  SimpleStoreImpl(Context appContext, String namespace, NamespaceConfig config) {
    this.context = appContext;
    this.namespace = namespace;
    orderedIoExecutor.execute(
        () -> {
          File directory;
          if (config.equals(NamespaceConfig.CACHE)) {
            directory = context.getCacheDir();
          } else {
            directory = context.getFilesDir();
          }
          namespacedDirectory = new File(directory.getAbsolutePath() + "/simplestore/" + namespace);
          //noinspection ResultOfMethodCallIgnored
          namespacedDirectory.mkdirs();
        });
  }

  @Override
  public ListenableFuture<String> getString(String key) {
    return Futures.transform(
        get(key),
        (bytes) -> {
          if (bytes != null && bytes.length > 0) {
            return new String(bytes, STRING_ENCODING);
          } else {
            return "";
          }
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public ListenableFuture<String> putString(String key, @Nullable String value) {
    byte[] bytes;
    if (value == null || value.isEmpty()) {
      bytes = null;
    } else {
      bytes = value.getBytes(STRING_ENCODING);
    }
    return Futures.transform(put(key, bytes), (b) -> value, MoreExecutors.directExecutor());
  }

  @Override
  public ListenableFuture<byte[]> get(String key) {
    requireOpen();
    return Futures.submitAsync(
        () -> {
          Exception isDead = isDead();
          if (isDead != null) {
            return Futures.immediateFailedFuture(isDead);
          }
          byte[] value;
          if (cache.containsKey(key)) {
            value = cache.get(key);
          } else {
            try {
              value = readFile(key);
            } catch (IOException e) {
              return Futures.immediateFailedFuture(e);
            }
            if (value == null || value.length == 0) {
              value = EMPTY_BYTES;
            }
            cache.put(key, value);
          }
          return Futures.immediateFuture(value);
        },
        orderedIoExecutor);
  }

  @Override
  public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
    requireOpen();
    return Futures.submitAsync(
        () -> {
          Exception isDead = isDead();
          if (isDead != null) {
            return Futures.immediateFailedFuture(isDead);
          }
          if (value == null || value.length == 0) {
            cache.put(key, EMPTY_BYTES);
            deleteFile(key);
            return Futures.immediateFuture(EMPTY_BYTES);
          } else {
            cache.put(key, value);
            try {
              writeFile(key, value);
            } catch (IOException e) {
              return Futures.immediateFailedFuture(e);
            }
            return Futures.immediateFuture(value);
          }
        },
        orderedIoExecutor);
  }

  @Override
  public ListenableFuture<Boolean> contains(String key) {
    requireOpen();
    return Futures.transform(
        get(key),
        (value) -> value != null && value.length > 0,
        SimpleStoreConfig.getComputationExecutor());
  }

  @Override
  public ListenableFuture<Void> remove(String key) {
    return Futures.transform(
        put(key, null), (ignored) -> null, SimpleStoreConfig.getComputationExecutor());
  }

  @Override
  public ListenableFuture<Void> clear() {
    requireOpen();
    return Futures.submitAsync(
        () -> {
          Exception isDead = isDead();
          if (isDead != null) {
            return Futures.immediateFailedFuture(isDead);
          }
          try {
            File[] files = Objects.requireNonNull(namespacedDirectory).listFiles(File::isFile);
            if (files != null && files.length > 0) {
              for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
              }
            }
            //noinspection ResultOfMethodCallIgnored
            namespacedDirectory.delete();
            cache.clear();
          } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
          }
          return Futures.immediateFuture(null);
        },
        orderedIoExecutor);
  }

  @Override
  public ListenableFuture<Void> deleteAllNow() {
    SimpleStoreFactory.flushAndClearRecursive(this);

    return Futures.submitAsync(
        () -> {
          if (namespacedDirectory != null) {
            recursiveDelete(namespacedDirectory);
          }
          return Futures.immediateFuture(null);
        },
        orderedIoExecutor);
  }

  /** Only call from the orderedIoExecutor. */
  void clearCache() {
    cache.clear();
  }

  /**
   * Cause all items in the queue to fail out, then run something before enabling the queue again
   */
  void failQueueThenRun(Exception exception, Runnable runnable) {
    if (flush != null) {
      throw new IllegalStateException();
    }
    this.flush = exception;
    orderedIoExecutor.execute(
        () -> {
          runnable.run();
          this.flush = null;
        });
  }

  void moveAway() {
    if (namespacedDirectory == null) {
      return;
    }
    File newLocation = new File(namespacedDirectory.getAbsolutePath() + ".bak");
    if (!namespacedDirectory.renameTo(newLocation)) {
      Log.e(getClass().getName(), "moveAway rename failed");
      return;
    }
    namespacedDirectory = newLocation;
  }

  private static void recursiveDelete(File directory) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (File f : files) {
        recursiveDelete(f);
      }
    }
    //noinspection ResultOfMethodCallIgnored
    directory.delete();
  }

  @Override
  public void close() {
    if (available.compareAndSet(OPEN, CLOSED)) {
      orderedIoExecutor.execute(() -> SimpleStoreFactory.tombstone(SimpleStoreImpl.this));
    }
  }

  private void requireOpen() {
    if (available.get() > OPEN) {
      throw new StoreClosedException();
    }
  }

  @VisibleForTesting
  Executor getOrderedExecutor() {
    return orderedIoExecutor;
  }

  boolean tombstone() {
    return available.compareAndSet(CLOSED, TOMBSTONED);
  }

  String getNamespace() {
    return namespace;
  }

  boolean openIfClosed() {
    return available.compareAndSet(CLOSED, OPEN);
  }

  @Nullable
  private Exception isDead() {
    if (available.get() > CLOSED) {
      return new StoreClosedException();
    } else if (flush != null) {
      return flush;
    }
    return null;
  }

  private void deleteFile(String key) {
    File baseFile = new File(namespacedDirectory, key);
    AtomicFile file = new AtomicFile(baseFile);
    file.delete();
  }

  @Nullable
  private byte[] readFile(String key) throws IOException {
    File baseFile = new File(namespacedDirectory, key);
    AtomicFile file = new AtomicFile(baseFile);
    if (baseFile.exists()) {
      return file.readFully();
    } else {
      return null;
    }
  }

  private void writeFile(String key, byte[] value) throws IOException {
    File baseFile = new File(namespacedDirectory, key);
    AtomicFile file = new AtomicFile(baseFile);
    FileOutputStream writer = file.startWrite();
    writer.write(value);
    file.finishWrite(writer);
  }
}
