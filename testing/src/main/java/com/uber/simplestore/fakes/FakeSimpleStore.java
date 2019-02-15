package com.uber.simplestore.fakes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.StoreClosedException;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.HashMap;

public final class FakeSimpleStore implements SimpleStore {

    private boolean closed = false;
    private final HashMap<String, byte[]> data;
    @Nullable
    private Throwable failureType;

    public FakeSimpleStore() {
        this.data = new HashMap<>();
    }

    @VisibleForTesting
    public void setFailureType(Throwable e) {
        this.failureType = e;
    }

    @Override
    public ListenableFuture<String> getString(String key) {
        String value;
        if (data.containsKey(key)) {
            value = new String(data.get(key), Charset.defaultCharset());
        } else {
            value = null;
        }
        return returnOrFail(value);
    }


    @Override
    public ListenableFuture<String> putString(String key, @Nullable String value) {
        if (value != null) {
            data.put(key, value.getBytes());
        } else {
            data.remove(key);
        }
        return returnOrFail(value);
    }

    @Override
    public ListenableFuture<byte[]> get(String key) {
        return returnOrFail(data.get(key));
    }

    @Override
    public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
        if (value != null) {
            data.put(key, value);
        } else {
            data.remove(key);
        }
        return returnOrFail(value);
    }

    @Override
    public ListenableFuture<Boolean> contains(String key) {
        return returnOrFail(data.containsKey(key));
    }

    @Override
    public ListenableFuture<Void> deleteAll() {
        data.clear();
        return returnOrFail(null);
    }

    @Override
    public void close() {
        closed = true;
    }

    private <T> ListenableFuture<T> returnOrFail(@Nullable T value) {
        if (closed) {
            throw new StoreClosedException();
        }
        if (failureType != null) {
            return Futures.immediateFailedFuture(failureType);
        }
        return Futures.immediateFuture(value);
    }
}
