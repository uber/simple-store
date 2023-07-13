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

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.uber.simplestore.SimpleStore

internal class PrimitiveSimpleStoreImpl(private val simpleStore: SimpleStore) : PrimitiveSimpleStore {
    override fun getString(key: String): ListenableFuture<String> {
        return Futures.transform(
            simpleStore.getString(key), { value: String? -> value ?: "" }, MoreExecutors.directExecutor()
        )
    }

    override fun put(key: String?, value: String?): ListenableFuture<String?>? {
        return simpleStore.putString(key, value)
    }

    override fun putString(key: String, value: String?): ListenableFuture<String> {
        return simpleStore.putString(key, value)
    }

    override fun get(key: String): ListenableFuture<ByteArray> {
        return simpleStore[key]
    }

    override fun put(key: String, value: ByteArray?): ListenableFuture<ByteArray> {
        return simpleStore.put(key, value)
    }

    override fun contains(key: String): ListenableFuture<Boolean> {
        return simpleStore.contains(key)
    }

    override fun clear(): ListenableFuture<Void> {
        return simpleStore.clear()
    }

    override fun deleteAllNow(): ListenableFuture<Void> {
        return simpleStore.deleteAllNow()
    }

    override fun close() {
        simpleStore.close()
    }

    override fun getInt(key: String?): ListenableFuture<Int?>? {
        return Futures.transform(
            get(key!!),
            { b: ByteArray? ->
                if (b == null || b.size != 4) {
                    return@transform 0
                }
                b[0].toInt() shl 24 or (b[1].toInt() and 0xFF shl 16) or (b[2].toInt() and 0xFF shl 8) or (b[3].toInt() and 0xFF)
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun put(key: String?, value: Int): ListenableFuture<Int?>? {
        val bytes: ByteArray?
        bytes = if (value != 0) {
            // encode big endian
            byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())
        } else {
            null
        }
        return Futures.transform(put(key!!, bytes), { v: ByteArray? -> value }, MoreExecutors.directExecutor())
    }

    override fun getLong(key: String?): ListenableFuture<Long?>? {
        return Futures.transform(
            get(key!!),
            { b: ByteArray? ->
                if (b == null || b.size != 8) {
                    return@transform 0L
                }
                b[0].toLong() and 0xFFL shl 56 or (b[1].toLong() and 0xFFL shl 48
                        ) or (b[2].toLong() and 0xFFL shl 40
                        ) or (b[3].toLong() and 0xFFL shl 32
                        ) or (b[4].toLong() and 0xFFL shl 24
                        ) or (b[5].toLong() and 0xFFL shl 16
                        ) or (b[6].toLong() and 0xFFL shl 8
                        ) or (b[7].toLong() and 0xFFL)
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun put(key: String?, value: Long): ListenableFuture<Long?>? {
        val bytes: ByteArray?
        if (value != 0L) {
            var v = value
            bytes = ByteArray(8)
            // encode big endian
            for (i in 7 downTo 0) {
                bytes[i] = (v and 0xffL).toByte()
                v = v shr 8
            }
        } else {
            bytes = null
        }
        return Futures.transform(put(key!!, bytes), { v: ByteArray? -> value }, MoreExecutors.directExecutor())
    }

    override fun getBoolean(key: String?): ListenableFuture<Boolean?>? {
        return Futures.transform(
            get(key!!), { b: ByteArray? -> b != null && b.size > 0 && b[0] > 0 }, MoreExecutors.directExecutor()
        )
    }

    override fun put(key: String?, value: Boolean): ListenableFuture<Boolean?>? {
        val bytes: ByteArray
        bytes = if (value) {
            byteArrayOf(1)
        } else {
            byteArrayOf(0)
        }
        return Futures.transform(put(key!!, bytes), { v: ByteArray? -> value }, MoreExecutors.directExecutor())
    }

    override fun getDouble(key: String?): ListenableFuture<Double?>? {
        return Futures.transform(getLong(key), { l: Long? ->
            java.lang.Double.longBitsToDouble(
                l!!
            )
        }, MoreExecutors.directExecutor())
    }

    override fun put(key: String?, value: Double): ListenableFuture<Double?>? {
        return Futures.transform(
            put(key, java.lang.Double.doubleToRawLongBits(value)), { v: Long? -> value }, MoreExecutors.directExecutor()
        )
    }

    override fun remove(key: String): ListenableFuture<Void> {
        return simpleStore.remove(key)
    }
}
