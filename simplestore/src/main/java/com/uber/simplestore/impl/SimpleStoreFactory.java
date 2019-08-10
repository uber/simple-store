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

import android.content.Context;
import com.google.common.annotations.VisibleForTesting;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.GuardedBy;

/**
 * Obtain a SimpleStore interface that can read and write into a namespace.
 *
 * <p>Only one instance per namespace may exist at any time to guarentee FIFO-ordering within the
 * namespace. A namespace is a set of /-delimited strings that refer to a logical location on disk.
 * It is recommended a random UUID be used to both prevent collisions and to obfuscate the contents
 * on disk from rooted users.
 */
public final class SimpleStoreFactory {

  private static final Object namespacesLock = new Object();

  @GuardedBy("namespacesLock")
  private static Map<String, SimpleStoreImpl> namespaces = new HashMap<>();

  /**
   * Obtain a store for a namespace with default configuration.
   *
   * @param context to store in
   * @param namespace forward-slash delimited logical address
   * @return open store
   */
  public static SimpleStore create(Context context, String namespace) {
    return create(context, namespace, NamespaceConfig.DEFAULT);
  }

  /**
   * Obtain a store for a namespace.
   *
   * @param context to store in
   * @param namespace forward-slash delimited logical address
   * @param config to use
   * @return open store
   */
  public static SimpleStore create(Context context, String namespace, NamespaceConfig config) {
    Context appContext = context.getApplicationContext();
    SimpleStoreImpl store;
    synchronized (namespacesLock) {
      if (namespaces.containsKey(namespace)) {
        store = namespaces.get(namespace);
        if (!Objects.requireNonNull(store).openIfClosed()) {
          // Never let two references be issued.
          throw new IllegalStateException("namespace '" + namespace + "' already open");
        }
      } else {
        store = new SimpleStoreImpl(appContext, namespace, config);
        namespaces.put(namespace, store);
      }
    }
    return store;
  }

  static void tombstone(SimpleStoreImpl store) {
    synchronized (namespacesLock) {
      if (store.tombstone()) {
        namespaces.remove(store.getNamespace());
      }
    }
  }

  @VisibleForTesting
  public static void crashIfAnyOpen() {
    synchronized (namespacesLock) {
      for (Map.Entry<String, SimpleStoreImpl> e : namespaces.entrySet()) {
        if (e.getValue().available.get() == 0) {
          throw new IllegalStateException("Leaked namespace " + e.getKey());
        }
      }
    }
  }
}
