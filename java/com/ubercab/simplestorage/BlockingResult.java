package com.ubercab.simplestorage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class BlockingResult<T> implements SimpleStore.Callback<T> {
    private final int timeoutSeconds;
    private T value;
    private Throwable throwable;
    private CountDownLatch latch = new CountDownLatch(1);

    BlockingResult() {
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

    boolean isSuccess() {
        try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
        return throwable == null;
    }

    T getSuccessful() {
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
