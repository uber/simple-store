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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.impl.AndroidDirectoryProvider;
import com.uber.simplestore.impl.SimpleStoreFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@SuppressWarnings("UnstableApiUsage")
@RunWith(RobolectricTestRunner.class)
public final class PrimitiveSimpleStoreTest {

  private static final String TEST_KEY = "test";
  private Context context =
      InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
  private final DirectoryProvider directoryProvider = new AndroidDirectoryProvider(context);

  @After
  public void reset() {
    SimpleStoreConfig.setIOExecutor(null);
    SimpleStoreFactory.crashIfAnyOpen();
  }

  @Test
  public void whenMissingOnDisk_numbers() throws Exception {
    try (PrimitiveSimpleStore store =
        PrimitiveSimpleStoreFactory.create(directoryProvider, "", NamespaceConfig.DEFAULT)) {
      assertThat(store.contains(TEST_KEY).get()).isFalse();

      Integer integer = store.getInt(TEST_KEY).get();
      assertThat(integer).isEqualTo(0);
      Long l = store.getLong(TEST_KEY).get();
      assertThat(l).isEqualTo(0L);
      Double d = store.getDouble(TEST_KEY).get();
      assertThat(d).isEqualTo(0.0);

      // Ensure we don't cache this wrong.
      assertThat(store.contains(TEST_KEY).get()).isFalse();
    }
  }

  @Test
  public void whenMissingOnDisk_string() throws Exception {
    try (PrimitiveSimpleStore store =
        PrimitiveSimpleStoreFactory.create(directoryProvider, "", NamespaceConfig.DEFAULT)) {
      assertThat(store.contains(TEST_KEY).get()).isFalse();

      String s = store.getString(TEST_KEY).get();
      assertThat(s).isNotNull();
      assertThat(s).isEmpty();

      assertThat(store.contains(TEST_KEY).get()).isFalse();
    }
  }

  @Test
  public void whenMissingOnDisk_boolean() throws Exception {
    try (PrimitiveSimpleStore store =
        PrimitiveSimpleStoreFactory.create(directoryProvider, "", NamespaceConfig.DEFAULT)) {
      assertThat(store.contains(TEST_KEY).get()).isFalse();

      boolean b = store.getBoolean(TEST_KEY).get();
      assertThat(b).isFalse();

      assertThat(store.contains(TEST_KEY).get()).isFalse();
    }
  }

  @Test
  public void put_Integer() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      ListenableFuture<Integer> future = store.put(TEST_KEY, Integer.MAX_VALUE);
      future.get();
      assertThat(store.getInt(TEST_KEY).get()).isEqualTo(Integer.MAX_VALUE);

      store.put(TEST_KEY, Integer.MIN_VALUE).get();
      assertThat(store.getInt(TEST_KEY).get()).isEqualTo(Integer.MIN_VALUE);

      assertThat(store.contains(TEST_KEY).get()).isTrue();
    }
  }

  @Test
  public void put_Long() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      ListenableFuture<Long> future = store.put(TEST_KEY, Long.MAX_VALUE);
      future.get();
      assertThat(store.getLong(TEST_KEY).get()).isEqualTo(Long.MAX_VALUE);

      store.put(TEST_KEY, Long.MIN_VALUE).get();
      assertThat(store.getLong(TEST_KEY).get()).isEqualTo(Long.MIN_VALUE);

      assertThat(store.contains(TEST_KEY).get()).isTrue();
    }
  }

  @Test
  public void put_Double() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      ListenableFuture<Double> future = store.put(TEST_KEY, Double.MAX_VALUE);
      future.get();
      assertThat(store.getDouble(TEST_KEY).get()).isEqualTo(Double.MAX_VALUE);
      assertThat(store.contains(TEST_KEY).get()).isTrue();
    }
  }

  @Test
  public void put_Boolean() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      ListenableFuture<Boolean> future = store.put(TEST_KEY, true);
      future.get();
      assertThat(store.getBoolean(TEST_KEY).get()).isTrue();
      assertThat(store.contains(TEST_KEY).get()).isTrue();

      store.put(TEST_KEY, false).get();
      assertThat(store.getBoolean(TEST_KEY).get()).isFalse();
      assertThat(store.contains(TEST_KEY).get()).isTrue();
    }
  }

  @Test
  public void put_String() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      store.put(TEST_KEY, "stuff").get();

      assertThat(store.getString(TEST_KEY).get()).isEqualTo("stuff");
      assertThat(store.contains(TEST_KEY).get()).isTrue();

      store.put(TEST_KEY, "").get();
      assertThat(store.contains(TEST_KEY).get()).isFalse();
      assertThat(store.getString(TEST_KEY).get()).isEqualTo("");
    }
  }

  @Test
  public void remove() throws Exception {
    try (PrimitiveSimpleStore store = PrimitiveSimpleStoreFactory.create(directoryProvider, "")) {
      store.putString(TEST_KEY, "junk").get();
      store.remove(TEST_KEY).get();
      assertThat(store.getInt(TEST_KEY).get()).isEqualTo(0);
      assertThat(store.getString(TEST_KEY).get()).isEqualTo("");
      assertThat(store.contains(TEST_KEY).get()).isFalse();
    }
  }
}
