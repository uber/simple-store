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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import com.uber.simplestore.impl.SimpleStoreFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SanityEspressoTest {

  private static final String TEST_NAMESPACE = "test_namespace";
  private static final String KEY_ONE = "key_one";
  private static final String SAMPLE_STRING = "persisted_value";
  private static final byte[] SOME_BYTES = new byte[] {0xD, 0xE, 0xA, 0xD, 0x0, 0xB, 0xE, 0xE, 0xF};
  private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

  @Test
  public void defaultNamespace_bytes() throws Exception {
    SimpleStore store = SimpleStoreFactory.create(context, TEST_NAMESPACE);
    store.put(KEY_ONE, SOME_BYTES).get();
    store.close();

    SimpleStore storeTwo = SimpleStoreFactory.create(context, TEST_NAMESPACE);
    byte[] fromDisk = storeTwo.get(KEY_ONE).get();
    assertEquals(SOME_BYTES, fromDisk);
    storeTwo.close();
  }

  @Test
  public void defaultNamespace_string() throws Exception {
    SimpleStore store = SimpleStoreFactory.create(context, TEST_NAMESPACE);
    store.putString(KEY_ONE, SAMPLE_STRING).get();
    store.close();

    SimpleStore storeTwo = SimpleStoreFactory.create(context, TEST_NAMESPACE);
    String fromDisk = storeTwo.getString(KEY_ONE).get();
    assertEquals(SAMPLE_STRING, fromDisk);
    storeTwo.close();
  }

  @Test
  public void cache() throws Exception {
    SimpleStore store = SimpleStoreFactory.create(context, TEST_NAMESPACE, NamespaceConfig.CACHE);
    store.putString(KEY_ONE, SAMPLE_STRING).get();
    store.close();

    SimpleStore storeTwo =
        SimpleStoreFactory.create(context, TEST_NAMESPACE, NamespaceConfig.CACHE);
    String fromDisk = storeTwo.getString(KEY_ONE).get();
    assertEquals(SAMPLE_STRING, fromDisk);
    storeTwo.close();
  }
}
