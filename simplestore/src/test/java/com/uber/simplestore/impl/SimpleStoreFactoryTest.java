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

import android.content.Context;
import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.SimpleStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class SimpleStoreFactoryTest {

  private Context context = RuntimeEnvironment.systemContext;
  private final DirectoryProvider directoryProvider = new AndroidDirectoryProvider(context);

  @Test
  public void findChildren() {
    SimpleStore root = SimpleStoreFactory.create(directoryProvider, "");
    SimpleStore parent = SimpleStoreFactory.create(directoryProvider, "parent");
    SimpleStore child = SimpleStoreFactory.create(directoryProvider, "parent/child");
    SimpleStore leaf = SimpleStoreFactory.create(directoryProvider, "leaf");

    assertThat(SimpleStoreFactory.getOpenChildren("leaf")).isEmpty();
    assertThat(SimpleStoreFactory.getOpenChildren("parent/child")).isEmpty();
    assertThat(SimpleStoreFactory.getOpenChildren("parent")).containsExactly(child);
    assertThat(SimpleStoreFactory.getOpenChildren("")).containsExactly(parent, child, leaf);

    root.close();
    parent.close();
    child.close();
    leaf.close();
  }
}
