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
package com.uber.simplestore.impl

import com.google.common.annotations.VisibleForTesting
import com.uber.simplestore.DirectoryProvider
import com.uber.simplestore.NamespaceConfig
import com.uber.simplestore.SimpleStore
import com.uber.simplestore.StoreClosedException

/**
 * Obtain a SimpleStore interface that can read and write into a namespace.
 *
 * Only one instance per namespace may exist at any time to guarentee FIFO-ordering within the
 * namespace. A namespace is a set of /-delimited strings that refer to a logical location on disk.
 * It is recommended a random UUID be used to both prevent collisions and to obfuscate the contents
 * on disk from rooted users.
 */
object SimpleStoreFactory {

    private val namespacesLock = Any()
    private val namespaces = mutableMapOf<String, SimpleStoreImpl>()

    /**
     * Obtain a store for a namespace with default configuration.
     *
     * @param directoryProvider to store the files in
     * @param namespace forward-slash delimited logical address
     * @return open store
     */
    fun create(directoryProvider: DirectoryProvider, namespace: String): SimpleStore {
        return create(directoryProvider, namespace, NamespaceConfig.DEFAULT)
    }

    /**
     * Obtain a store for a namespace.
     *
     * @param directoryProvider to store the files in
     * @param namespace forward-slash delimited logical address
     * @param config to use
     * @return open store
     */
    fun create(
        directoryProvider: DirectoryProvider,
        namespace: String,
        config: NamespaceConfig
    ): SimpleStore {
        return synchronized(namespacesLock) {
            val store = namespaces[namespace]
            when {
                store != null -> {
                    if (!store.openIfClosed()) {
                        // Never let two references be issued.
                        throw IllegalStateException("namespace '$namespace' already open")
                    }
                    store
                }
                else -> {
                    val newStore = SimpleStoreImpl(directoryProvider, namespace, config)
                    namespaces[namespace] = newStore
                    newStore
                }
            }
        }
    }

    fun tombstone(store: SimpleStoreImpl) {
        synchronized(namespacesLock) {
            if (store.tombstone()) {
                namespaces.remove(store.getNamespace())
            }
        }
    }

    fun flushAndClearRecursive(store: SimpleStoreImpl) {
        synchronized(namespacesLock) {
            store.failQueueThenRun(StoreClosedException("deleteAllNow")) {
                store.clearCache()
            }
            val children = getOpenChildren(store.getNamespace())
            children.forEach { child ->
                child.failQueueThenRun(StoreClosedException("parent deleteAllNow")) {
                    child.clearCache()
                }
            }
            store.moveAway()
        }
    }

    @VisibleForTesting
    fun getOpenChildren(scope: String): List<SimpleStoreImpl> {
        return synchronized(namespacesLock) {
            namespaces.entries
                .filter { (key, _) -> key.startsWith(scope) && key != scope }
                .map { it.value }
                .toList()
        }
    }

    @VisibleForTesting
    fun crashIfAnyOpen() {
        synchronized(namespacesLock) {
            namespaces.forEach { (key, value) ->
                if (value.available.get() == 0) {
                    throw IllegalStateException("Leaked namespace $key")
                }
            }
        }
    }
}