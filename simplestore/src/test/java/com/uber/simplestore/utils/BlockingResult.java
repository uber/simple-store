package com.uber.simplestore.utils;

import com.uber.simplestore.SimpleStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class BlockingResult<T> implements SimpleStore.Callback<T> {
    private final int timeoutSeconds;
    private T value;
    private Throwable throwable;
    private CountDownLatch latch = new CountDownLatch(1);

    public BlockingResult() {
        this(1);
    }

    BlockingResult(int timeoutSeconds) {
       this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void onSuccess(T value) {
        if (latch.getCount() <= 0) {
            throw new IllegalStateException("only use blockingresult once");
        }
        this.value = value;
        latch.countDown();
    }

    @Override
    public void onError(Throwable t) {
        if (latch.getCount() <= 0) {
            throw new IllegalStateException("only use blockingresult once");
        }
        this.throwable = t;
        latch.countDown();
    }

    public Throwable getFailure() {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (throwable == null) {
            throw new IllegalStateException("succeeded");
        }
        return throwable;
    }

    public boolean isSuccess() {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return throwable == null;
    }

    public T getSuccessful() {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (throwable != null) {
            throw new RuntimeException(throwable);
        }
        return value;
    }
}
