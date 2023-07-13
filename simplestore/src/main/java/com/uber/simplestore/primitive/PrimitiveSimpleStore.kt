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
import javax.annotation.CheckReturnValue

/**
 * Store primitives on disk.
 *
 *
 * All methods never return null in the ListenableFuture, #[SimpleStore.contains]
 * should be used for optionality. If the value is not set, the 0-byte primitive will be returned.
 */
interface PrimitiveSimpleStore : SimpleStore {
    @CheckReturnValue
    fun getInt(key: String?): ListenableFuture<Int?>?

    @CheckReturnValue
    fun put(key: String?, value: Int): ListenableFuture<Int?>?

    @CheckReturnValue
    fun getLong(key: String?): ListenableFuture<Long?>?

    @CheckReturnValue
    fun put(key: String?, value: Long): ListenableFuture<Long?>?

    @CheckReturnValue
    fun getBoolean(key: String?): ListenableFuture<Boolean?>?

    @CheckReturnValue
    fun put(key: String?, value: Boolean): ListenableFuture<Boolean?>?

    @CheckReturnValue
    fun getDouble(key: String?): ListenableFuture<Double?>?

    @CheckReturnValue
    fun put(key: String?, value: Double): ListenableFuture<Double?>?

    /**
     * Retrieves a #[java.nio.charset.StandardCharsets.UTF_16BE] string.
     *
     * @param key to fetch from
     * @return value if present, otherwise ""
     */
    @CheckReturnValue
    override fun getString(key: String): ListenableFuture<String>

    /**
     * Store string as #[java.nio.charset.StandardCharsets.UTF_16BE].
     *
     *
     * Putting "" will remove the value from disk.
     *
     * @param key name
     * @param value to store
     * @return stored value
     */
    @CheckReturnValue
    fun put(key: String?, value: String?): ListenableFuture<String?>?
    @CheckReturnValue
    override fun remove(key: String): ListenableFuture<Void>
}
