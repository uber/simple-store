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

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.uber.simplestore.DirectoryProvider
import com.uber.simplestore.NamespaceConfig
import com.uber.simplestore.SimpleStoreConfig
import com.uber.simplestore.impl.AndroidDirectoryProvider
import com.uber.simplestore.impl.SimpleStoreFactory
import com.uber.simplestore.primitive.PrimitiveSimpleStoreFactory.create
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrimitiveSimpleStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val directoryProvider: DirectoryProvider = AndroidDirectoryProvider(context)
    @After
    fun reset() {
        SimpleStoreConfig.setIOExecutor(null)
        SimpleStoreFactory.crashIfAnyOpen()
    }

    @Test
    @Throws(Exception::class)
    fun whenMissingOnDisk_numbers() {
        create(directoryProvider, "", NamespaceConfig.DEFAULT).use { store ->
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
            val integer = store.getInt(TEST_KEY)!!.get()!!
            Truth.assertThat(integer).isEqualTo(0)
            val l = store.getLong(TEST_KEY)!!.get()!!
            Truth.assertThat(l).isEqualTo(0L)
            val d = store.getDouble(TEST_KEY)!!.get()!!
            Truth.assertThat(d).isEqualTo(0.0)

            // Ensure we don't cache this wrong.
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
        }
    }

    @Test
    @Throws(Exception::class)
    fun whenMissingOnDisk_string() {
        create(directoryProvider, "", NamespaceConfig.DEFAULT).use { store ->
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
            val s = store.getString(TEST_KEY).get()
            Truth.assertThat(s).isNotNull()
            Truth.assertThat(s).isEmpty()
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
        }
    }

    @Test
    @Throws(Exception::class)
    fun whenMissingOnDisk_boolean() {
        create(directoryProvider, "", NamespaceConfig.DEFAULT).use { store ->
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
            val b = store.getBoolean(TEST_KEY)!!.get()!!
            Truth.assertThat(b).isFalse()
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
        }
    }

    @Test
    @Throws(Exception::class)
    fun put_Integer() {
        create(directoryProvider, "").use { store ->
            val future = store.put(TEST_KEY, Int.MAX_VALUE)
            future!!.get()
            Truth.assertThat(store.getInt(TEST_KEY)!!.get()).isEqualTo(Int.MAX_VALUE)
            store.put(TEST_KEY, Int.MIN_VALUE)!!.get()
            Truth.assertThat(store.getInt(TEST_KEY)!!.get()).isEqualTo(Int.MIN_VALUE)
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
        }
    }

    @Test
    @Throws(Exception::class)
    fun put_Long() {
        create(directoryProvider, "").use { store ->
            val future = store.put(TEST_KEY, Long.MAX_VALUE)
            future!!.get()
            Truth.assertThat(store.getLong(TEST_KEY)!!.get()).isEqualTo(Long.MAX_VALUE)
            store.put(TEST_KEY, Long.MIN_VALUE)!!.get()
            Truth.assertThat(store.getLong(TEST_KEY)!!.get()).isEqualTo(Long.MIN_VALUE)
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
        }
    }

    @Test
    @Throws(Exception::class)
    fun put_Double() {
        create(directoryProvider, "").use { store ->
            val future = store.put(TEST_KEY, Double.MAX_VALUE)
            future!!.get()
            Truth.assertThat(store.getDouble(TEST_KEY)!!.get()).isEqualTo(Double.MAX_VALUE)
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
        }
    }

    @Test
    @Throws(Exception::class)
    fun put_Boolean() {
        create(directoryProvider, "").use { store ->
            val future = store.put(TEST_KEY, true)
            future!!.get()
            Truth.assertThat(store.getBoolean(TEST_KEY)!!.get()).isTrue()
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
            store.put(TEST_KEY, false)!!.get()
            Truth.assertThat(store.getBoolean(TEST_KEY)!!.get()).isFalse()
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
        }
    }

    @Test
    @Throws(Exception::class)
    fun put_String() {
        create(directoryProvider, "").use { store ->
            store.put(TEST_KEY, "stuff")!!.get()
            Truth.assertThat(store.getString(TEST_KEY).get()).isEqualTo("stuff")
            Truth.assertThat(store.contains(TEST_KEY).get()).isTrue()
            store.put(TEST_KEY, "")!!.get()
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
            Truth.assertThat(store.getString(TEST_KEY).get()).isEqualTo("")
        }
    }

    @Test
    @Throws(Exception::class)
    fun remove() {
        create(directoryProvider, "").use { store ->
            store.putString(TEST_KEY, "junk").get()
            store.remove(TEST_KEY).get()
            Truth.assertThat(store.getInt(TEST_KEY)!!.get()).isEqualTo(0)
            Truth.assertThat(store.getString(TEST_KEY).get()).isEqualTo("")
            Truth.assertThat(store.contains(TEST_KEY).get()).isFalse()
        }
    }

    companion object {
        private const val TEST_KEY = "test"
    }
}
