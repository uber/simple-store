package com.ubercab.simplestorage;

import android.content.Context;

import com.google.common.util.concurrent.MoreExecutors;
import com.ubercab.simplestorage.impl.SimpleStoreImplFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class SimpleStoreImplTest {

    private static final String TEST_KEY = "test";
    private Context context = RuntimeEnvironment.systemContext;

    @Test
    public void nullWhenMissing() {
        try(SimpleStore store = SimpleStoreImplFactory.get(context)) {
            BlockingResult<byte[]> missing = new BlockingResult<>();
            store.get(TEST_KEY, missing, MoreExecutors.directExecutor());
            Assert.assertTrue(missing.isSuccess());
            Assert.assertNull(missing.getSuccessful());
        }
    }

    @Test
    public void puttingNullDeletesKey() {
        try(SimpleStore store = SimpleStoreImplFactory.get(context)) {
            BlockingResult<byte[]> saved = new BlockingResult<>();
            store.put(TEST_KEY, new byte[1], saved, MoreExecutors.directExecutor());
            saved.getSuccessful();
            BlockingResult<byte[]> done = new BlockingResult<>();
            store.put(TEST_KEY, null, done, MoreExecutors.directExecutor());
            Assert.assertNull(done.getSuccessful());
        }
    }

    @Test
    public void deleteAll() {
        try(SimpleStore store = SimpleStoreImplFactory.get(context)) {
            BlockingResult<byte[]> saved = new BlockingResult<>();
            store.put(TEST_KEY, new byte[1], saved, MoreExecutors.directExecutor());
            saved.getSuccessful();
            BlockingResult<Void> deleted = new BlockingResult<>();
            store.deleteAll(deleted, MoreExecutors.directExecutor());
            deleted.getSuccessful();
            BlockingResult<byte[]> emptyResult = new BlockingResult<>();
            store.get(TEST_KEY, emptyResult, MoreExecutors.directExecutor());
            assertNull(emptyResult.getSuccessful());
        }
    }
}
