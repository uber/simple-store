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
package com.uber.simplestore.executors

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** Like MoreExecutors, but no Guava. */
object StorageExecutors {

    private val MAIN_EXECUTOR = MainThreadExecutor()

    private var ioThreadCount = 0
    private val IO_EXECUTOR = Executors.newCachedThreadPool { r ->
        Thread(r, "SimpleStoreIO-${ioThreadCount++}")
    }

    private var compThreadCount = 0
    private val COMPUTATION_EXECUTOR = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "SimpleStoreComp-${compThreadCount++}")
    }

    fun mainExecutor(): Executor = MAIN_EXECUTOR

    fun computationExecutor(): Executor = COMPUTATION_EXECUTOR

    fun ioExecutor(): Executor = IO_EXECUTOR

    // Coroutine dispatchers for modern Kotlin usage
    fun mainDispatcher() = Dispatchers.Main

    fun computationDispatcher() = COMPUTATION_EXECUTOR.asCoroutineDispatcher()

    fun ioDispatcher() = IO_EXECUTOR.asCoroutineDispatcher()

    private class MainThreadExecutor : Executor {
        private val handler = Handler(Looper.getMainLooper())

        override fun execute(r: Runnable) {
            handler.post(r)
        }
    }
}