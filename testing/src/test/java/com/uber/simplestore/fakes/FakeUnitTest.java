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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.StoreClosedException;
import java.io.IOException;
import org.junit.Test;

public class FakeUnitTest {

  private static final String TEST_KEY = "test";

  @Test
  public void saves() throws Exception {
    SimpleStore store = new FakeSimpleStore();
    store.putString(TEST_KEY, "bar").get();
    assertThat(store.getString(TEST_KEY).get()).isEqualTo("bar");
  }

  @Test
  public void nullClears() throws Exception {
    SimpleStore store = new FakeSimpleStore();
    store.putString(TEST_KEY, "bar").get();
    store.put(TEST_KEY, null).get();
    assertThat(store.contains(TEST_KEY).get()).isFalse();
  }

  @Test
  public void deleteAll_noChildren() throws Exception {
    try (SimpleStore store = new FakeSimpleStore()) {
      store.put(TEST_KEY, new byte[1]).get();
      store.deleteAll().get();
      ListenableFuture<byte[]> empty = store.get(TEST_KEY);
      assertThat(empty.get()).isEmpty();
    }
  }

  @Test
  public void handlesAbsence() throws Exception {
    SimpleStore store = new FakeSimpleStore();
    assertThat(store.getString(TEST_KEY).get()).isEqualTo("");
    assertThat(store.get(TEST_KEY).get()).hasLength(0);
    assertThat(store.contains(TEST_KEY).get()).isFalse();
  }

  @Test
  public void throwsAfterClose() throws Exception {
    SimpleStore store = new FakeSimpleStore();
    store.close();
    try {
      store.putString(TEST_KEY, "foo").get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(StoreClosedException.class);
    }
  }

  @Test
  public void supportsFailure() throws Exception {
    FakeSimpleStore store = new FakeSimpleStore();
    store.setFailureType(new IOException("foo"));

    ListenableFuture<String> future = store.getString(TEST_KEY);
    try {
      future.get();
      fail();
    } catch (Exception e) {
      assertThat(e).hasCauseThat().isInstanceOf(IOException.class);
    }
  }
}
