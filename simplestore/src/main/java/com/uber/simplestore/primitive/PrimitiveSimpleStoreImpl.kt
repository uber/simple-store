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

import com.uber.simplestore.SimpleStore

internal class PrimitiveSimpleStoreImpl(
    private val simpleStore: SimpleStore
) : PrimitiveSimpleStore {

    override suspend fun getString(key: String): String {
        val value = simpleStore.getString(key)
        return value.ifEmpty { "" }
    }

    override suspend fun put(key: String, value: String): String {
        return simpleStore.putString(key, value)
    }

    override suspend fun putString(key: String, value: String?): String {
        return simpleStore.putString(key, value)
    }

    override suspend fun get(key: String): ByteArray {
        return simpleStore.get(key)
    }

    override suspend fun put(key: String, value: ByteArray?): ByteArray {
        return simpleStore.put(key, value)
    }

    override suspend fun contains(key: String): Boolean {
        return simpleStore.contains(key)
    }

    override suspend fun clear() {
        simpleStore.clear()
    }

    override suspend fun deleteAllNow() {
        simpleStore.deleteAllNow()
    }

    override fun close() {
        simpleStore.close()
    }

    override suspend fun getInt(key: String): Int {
        val bytes = get(key)
        return when {
            bytes.isEmpty() || bytes.size != 4 -> 0
            else -> {
                // decode big endian
                bytes[0].toInt() shl 24 or
                        (bytes[1].toInt() and 0xFF) shl 16 or
                        (bytes[2].toInt() and 0xFF) shl 8 or
                        (bytes[3].toInt() and 0xFF)
            }
        }
    }

    override suspend fun put(key: String, value: Int): Int {
        val bytes = if (value != 0) {
            // encode big endian
            byteArrayOf(
                (value shr 24).toByte(),
                (value shr 16).toByte(),
                (value shr 8).toByte(),
                value.toByte()
            )
        } else {
            null
        }
        put(key, bytes)
        return value
    }

    override suspend fun getLong(key: String): Long {
        val bytes = get(key)
        return when {
            bytes.isEmpty() || bytes.size != 8 -> 0L
            else -> {
                (bytes[0].toLong() and 0xFF) shl 56 or
                        (bytes[1].toLong() and 0xFF) shl 48 or
                        (bytes[2].toLong() and 0xFF) shl 40 or
                        (bytes[3].toLong() and 0xFF) shl 32 or
                        (bytes[4].toLong() and 0xFF) shl 24 or
                        (bytes[5].toLong() and 0xFF) shl 16 or
                        (bytes[6].toLong() and 0xFF) shl 8 or
                        (bytes[7].toLong() and 0xFF)
            }
        }
    }

    override suspend fun put(key: String, value: Long): Long {
        val bytes = if (value != 0L) {
            val bytes = ByteArray(8)
            var v = value
            // encode big endian
            for (i in 7 downTo 0) {
                bytes[i] = (v and 0xffL).toByte()
                v = v shr 8
            }
            bytes
        } else {
            null
        }
        put(key, bytes)
        return value
    }

    override suspend fun getBoolean(key: String): Boolean {
        val bytes = get(key)
        return bytes.isNotEmpty() && bytes[0] > 0
    }

    override suspend fun put(key: String, value: Boolean): Boolean {
        val bytes = if (value) {
            byteArrayOf(1)
        } else {
            byteArrayOf(0)
        }
        put(key, bytes)
        return value
    }

    override suspend fun getDouble(key: String): Double {
        val longValue = getLong(key)
        return Double.fromBits(longValue)
    }

    override suspend fun put(key: String, value: Double): Double {
        put(key, value.toBits())
        return value
    }

    override suspend fun remove(key: String) {
        simpleStore.remove(key)
    }
}