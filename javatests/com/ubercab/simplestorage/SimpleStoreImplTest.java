package com.ubercab.simplestorage;

import android.content.Context;

import com.google.common.util.concurrent.MoreExecutors;
import com.ubercab.simplestorage.impl.SimpleStoreImplFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class SimpleStoreImplTest {

    private Context context = RuntimeEnvironment.systemContext;

    @Test
    public void saveBytes() {
        SimpleStore store = SimpleStoreImplFactory.get(context);
        BlockingResult<byte[]> missing = new BlockingResult<>();
        store.get("missing", missing, MoreExecutors.directExecutor());
        Assert.assertTrue(missing.isSuccess());
    }
}
