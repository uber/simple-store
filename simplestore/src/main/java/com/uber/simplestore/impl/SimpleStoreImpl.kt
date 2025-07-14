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

import android.util.Log
import com.google.common.annotations.VisibleForTesting
import com.uber.simplestore.DirectoryProvider
import com.uber.simplestore.NamespaceConfig
import com.uber.simplestore.SimpleStore
import com.uber.simplestore.SimpleStoreConfig
import com.uber.simplestore.StoreClosedException
import com.uber.simplestore.executors.StorageExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Asynchronous storage implementation. */
internal class SimpleStoreImpl(
    directoryProvider: DirectoryProvider,
    private val namespace: String,
    config: NamespaceConfig
) : SimpleStore {

    companion object {
        private const val OPEN = 0
        private const val CLOSED = 1
        private const val TOMBSTONED = 2
        private val EMPTY_BYTES = ByteArray(0)
        private val STRING_ENCODING: Charset = StandardCharsets.UTF_16BE
    }

    private var namespacedDirectory: File? = null
    private val available = AtomicInteger(OPEN)

    // Only touch from the serial executor.
    private val cache = mutableMapOf<String, ByteArray>()
    private val orderedIoExecutor: Executor = StorageExecutors.ioExecutor()
    private val flush = AtomicReference<Exception?>(null)

    init {
        orderedIoExecutor.execute {
            val directory = when (config) {
                is NamespaceConfig.CACHE -> directoryProvider.cacheDirectoryPath()
                else -> directoryProvider.filesDirectoryPath()
            }
            namespacedDirectory = File("${directory.absolutePath}/simplestore/$namespace")
            namespacedDirectory?.mkdirs()
        }
    }

    override suspend fun getString(key: String): String {
        val bytes = get(key)
        return if (bytes.isNotEmpty()) {
            String(bytes, STRING_ENCODING)
        } else {
            ""
        }
    }

    override suspend fun putString(key: String, value: String?): String {
        val bytes = when {
            value.isNullOrEmpty() -> null
            else -> value.toByteArray(STRING_ENCODING)
        }
        put(key, bytes)
        return value ?: ""
    }

    override suspend fun get(key: String): ByteArray {
        requireOpen()
        return withContext(Dispatchers.IO) {
            val isDead = isDead()
            if (isDead != null) {
                throw isDead
            }
            
            val value = cache[key] ?: run {
                try {
                    readFile(key)?.let { bytes ->
                        if (bytes.isEmpty()) EMPTY_BYTES else bytes
                    } ?: EMPTY_BYTES
                } catch (e: IOException) {
                    throw e
                }.also { bytes ->
                    cache[key] = bytes
                }
            }
            value
        }
    }

    override suspend fun put(key: String, value: ByteArray?): ByteArray {
        requireOpen()
        return withContext(Dispatchers.IO) {
            val isDead = isDead()
            if (isDead != null) {
                throw isDead
            }
            
            when {
                value.isNullOrEmpty() -> {
                    cache[key] = EMPTY_BYTES
                    deleteFile(key)
                    EMPTY_BYTES
                }
                else -> {
                    cache[key] = value
                    try {
                        writeFile(key, value)
                        value
                    } catch (e: IOException) {
                        throw e
                    }
                }
            }
        }
    }

    override suspend fun contains(key: String): Boolean {
        requireOpen()
        val value = get(key)
        return value.isNotEmpty()
    }

    override suspend fun remove(key: String) {
        put(key, null)
    }

    override suspend fun clear() {
        requireOpen()
        withContext(Dispatchers.IO) {
            val isDead = isDead()
            if (isDead != null) {
                throw isDead
            }
            
            try {
                namespacedDirectory?.listFiles { it.isFile }?.forEach { file ->
                    file.delete()
                }
                namespacedDirectory?.delete()
                cache.clear()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    override suspend fun deleteAllNow() {
        SimpleStoreFactory.flushAndClearRecursive(this)
        
        withContext(Dispatchers.IO) {
            namespacedDirectory?.let { recursiveDelete(it) }
        }
    }

    override fun close() {
        if (available.compareAndSet(OPEN, CLOSED)) {
            orderedIoExecutor.execute { SimpleStoreFactory.tombstone(this) }
        }
    }

    /** Only call from the orderedIoExecutor. */
    fun clearCache() {
        cache.clear()
    }

    /**
     * Cause all items in the queue to fail out, then run something before enabling the queue again
     */
    fun failQueueThenRun(exception: Exception, runnable: Runnable) {
        if (!flush.compareAndSet(null, exception)) {
            throw IllegalStateException()
        }
        orderedIoExecutor.execute {
            runnable.run()
            flush.set(null)
        }
    }

    fun moveAway() {
        val currentDir = namespacedDirectory ?: return
        val newLocation = File("${currentDir.absolutePath}.bak")
        if (!currentDir.renameTo(newLocation)) {
            Log.e(javaClass.name, "moveAway rename failed")
            return
        }
        namespacedDirectory = newLocation
    }

    private fun recursiveDelete(directory: File) {
        directory.listFiles()?.forEach { file ->
            recursiveDelete(file)
        }
        directory.delete()
    }

    private fun requireOpen() {
        if (available.get() > OPEN) {
            throw StoreClosedException()
        }
    }

    @VisibleForTesting
    fun getOrderedExecutor(): Executor = orderedIoExecutor

    fun tombstone(): Boolean = available.compareAndSet(CLOSED, TOMBSTONED)

    fun getNamespace(): String = namespace

    fun openIfClosed(): Boolean = available.compareAndSet(CLOSED, OPEN)

    private fun isDead(): Exception? {
        return when {
            available.get() > CLOSED -> StoreClosedException()
            else -> flush.get()
        }
    }

    private fun deleteFile(key: String) {
        val baseFile = File(namespacedDirectory, key)
        val file = AtomicFile(baseFile)
        file.delete()
    }

    private fun readFile(key: String): ByteArray? {
        val baseFile = File(namespacedDirectory, key)
        val file = AtomicFile(baseFile)
        return if (baseFile.exists()) {
            file.readFully()
        } else {
            null
        }
    }

    private fun writeFile(key: String, value: ByteArray) {
        val baseFile = File(namespacedDirectory, key)
        val file = AtomicFile(baseFile)
        val writer = file.startWrite()
        writer.write(value)
        file.finishWrite(writer)
    }
}