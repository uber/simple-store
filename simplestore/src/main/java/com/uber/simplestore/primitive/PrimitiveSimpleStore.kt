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
package com.uber.simplestore.primitive

import com.google.common.util.concurrent.ListenableFuture
import com.uber.simplestore.SimpleStore

/**
 * Store primitives on disk.
 *
 * All methods never return null in the suspend functions, [SimpleStore.contains] should be used for optionality. 
 * If the value is not set, the 0-byte primitive will be returned.
 */
interface PrimitiveSimpleStore : SimpleStore {

    suspend fun getInt(key: String): Int

    suspend fun put(key: String, value: Int): Int

    suspend fun getLong(key: String): Long

    suspend fun put(key: String, value: Long): Long

    suspend fun getBoolean(key: String): Boolean

    suspend fun put(key: String, value: Boolean): Boolean

    suspend fun getDouble(key: String): Double

    suspend fun put(key: String, value: Double): Double

    /**
     * Retrieves a [java.nio.charset.StandardCharsets.UTF_16BE] string.
     *
     * @param key to fetch from
     * @return value if present, otherwise ""
     */
    override suspend fun getString(key: String): String

    /**
     * Store string as [java.nio.charset.StandardCharsets.UTF_16BE].
     *
     * Putting "" will remove the value from disk.
     *
     * @param key name
     * @param value to store
     * @return stored value
     */
    suspend fun put(key: String, value: String): String

    override suspend fun remove(key: String)
}

/**
 * Extension functions to provide ListenableFuture compatibility for Java callers
 */
fun PrimitiveSimpleStore.getIntFuture(key: String): ListenableFuture<Int> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { getInt(key) }
    )
}

fun PrimitiveSimpleStore.putIntFuture(key: String, value: Int): ListenableFuture<Int> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}

fun PrimitiveSimpleStore.getLongFuture(key: String): ListenableFuture<Long> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { getLong(key) }
    )
}

fun PrimitiveSimpleStore.putLongFuture(key: String, value: Long): ListenableFuture<Long> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}

fun PrimitiveSimpleStore.getBooleanFuture(key: String): ListenableFuture<Boolean> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { getBoolean(key) }
    )
}

fun PrimitiveSimpleStore.putBooleanFuture(key: String, value: Boolean): ListenableFuture<Boolean> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}

fun PrimitiveSimpleStore.getDoubleFuture(key: String): ListenableFuture<Double> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { getDouble(key) }
    )
}

fun PrimitiveSimpleStore.putDoubleFuture(key: String, value: Double): ListenableFuture<Double> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}

fun PrimitiveSimpleStore.putStringFuture(key: String, value: String): ListenableFuture<String> {
    return kotlinx.coroutines.guava.asListenableFuture(
        kotlinx.coroutines.GlobalScope.async { put(key, value) }
    )
}