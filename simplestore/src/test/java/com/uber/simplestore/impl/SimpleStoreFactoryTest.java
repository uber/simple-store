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
import static org.junit.Assert.*;

import android.content.Context;
import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.SimpleStore;
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SimpleStoreFactoryTest {

  private final Context context = RuntimeEnvironment.systemContext;
  private final DirectoryProvider directoryProvider = new AndroidDirectoryProvider(context);

  @Nullable private SimpleStore root = null;

  @Nullable private SimpleStore parent = null;

  @Nullable private SimpleStore child = null;

  @Nullable private SimpleStore leaf = null;

  @Before
  public void setUp() {
    root = SimpleStoreFactory.create(directoryProvider, "");
    parent = SimpleStoreFactory.create(directoryProvider, "parent");
    child = SimpleStoreFactory.create(directoryProvider, "parent/child");
    leaf = SimpleStoreFactory.create(directoryProvider, "leaf");
  }

  @Test
  public void findChildren() {
    assertThat(SimpleStoreFactory.getOpenChildren("leaf")).isEmpty();
    assertThat(SimpleStoreFactory.getOpenChildren("parent/child")).isEmpty();
    assertThat(SimpleStoreFactory.getOpenChildren("parent")).containsExactly(child);
    assertThat(SimpleStoreFactory.getOpenChildren("")).containsExactly(parent, child, leaf);
  }

  @Test(expected = IllegalStateException.class)
  public void createShouldThrowAExceptionWhenInstanceAlreadyExist() {
    SimpleStoreFactory.create(directoryProvider, "");
  }

  @Test
  public void getOrCreateShouldReturnAnInstanceWhenInstanceAlreadyExist() {
    SimpleStore store = SimpleStoreFactory.getOrCreate(directoryProvider, "");
    assertEquals(root, store);
  }

  @Test
  public void getOrCreateShouldReturnAnNewInstanceForClosedNamespaces() {
    SimpleStore store = SimpleStoreFactory.getOrCreate(directoryProvider, "test");
    assertNotNull(store);
    store.close();
    store = SimpleStoreFactory.getOrCreate(directoryProvider, "test");
    assertNotNull(store);
    store.close();
  }

  @After
  public void tearDown() {
    Objects.requireNonNull(root).close();
    Objects.requireNonNull(parent).close();
    Objects.requireNonNull(child).close();
    Objects.requireNonNull(leaf).close();
  }
}
