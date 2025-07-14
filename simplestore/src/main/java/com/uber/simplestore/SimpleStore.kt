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
package com.uber.simplestore

import com.google.common.annotations.Beta
import com.google.common.util.concurrent.ListenableFuture
import java.io.Closeable

/** Fast, reliable storage. */
interface SimpleStore : Closeable {

    /**
     * Retrieve a byte[]-backed String.
     *
     * @param key to fetch from
     */
    suspend fun getString(key: String): String

    /**
     * Stores a String as a byte[].
     *
     * @param key to store to
     * @param value to write
     */
    suspend fun putString(key: String, value: String?): String

    /**
     * Retrieve a byte[] from disk.
     *
     * @param key to read from
     * @return value if present, empty array if absent
     */
    suspend fun get(key: String): ByteArray

    /**
     * Stores a byte[] on disk.
     *
     * @param key to store to
     * @param value to store
     */
    suspend fun put(key: String, value: ByteArray?): ByteArray

    /**
     * Removes a key from memory & disk.
     *
     * @param key to remove
     * @return when complete
     */
    suspend fun remove(key: String)

    /**
     * Determine if a key exists in storage.
     *
     * @param key to check
     * @return if key is set
     */
    suspend fun contains(key: String): Boolean

    /** Delete all keys in this direct namespace. */
    suspend fun clear()

    /**
     * Recursively delete all keys in this scope and child scopes. Fails all outstanding operations on
     * the stores.
     */
    @Beta
    suspend fun deleteAllNow()

    /** Fails all outstanding operations then releases the memory cache. */
    override fun close()
}

/**
 * Extension functions to provide ListenableFuture compatibility for Java callers
 */
fun SimpleStore.getStringFuture(key: String): ListenableFuture<String> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { getString(key) }
    )
}

fun SimpleStore.putStringFuture(key: String, value: String?): ListenableFuture<String> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { putString(key, value) }
    )
}

fun SimpleStore.getFuture(key: String): ListenableFuture<ByteArray> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { get(key) }
    )
}

fun SimpleStore.putFuture(key: String, value: ByteArray?): ListenableFuture<ByteArray> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}

fun SimpleStore.removeFuture(key: String): ListenableFuture<Void> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { 
            remove(key)
            null
        }
    )
}

fun SimpleStore.containsFuture(key: String): ListenableFuture<Boolean> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { contains(key) }
    )
}

fun SimpleStore.clearFuture(): ListenableFuture<Void> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { 
            clear()
            null
        }
    )
}

fun SimpleStore.deleteAllNowFuture(): ListenableFuture<Void> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { 
            deleteAllNow()
            null
        }
    )
}