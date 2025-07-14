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

import com.uber.simplestore.executors.StorageExecutors
import java.util.concurrent.Executor

/**
 * Configure executors used by SimpleStore.
 *
 * Set may only be called once, and should be called before any use of stores.
 */
object SimpleStoreConfig {

    private val writeLock = Any()

    @Volatile
    private var ioExecutor: Executor? = null

    @Volatile
    private var computationExecutor: Executor? = null

    fun getIOExecutor(): Executor {
        if (ioExecutor == null) {
            synchronized(writeLock) {
                if (ioExecutor == null) {
                    ioExecutor = StorageExecutors.ioExecutor()
                }
            }
        }
        return ioExecutor!!
    }

    /**
     * Override the executor used for IO operations.
     *
     * @param executor to set, null unsets.
     */
    fun setIOExecutor(executor: Executor?) {
        synchronized(writeLock) {
            ioExecutor = executor
        }
    }

    fun getComputationExecutor(): Executor {
        if (computationExecutor == null) {
            synchronized(writeLock) {
                if (computationExecutor == null) {
                    computationExecutor = StorageExecutors.computationExecutor()
                }
            }
        }
        return computationExecutor!!
    }

    /**
     * Override the executor used for computation.
     *
     * @param executor to set, null unsets.
     */
    fun setComputationExecutor(executor: Executor?) {
        synchronized(writeLock) {
            computationExecutor = executor
        }
    }
}