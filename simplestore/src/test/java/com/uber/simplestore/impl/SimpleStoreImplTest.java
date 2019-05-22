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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.fail;

import android.content.Context;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.StoreClosedException;
import java.util.concurrent.CountDownLatch;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@SuppressWarnings("UnstableApiUsage")
@RunWith(RobolectricTestRunner.class)
public final class SimpleStoreImplTest {

  private static final String TEST_KEY = "test";
  private Context context = RuntimeEnvironment.systemContext;

  @After
  public void reset() {
    SimpleStoreConfig.setIOExecutor(null);
    SimpleStoreFactory.crashIfAnyOpen();
  }

  @Test
  public void zeroLengthWhenMissing() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      ListenableFuture<byte[]> future = store.get(TEST_KEY);
      assertThat(future.get()).hasLength(0);
    }
  }

  @Test
  public void puttingNullDeletesKey() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      ListenableFuture<byte[]> first = store.put(TEST_KEY, new byte[1]);
      ListenableFuture<byte[]> second = store.put(TEST_KEY, null);
      assertThat(second.get()).isEmpty();
    }
  }

  @Test
  public void putString() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      store.putString(TEST_KEY, "foo").get();
      assertThat(store.getString(TEST_KEY).get()).isEqualTo("foo");
      assertThat(store.contains(TEST_KEY).get()).isTrue();

      store.putString(TEST_KEY, "").get();
      assertThat(store.contains(TEST_KEY).get()).isFalse();
      assertThat(store.getString(TEST_KEY).get()).isEqualTo("");

      store.putString(TEST_KEY, null).get();
      assertThat(store.contains(TEST_KEY).get()).isFalse();
      assertThat(store.getString(TEST_KEY).get()).isEqualTo("");
    }
  }

  @Test
  public void deleteAll() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      ListenableFuture<byte[]> first = store.put(TEST_KEY, new byte[1]);
      ListenableFuture<Void> second = store.deleteAll();
      ListenableFuture<byte[]> empty = store.get(TEST_KEY);
      assertThat(empty.get()).isEmpty();
    }
  }

  @Test
  public void failsAllOnClose() throws Exception {
    ListenableFuture<byte[]> success;
    ListenableFuture<byte[]> read;
    ListenableFuture<byte[]> write;
    CountDownLatch latch = new CountDownLatch(1);
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      success = store.put(TEST_KEY, new byte[1]);
      SimpleStoreConfig.getIOExecutor()
          .execute(
              () -> {
                try {
                  // very very slow IO.
                  latch.await();
                } catch (InterruptedException ignored) {

                }
              });
      write = store.put(TEST_KEY, new byte[1]);
      read = store.get(TEST_KEY);
    }
    success.get();
    latch.countDown();
    Futures.addCallback(
        write,
        new FutureCallback<byte[]>() {
          @Override
          public void onSuccess(@NullableDecl byte[] result) {
            fail("write succeeded");
          }

          @Override
          public void onFailure(Throwable t) {
            assertThat(t.getClass()).isEqualTo(StoreClosedException.class);
          }
        },
        directExecutor());
    assertThat(Futures.successfulAsList(write, read).get()).containsExactly(null, null);
  }

  @Test
  public void closes() {
    SimpleStore store = SimpleStoreFactory.create(context, "");
    store.close();
  }

  @Test
  public void scopes() throws Exception {
    String someScope = "bar";
    String value = "some value";
    try (SimpleStore scopedBase =
        SimpleStoreFactory.create(context, someScope, ScopeConfig.DEFAULT)) {
      ListenableFuture<String> success = scopedBase.putString("foo", value);
      success.get();
    }

    try (SimpleStore scopedFactory =
        SimpleStoreFactory.create(context, someScope, ScopeConfig.DEFAULT)) {
      ListenableFuture<String> read = scopedFactory.getString("foo");
      assertThat(read.get()).isEqualTo(value);
    }
  }

  @Test
  public void oneInstancePerScope() {
    String someScope = "foo";
    try (SimpleStore ignoredOuter = SimpleStoreFactory.create(context, "")) {
      try (SimpleStore ignored =
          SimpleStoreFactory.create(context, someScope, ScopeConfig.DEFAULT)) {
        try {
          SimpleStoreFactory.create(context, someScope, ScopeConfig.DEFAULT);
          fail();
        } catch (IllegalStateException e) {
          // expected.
        }
      }
    }
  }
}
