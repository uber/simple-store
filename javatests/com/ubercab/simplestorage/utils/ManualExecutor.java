package com.ubercab.simplestorage.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

public final class ManualExecutor implements Executor {

    private Queue<Runnable> queue = new ConcurrentLinkedDeque<>();

    @Override
    public void execute(Runnable command) {
        queue.add(command);
    }

    public void flush() {
        while(!queue.isEmpty()) {
            queue.poll().run();
        }
    }
}
