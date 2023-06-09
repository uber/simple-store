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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.impl.AndroidDirectoryProvider;
import com.uber.simplestore.proto.impl.SimpleProtoStoreFactory;
import com.uber.simplestore.proto.test.TestProto;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class SanityEspressoTest {

  private static final String TEST_NAMESPACE = "test_namespace";
  private static final String KEY_ONE = "key_one";
  private static final String SAMPLE_STRING = "persisted_value";
  private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
  private final DirectoryProvider directoryProvider = new AndroidDirectoryProvider(context);

  @Test
  public void defaultNamespace() throws Exception {
    SimpleProtoStore store = SimpleProtoStoreFactory.create(directoryProvider, TEST_NAMESPACE);
    TestProto.Basic proto = TestProto.Basic.newBuilder().setName(SAMPLE_STRING).build();
    store.put(KEY_ONE, proto).get();
    store.close();

    SimpleProtoStore storeTwo = SimpleProtoStoreFactory.create(directoryProvider, TEST_NAMESPACE);
    TestProto.Basic fromDisk = storeTwo.get(KEY_ONE, TestProto.Basic.parser()).get();
    assertEquals(proto.getName(), fromDisk.getName());
    storeTwo.close();
  }

  @Test
  public void cache() throws Exception {
    SimpleProtoStore store =
        SimpleProtoStoreFactory.create(directoryProvider, TEST_NAMESPACE, NamespaceConfig.CACHE);
    TestProto.Basic proto = TestProto.Basic.newBuilder().setName(SAMPLE_STRING).build();
    store.put(KEY_ONE, proto).get();
    store.close();

    SimpleProtoStore storeTwo =
        SimpleProtoStoreFactory.create(directoryProvider, TEST_NAMESPACE, NamespaceConfig.CACHE);
    TestProto.Basic fromDisk = storeTwo.get(KEY_ONE, TestProto.Basic.parser()).get();
    assertEquals(proto.getName(), fromDisk.getName());
    storeTwo.close();
  }
}
