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

import net.jodah.concurrentunit.Waiter
import java.util.concurrent.Executors


/**
 * A utility class to execute a given action concurrently.
 * It will trigger [Waiter.fail] if the action throws an exception during the whole concurrent execution process.
 * It will trigger [Waiter.resume] if the action is concluded successfully for the whole concurrent execution.
 */
object ConcurrentTestUtil {
    /**
     * The maximum number of threads to be executed concurrently.
     */
    private const val MAX_THREAD = 5


    /**
     * Executes the given action concurrently.
     * It will trigger [Waiter.fail] if the action throws an exception during the whole concurrent execution process.
     * It will trigger [Waiter.resume] if the action is concluded successfully for the whole concurrent execution.
     */
    fun executeConcurrent(action: () -> Unit) {
        val waiter = Waiter()
        for (i in 0 until 1000) {
            val service = Executors.newFixedThreadPool(MAX_THREAD)
            for (j in 0 until 10) {
                service.submit {
                    executeConcurrentInternally(action, waiter)
                }
            }
        }
        waiter.await(1000L * MAX_THREAD)
    }

    private fun executeConcurrentInternally(action: () -> Unit, waiter: Waiter) {
        try {
            run(action)
        } catch (e: Exception) {
            waiter.fail(e)
        } finally {
            waiter.resume()
        }
    }


}
