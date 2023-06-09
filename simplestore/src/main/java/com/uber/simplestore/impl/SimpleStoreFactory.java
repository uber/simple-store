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

import com.google.common.annotations.VisibleForTesting;
import com.uber.simplestore.DirectoryProvider;
import com.uber.simplestore.NamespaceConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.StoreClosedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
   * @param directoryProvider to store the files in
   * @param namespace forward-slash delimited logical address
   * @return open store
   */
  public static SimpleStore create(DirectoryProvider directoryProvider, String namespace) {
    return create(directoryProvider, namespace, NamespaceConfig.DEFAULT);
  }

  /**
   * Obtain a store for a namespace.
   *
   * @param directoryProvider to store the files in
   * @param namespace forward-slash delimited logical address
   * @param config to use
   * @return open store
   */
  public static SimpleStore create(
      DirectoryProvider directoryProvider, String namespace, NamespaceConfig config) {
    SimpleStoreImpl store;
    synchronized (namespacesLock) {
      if (namespaces.containsKey(namespace)) {
        store = namespaces.get(namespace);
        if (!Objects.requireNonNull(store).openIfClosed()) {
          // Never let two references be issued.
          throw new IllegalStateException("namespace '" + namespace + "' already open");
        }
      } else {
        store = new SimpleStoreImpl(directoryProvider, namespace, config);
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

  static void flushAndClearRecursive(SimpleStoreImpl store) {
    synchronized (namespacesLock) {
      store.failQueueThenRun(new StoreClosedException("deleteAllNow"), store::clearCache);
      List<SimpleStoreImpl> children = getOpenChildren(store.getNamespace());
      for (SimpleStoreImpl child : children) {
        child.failQueueThenRun(new StoreClosedException("parent deleteAllNow"), child::clearCache);
      }
      store.moveAway();
    }
  }

  @VisibleForTesting
  static List<SimpleStoreImpl> getOpenChildren(String scope) {
    List<SimpleStoreImpl> list = new ArrayList<>();
    synchronized (namespacesLock) {
      for (String key : namespaces.keySet()) {
        if (key.startsWith(scope) && !key.equals(scope)) {
          list.add(namespaces.get(key));
        }
      }
    }
    return list;
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
