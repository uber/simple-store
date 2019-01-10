package com.uber.simplestore.impl;

import android.content.Context;

import com.google.common.util.concurrent.*;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.StoreClosedException;
import com.uber.simplestore.executors.StorageExecutors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

/**
 * Asynchronous storage implementation.
 */
final class SimpleStoreImpl implements SimpleStore {
    private static final int OPEN = 0;
    private static final int CLOSED = 1;
    private static final int TOMBSTONED = 2;

    private final Context context;
    private final String scope;
    @Nullable
    private File scopedDirectory;

    AtomicInteger available = new AtomicInteger(OPEN);

    // Only touch from the serial executor.
    private final Map<String, byte[]> cache = new HashMap<>();
    private final Executor orderedIoExecutor = MoreExecutors.newSequentialExecutor(SimpleStoreConfig.getIOExecutor());

    SimpleStoreImpl(Context appContext, String scope, ScopeConfig config) {
        this.context = appContext;
        this.scope = scope;
        orderedIoExecutor.execute(() -> {
            File directory;
            if (config.equals(ScopeConfig.CACHE)) {
                directory = context.getCacheDir();
            } else {
                directory = context.getFilesDir();
            }
            scopedDirectory = new File(directory.getAbsolutePath() + "/simplestore/" + scope);
            //noinspection ResultOfMethodCallIgnored
            scopedDirectory.mkdirs();
        });
    }

    @Override
    public ListenableFuture<String> getString(String key) {
        return Futures.transform(get(key), (bytes) -> {
            if (bytes != null && bytes.length > 0) {
                return new String(bytes);
            } else {
                return null;
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<String> putString(String key, String value) {
        byte[] bytes = value != null ? value.getBytes() : null;
        return Futures.transform(put(key, bytes), (b) -> value, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<byte[]> get(String key) {
        requireOpen();
        return Futures.submitAsync(() -> {
            if (isClosed()) {
                return Futures.immediateFailedFuture(new StoreClosedException());
            }
            byte[] value;
            if (cache.containsKey(key)) {
                value = cache.get(key);
            } else {
                try {
                    value = readFile(key);
                } catch (IOException e) {
                    return Futures.immediateFailedFuture(e);
                }
                if (value == null) {
                    cache.remove(key);
                } else {
                    cache.put(key, value);
                }
            }
            return Futures.immediateFuture(value);
        }, orderedIoExecutor);
    }

    @Override
    public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
        requireOpen();
        return Futures.submitAsync(() -> {
            if (isClosed()) {
                return Futures.immediateFailedFuture(new StoreClosedException());
            }
            if (value == null) {
                cache.remove(key);
                deleteFile(key);
            } else {
                cache.put(key, value);
                try {
                    writeFile(key, value);
                } catch (IOException e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
            return Futures.immediateFuture(value);
        }, orderedIoExecutor);
    }

    @Override
    public ListenableFuture<Void> deleteAll() {
        requireOpen();
        return Futures.submitAsync(() -> {
            if (isClosed()) {
                return Futures.immediateFailedFuture(new StoreClosedException());
            }
            try {
                File[] files = Objects.requireNonNull(scopedDirectory).listFiles(File::isFile);
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
                //noinspection ResultOfMethodCallIgnored
                scopedDirectory.delete();
                cache.clear();
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
            return Futures.immediateFuture(null);
        }, orderedIoExecutor);
    }

    @Override
    public void close() {
        if (available.compareAndSet(OPEN, CLOSED)) {
            orderedIoExecutor.execute(() -> SimpleStoreFactory.tombstone(SimpleStoreImpl.this));
        }
    }

    private void requireOpen() {
        if (available.get() > OPEN) {
            throw new StoreClosedException();
        }
    }

    boolean tombstone() {
        return available.compareAndSet(CLOSED, TOMBSTONED);
    }

    String getScope() {
        return scope;
    }

    boolean openIfClosed() {
        return available.compareAndSet(CLOSED, OPEN);
    }

    private boolean isClosed() {
        return available.get() > OPEN;
    }

    private void deleteFile(String key) {
        File baseFile = new File(scopedDirectory, key);
        AtomicFile file = new AtomicFile(baseFile);
        file.delete();
    }

    private byte[] readFile(String key) throws IOException {
        File baseFile = new File(scopedDirectory, key);
        AtomicFile file = new AtomicFile(baseFile);
        if (baseFile.exists()) {
            return file.readFully();
        } else {
            return null;
        }
    }

    private void writeFile(String key, byte[] value) throws IOException {
        File baseFile = new File(scopedDirectory, key);
        AtomicFile file = new AtomicFile(baseFile);
        FileOutputStream writer = file.startWrite();
        writer.write(value);
        file.finishWrite(writer);
    }
}
