package com.ubercab.simplestorage.impl;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;

final class TaggedSerialExecutor implements Executor {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Set<String> cancellationList = new HashSet<>();
    private final Executor executor;
    private Runnable active;

    TaggedSerialExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void execute(final Runnable r) {
        execute("", r);
    }

    public synchronized void execute(String tag, final Runnable r) {
        tasks.offer(() -> {
            try {
                if (!isCancelled(tag)) {
                    r.run();
                }
            } finally {
                scheduleNext();
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    public void cancel(String tag) {
        cancellationList.add(tag);
    }

    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            executor.execute(active);
        }
    }

    private boolean isCancelled(String tag) {
        return cancellationList.contains(tag);
    }
}
