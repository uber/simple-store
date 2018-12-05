package com.uber.simplestore.impl;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;

/**
 * Ensure runnables happen in order on the backing pool.
 *
 * Stolen from Guava.
 */
final class SerialExecutor implements Executor {
    private final Object lock = new Object();

    @GuardedBy("lock")
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Executor executor;

    @GuardedBy("lock")
    private Runnable active;

    SerialExecutor(Executor executor) {
        this.executor = executor;
    }

    public void execute(final Runnable r) {
        synchronized (lock) {
            tasks.offer(() -> {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }
    }

    private void scheduleNext() {
        synchronized (lock) {
            if ((active = tasks.poll()) != null) {
                executor.execute(active);
            }
        }
    }
}
