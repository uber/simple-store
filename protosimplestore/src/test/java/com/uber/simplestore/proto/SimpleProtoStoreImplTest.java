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
package com.uber.simplestore.proto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.proto.impl.SimpleProtoStoreFactory;
import com.uber.simplestore.proto.test.TestProto;
import java.nio.charset.Charset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@SuppressWarnings("UnstableApiUsage")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SimpleProtoStoreImplTest {
  private static final String TEST_KEY = "test";
  private static final String FOO = "foo";
  private Context context = RuntimeEnvironment.systemContext;

  @Test
  public void defaultInstanceWhenEmpty() throws Exception {
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      ListenableFuture<TestProto.Basic> future = store.get(TEST_KEY, TestProto.Basic.parser());
      assertThat(future.get()).isNotNull();
      assertThat(future.get()).isEqualTo(TestProto.Basic.getDefaultInstance());
    }
  }

  @Test
  public void defaultInstanceWhenEmpty_withRequiredField() throws Exception {
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      ListenableFuture<TestProto.Required> future =
          store.get(TEST_KEY, TestProto.Required.parser());
      try {
        Futures.getChecked(future, InvalidProtocolBufferException.class);
        fail();
      } catch (InvalidProtocolBufferException e) {
        // expected
        assertThat(e).hasMessageThat().contains("Message was missing required fields.");
      }
    }
  }

  @Test
  public void savesDefaultInstance() throws Exception {
    TestProto.Basic basic = TestProto.Basic.getDefaultInstance();
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      store.put(TEST_KEY, basic).get();
    }
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      TestProto.Basic out = store.get(TEST_KEY, TestProto.Basic.parser()).get();
      assertThat(out).isEqualTo(basic);
    }
  }

  @Test
  public void savesValue() throws Exception {
    TestProto.Basic basic = TestProto.Basic.newBuilder().setName(FOO).build();
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      store.put(TEST_KEY, basic).get();
    }
    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      TestProto.Basic out = store.get(TEST_KEY, TestProto.Basic.parser()).get();
      assertThat(out).isEqualTo(basic);
    }
  }

  @Test
  public void failsGracefullyOnParsingFail() throws Exception {
    try (SimpleStore simpleStore = SimpleProtoStoreFactory.create(context, "")) {
      simpleStore.put(TEST_KEY, "garbage".getBytes(Charset.defaultCharset())).get();
    }

    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      ListenableFuture<TestProto.Basic> out = store.get(TEST_KEY, TestProto.Basic.parser());
      try {
        Futures.getChecked(out, InvalidProtocolBufferException.class);
        fail();
      } catch (InvalidProtocolBufferException e) {
        // expected
      }
    }
  }

  @Test
  public void whenCache_returnsDefaultOnParseFailure() throws Exception {
    try (SimpleStore simpleStore = SimpleProtoStoreFactory.create(context, "")) {
      simpleStore.put(TEST_KEY, "garbage".getBytes(Charset.defaultCharset())).get();
    }

    try (SimpleProtoStore store =
        SimpleProtoStoreFactory.create(context, "", NamespaceConfig.CACHE)) {
      TestProto.Basic failed = store.get(TEST_KEY, TestProto.Basic.parser()).get();
      assertThat(failed).isEqualTo(TestProto.Basic.getDefaultInstance());
    }
  }
}
