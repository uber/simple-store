package com.uber.simplestore.primitive;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import javax.annotation.Nullable;

@SuppressWarnings("UnstableApiUsage")
final class PrimitiveSimpleStoreImpl implements PrimitiveSimpleStore {

  private final SimpleStore simpleStore;

  PrimitiveSimpleStoreImpl(SimpleStore simpleStore) {
    this.simpleStore = simpleStore;
  }

  @Override
  public ListenableFuture<String> getString(String key) {
    return Futures.transform(
        simpleStore.getString(key), value -> value != null ? value : "", directExecutor());
  }

  @Override
  public ListenableFuture<String> put(String key, String value) {
    return simpleStore.putString(key, value);
  }

  @Override
  public ListenableFuture<String> putString(String key, @Nullable String value) {
    return simpleStore.putString(key, value);
  }

  @Override
  public ListenableFuture<byte[]> get(String key) {
    return simpleStore.get(key);
  }

  @Override
  public ListenableFuture<byte[]> put(String key, @Nullable byte[] value) {
    return simpleStore.put(key, value);
  }

  @Override
  public ListenableFuture<Boolean> contains(String key) {
    return simpleStore.contains(key);
  }

  @Override
  public ListenableFuture<Void> deleteAll() {
    return simpleStore.deleteAll();
  }

  @Override
  public void close() {
    simpleStore.close();
  }

  @Override
  public ListenableFuture<Integer> getInt(String key) {
    return Futures.transform(
        get(key),
        (b) -> {
          if (b == null || b.length != 4) {
            return 0;
          }
          // decode big endian
          return b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
        },
        directExecutor());
  }

  @Override
  public ListenableFuture<Integer> put(String key, int value) {
    byte[] bytes;
    if (value != 0) {
      // encode big endian
      bytes =
          new byte[] {
            (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value
          };
    } else {
      bytes = null;
    }
    return Futures.transform(put(key, bytes), (v) -> value, directExecutor());
  }

  @Override
  public ListenableFuture<Long> getLong(String key) {
    return Futures.transform(
        get(key),
        (b) -> {
          if (b == null || b.length != 8) {
            return 0L;
          }
          return (b[0] & 0xFFL) << 56
              | (b[1] & 0xFFL) << 48
              | (b[2] & 0xFFL) << 40
              | (b[3] & 0xFFL) << 32
              | (b[4] & 0xFFL) << 24
              | (b[5] & 0xFFL) << 16
              | (b[6] & 0xFFL) << 8
              | (b[7] & 0xFFL);
        },
        directExecutor());
  }

  @Override
  public ListenableFuture<Long> put(String key, long value) {
    byte[] bytes;
    if (value != 0) {
      long v = value;
      bytes = new byte[8];
      // encode big endian
      for (int i = 7; i >= 0; i--) {
        bytes[i] = (byte) (v & 0xffL);
        v >>= 8;
      }
    } else {
      bytes = null;
    }
    return Futures.transform(put(key, bytes), (v) -> value, directExecutor());
  }

  @Override
  public ListenableFuture<Boolean> getBoolean(String key) {
    return Futures.transform(
        get(key), (b) -> b != null && b.length > 0 && b[0] > 0, directExecutor());
  }

  @Override
  public ListenableFuture<Boolean> put(String key, boolean value) {
    byte[] bytes;
    if (value) {
      bytes = new byte[] {1};
    } else {
      bytes = new byte[] {0};
    }
    return Futures.transform(put(key, bytes), (v) -> value, directExecutor());
  }

  @Override
  public ListenableFuture<Double> getDouble(String key) {
    return Futures.transform(getLong(key), Double::longBitsToDouble, directExecutor());
  }

  @Override
  public ListenableFuture<Double> put(String key, double value) {
    return Futures.transform(
        put(key, Double.doubleToRawLongBits(value)), (v) -> value, directExecutor());
  }

  @Override
  public ListenableFuture<Void> remove(String key) {
    return simpleStore.remove(key);
  }
}
