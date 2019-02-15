package com.uber.simplestore.fakes;

import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.StoreClosedException;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class FakeUnitTest {

    private static final String TEST_KEY = "test";

    @Test
    public void saves() throws Exception {
        SimpleStore store = new FakeSimpleStore();
        store.putString(TEST_KEY, "bar").get();
        assertThat(store.getString(TEST_KEY).get()).isEqualTo("bar");
    }

    @Test
    public void nullClears() throws Exception {
        SimpleStore store = new FakeSimpleStore();
        store.putString(TEST_KEY, "bar").get();
        store.put(TEST_KEY, null).get();
        assertThat(store.contains(TEST_KEY).get()).isFalse();
    }

    @Test
    public void throwsAfterClose() throws Exception {
        SimpleStore store = new FakeSimpleStore();
        store.close();
        try {
            store.putString(TEST_KEY, "foo").get();
            fail();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(StoreClosedException.class);
        }
    }

    @Test
    public void supportsFailure() throws Exception {
        FakeSimpleStore store = new FakeSimpleStore();
        store.setFailureType(new IOException("foo"));

        ListenableFuture<String> future = store.getString(TEST_KEY);
        try {
            future.get();
        } catch (Exception e) {
            assertThat(e).hasCauseThat().isInstanceOf(IOException.class);
        }
    }
}