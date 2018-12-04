package com.uber.simplestore.impl;

import android.content.Context;

import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;
import com.uber.simplestore.SimpleStoreConfig;
import com.uber.simplestore.StoreClosedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//TODO: Java doc please.
final class SimpleStoreImpl implements SimpleStore {
    static final int OPEN = 0;
    static final int CLOSED = 1;
    static final int TOMBSTONED = 2;

    private final Context context;
    private final ScopeConfig config;
    private final String scope;
    @Nullable
    private File scopedDirectory;

    // 0 = open, 1 = closed, 2 = tombstoned
    //TODO: This defaults open. I assume this is intentional?
    AtomicInteger available = new AtomicInteger(OPEN);

    // Only touch from the serial executor.
    private final Map<String, byte[]> cache = new HashMap<>();
    private final SerialExecutor orderedIoExecutor = new SerialExecutor(SimpleStoreConfig.getIOExecutor());

    SimpleStoreImpl(Context appContext, String scope, ScopeConfig config) {
        this.context = appContext;
        this.scope = scope;
        this.config = config;
        orderedIoExecutor.execute(() -> {
            //TODO Consider using context.getDir (We can specify private status for the directory)
            scopedDirectory = new File(context.getFilesDir().getAbsolutePath() + "/simplestore/" + scope);
            //noinspection ResultOfMethodCallIgnored
            scopedDirectory.mkdirs();
        });
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
                    executor.execute(() -> cb.onSuccess(value));
                } catch (IOException e) {
                    executor.execute(() -> cb.onError(e));
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
        if (available.compareAndSet(OPEN, CLOSED)) {
            orderedIoExecutor.execute(() -> SimpleStoreImplFactory.tombstone(SimpleStoreImpl.this));
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

    private boolean failIfClosed(Executor executor, Callback<?> callback) {
        if (available.get() > OPEN) {
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

    @Nullable
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

    static final class ByteToString implements Callback<byte[]> {

        private final Callback<String> wrapped;

        ByteToString(Callback<String> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void onSuccess(byte[] msg) {
            if (msg == null) {
                wrapped.onSuccess(null);
            } else {
                //TODO: Curious, what encoding is the byte, and is it guaranteed?
                wrapped.onSuccess(new String(msg));
            }
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }
    }
}
