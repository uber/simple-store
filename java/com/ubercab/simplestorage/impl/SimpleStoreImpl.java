package com.ubercab.simplestorage.impl;

import android.content.Context;

import com.ubercab.simplestorage.ScopeConfig;
import com.ubercab.simplestorage.SimpleStore;
import com.ubercab.simplestorage.SimpleStoreConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

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
    private final TaggedSerialExecutor orderedIoExecutor = new TaggedSerialExecutor(SimpleStoreConfig.getIOExecutor());

    SimpleStoreImpl(Context appContext, String scope, ScopeConfig config) {
        this.context = appContext;
        this.scope = scope;
        this.config = config;
        orderedIoExecutor.execute(() -> {
            scopedDirectory = new File(context.getFilesDir().getAbsolutePath() + "/simplestore/" + scope);
            scopedDirectory.mkdirs();});
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
    public void getString(String key, Callback<String> cb, Executor executor) {
        get(key, new ByteToString(cb), executor);
    }

    @Override
    public void putString(String key, String value, Callback<String> cb, Executor executor) {
        put(key, value.getBytes(), new ByteToString(cb), executor);
    }

    @Override
    public void get(String key, Callback<byte[]> cb, Executor executor) {
        requireOpen();
        orderedIoExecutor.execute("read", () -> {
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
    public void put(String key, @Nullable byte[] value, Callback<byte[]> cb, Executor executor) {
        requireOpen();
        orderedIoExecutor.execute("write", () -> {
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
    public void deleteAll(Callback<Void> cb, Executor executor) {
        requireOpen();
        orderedIoExecutor.execute(() -> {
            try {
                File[] files = scopedDirectory.listFiles(File::isFile);
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
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
        orderedIoExecutor.cancel("read");
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
            throw new IllegalStateException("store is closed");
        }
    }

    void open() {
        synchronized (closedLock) {
            closed = false;
        }
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
