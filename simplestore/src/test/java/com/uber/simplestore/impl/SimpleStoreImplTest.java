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
import javax.annotation.Nonnull;
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
  private static final byte[] VALUE_ONE = new byte[] {0xA, 0xB};
  private static final byte[] VALUE_TWO = new byte[] {0x1, 0x2};

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
      store.put(TEST_KEY, new byte[1]).get();
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
  public void deleteAll_noChildren() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      store.put(TEST_KEY, new byte[1]).get();
      store.deleteAll().get();
      ListenableFuture<byte[]> empty = store.get(TEST_KEY);
      assertThat(empty.get()).isEmpty();
    }
  }

  @Test
  public void deleteAll_withChildren() throws Exception {
    try (SimpleStore store = SimpleStoreFactory.create(context, "parent/child")) {
      store.put(TEST_KEY, new byte[1]).get();
    }
    try (SimpleStore store = SimpleStoreFactory.create(context, "parent")) {
      store.deleteAll().get();
    }
    try (SimpleStore store = SimpleStoreFactory.create(context, "parent/child")) {
      ListenableFuture<byte[]> empty = store.get(TEST_KEY);
      assertThat(empty.get()).isEmpty();
    }
  }

  @Test
  public void operationsBeforeCloseSucceed() throws Exception {
    ListenableFuture<byte[]> success;
    CountDownLatch latch;
    try (SimpleStore store = SimpleStoreFactory.create(context, "")) {
      success = store.put(TEST_KEY, VALUE_ONE);
      latch = enqueueBlockingOperation(store);
      // These callbacks are enqueued behind the slow write.
      Futures.addCallback(
          store.put(TEST_KEY, VALUE_TWO),
          new FutureCallback<byte[]>() {
            @Override
            public void onSuccess(@NullableDecl byte[] result) {
              assertThat(result).isEqualTo(VALUE_TWO);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
              fail(t.toString());
            }
          },
          directExecutor());
      Futures.addCallback(
          store.get(TEST_KEY),
          new FutureCallback<byte[]>() {
            @Override
            public void onSuccess(@NullableDecl byte[] result) {
              assertThat(result).isEqualTo(VALUE_TWO);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
              fail(t.toString());
            }
          },
          directExecutor());
      // This future is listened to.
      assertThat(success.get()).isEqualTo(VALUE_ONE);
    }
    latch.countDown(); // Finish the slow operation.
  }

  @Test
  public void operationsAfterCloseFail() throws Exception {
    SimpleStore store = SimpleStoreFactory.create(context, "");
    ListenableFuture<byte[]> success = store.put(TEST_KEY, VALUE_ONE);
    CountDownLatch latch = enqueueBlockingOperation(store);
    store.close();
    try {
      ListenableFuture<byte[]> badPut = store.put(TEST_KEY, VALUE_TWO);
      fail("Put after close succeeded");
      badPut.get();
    } catch (StoreClosedException ignored) {
      // expect failure
    }

    assertThat(success.get()).isEqualTo(VALUE_ONE); // doesn't matter when we get this

    try (SimpleStore storeTwo = SimpleStoreFactory.create(context, "")) {
      latch.countDown(); // simulate the close having been blocked on slow IO.
      assertThat(storeTwo.get(TEST_KEY).get()).isEqualTo(VALUE_ONE);
    }
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

  private CountDownLatch enqueueBlockingOperation(SimpleStore store) {
    CountDownLatch latch = new CountDownLatch(1);
    ((SimpleStoreImpl) store)
        .getOrderedExecutor()
        .execute(
            () -> {
              try {
                // very very slow IO.
                latch.await();
              } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
              }
            });
    return latch;
  }
}
