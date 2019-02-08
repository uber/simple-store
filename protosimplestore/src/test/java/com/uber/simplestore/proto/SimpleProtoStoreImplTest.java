package com.uber.simplestore.proto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.proto.impl.SimpleProtoStoreFactory;
import com.uber.simplestore.proto.test.TestProto;
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
      simpleStore.put(TEST_KEY, "garbage".getBytes()).get();
    }

    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "")) {
      ListenableFuture<TestProto.Basic> out = store.get(TEST_KEY, TestProto.Basic.parser());
      try {
        Futures.getChecked(out, InvalidProtocolBufferException.class);
        fail();
      } catch (InvalidProtocolBufferException e) {
        // expected
        assertThat(e).hasMessageThat().contains("invalid wire type");
      }
    }
  }

  @Test
  public void whenCache_returnsDefaultOnParseFailure() throws Exception {
    try (SimpleStore simpleStore = SimpleProtoStoreFactory.create(context, "")) {
      simpleStore.put(TEST_KEY, "garbage".getBytes()).get();
    }

    try (SimpleProtoStore store = SimpleProtoStoreFactory.create(context, "", ScopeConfig.CACHE)) {
      TestProto.Basic failed = store.get(TEST_KEY, TestProto.Basic.parser()).get();
      assertThat(failed).isEqualTo(TestProto.Basic.getDefaultInstance());
    }
  }
}
