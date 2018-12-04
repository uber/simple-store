package com.uber.simplestore.impl;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

final class SerialExecutor implements Executor {
    private final Object lock = new Object();

    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Executor executor;
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
