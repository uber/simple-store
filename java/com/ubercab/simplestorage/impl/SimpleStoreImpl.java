package com.ubercab.simplestorage.impl;

import android.content.Context;

import com.ubercab.simplestorage.ScopeConfig;
import com.ubercab.simplestorage.SimpleStore;
import com.ubercab.simplestorage.SimpleStoreConfig;
import com.ubercab.simplestorage.StoreClosedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

final class SimpleStoreImpl implements SimpleStore {

    private final Context context;
    private final ScopeConfig config;
    private final String scope;
    @Nullable
    private File scopedDirectory;

    private final Object closedLock = new Object();
    @GuardedBy("closedLock")
    private boolean closed = false;

    // Only touch from the serial executor.
    private final Map<String, byte[]> cache = new HashMap<>();
    private final SerialExecutor orderedIoExecutor = new SerialExecutor(SimpleStoreConfig.getIOExecutor());

    SimpleStoreImpl(Context appContext, String scope, ScopeConfig config) {
        this.context = appContext;
        this.scope = scope;
        this.config = config;
        orderedIoExecutor.execute(() -> {
            scopedDirectory = new File(context.getFilesDir().getAbsolutePath() + "/simplestore/" + scope);
            //noinspection ResultOfMethodCallIgnored
            scopedDirectory.mkdirs();
        });
    }

    @Override
    public SimpleStore scope(String name) {
        return scope(name, config);
    }

    @Override
    public SimpleStore scope(String name, ScopeConfig config) {
        return new SimpleStoreImpl(context, scope + "/" + name, config);
    }

    @Override
    public void getString(String key, @Nonnull Callback<String> cb, @Nonnull Executor executor) {
        get(key, new ByteToString(cb), executor);
    }

    @Override
    public void putString(String key, String value, @Nonnull Callback<String> cb, @Nonnull Executor executor) {
        put(key, value.getBytes(), new ByteToString(cb), executor);
    }

    @Override
    public void get(String key, @Nonnull Callback<byte[]> cb, @Nonnull Executor executor) {
        requireOpen();
        orderedIoExecutor.execute(() -> {
            if (failIfClosed(executor, cb)) {
                return;
            }
            byte[] value;
            if (cache.containsKey(key)) {
                value = cache.get(key);
            } else {
                try {
                    value = readFile(key);
                } catch (IOException e) {
                    executor.execute(() -> cb.onError(e));
                    return;
                }
                if (value == null) {
                    cache.remove(key);
                } else {
                    cache.put(key, value);
                }
            }
            executor.execute(() -> cb.onSuccess(value));
        });
    }


    @Override
    public void put(String key, @Nullable byte[] value, @Nonnull Callback<byte[]> cb, @Nonnull Executor executor) {
        requireOpen();
        orderedIoExecutor.execute(() -> {
            if (failIfClosed(executor, cb)) {
                return;
            }
            if (value == null) {
                cache.remove(key);
                deleteFile(key);
            } else {
                cache.put(key, value);
                try {
                    writeFile(key, value);
                    if (!isClosed()) {
                        executor.execute(() -> cb.onSuccess(value));
                    }
                } catch (IOException e) {
                    if (!isClosed()) {
                        executor.execute(() -> cb.onError(e));
                    }
                }
            }
        });
    }

    @Override
    public void deleteAll(@Nonnull Callback<Void> cb, @Nonnull Executor executor) {
        requireOpen();
        orderedIoExecutor.execute(() -> {
            if (failIfClosed(executor, cb)) {
                return;
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
                executor.execute(() -> cb.onError(e));
                return;
            }
            executor.execute(() -> cb.onSuccess(null));
        });
    }

    @Override
    public void close() {
        synchronized (closedLock) {
            closed = true;
        }
        orderedIoExecutor.execute(() -> {
            synchronized (closedLock) {
                if (closed) {
                    SimpleStoreImplFactory.tombstone(scope);
                }
            }
        });
    }

    boolean isClosed() {
        synchronized (closedLock) {
            return closed;
        }
    }

    private void requireOpen() {
        if (isClosed()) {
            throw new StoreClosedException();
        }
    }

    void open() {
        synchronized (closedLock) {
            closed = false;
        }
    }

    private boolean failIfClosed(Executor executor, Callback<?> callback) {
        if (isClosed()) {
            executor.execute(() -> callback.onError(new StoreClosedException()));
            return true;
        }
        return false;
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

    class ByteToString implements Callback<byte[]> {

        private final Callback<String> wrapped;

        ByteToString(Callback<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void onSuccess(byte[] msg) {
            if (msg == null) {
                wrapped.onSuccess(null);
            } else {
                wrapped.onSuccess(new String(msg));
            }
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }
    }
}
