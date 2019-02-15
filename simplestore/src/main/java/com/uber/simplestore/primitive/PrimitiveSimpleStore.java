package com.uber.simplestore.primitive;

import com.google.common.util.concurrent.ListenableFuture;
import com.uber.simplestore.SimpleStore;
import javax.annotation.CheckReturnValue;

/**
 * Store primitives on disk.
 *
 * <p>All methods never return null in the future, #{@link SimpleStore#contains(String)} should be
 * used for optionality.
 */
public interface PrimitiveSimpleStore extends SimpleStore {

  @CheckReturnValue
  ListenableFuture<Integer> getInt(String key);

  @CheckReturnValue
  ListenableFuture<Integer> put(String key, int value);

  @CheckReturnValue
  ListenableFuture<Long> getLong(String key);

  @CheckReturnValue
  ListenableFuture<Long> put(String key, long value);

  @CheckReturnValue
  ListenableFuture<Boolean> getBoolean(String key);

  @CheckReturnValue
  ListenableFuture<Boolean> put(String key, boolean value);

  @CheckReturnValue
  ListenableFuture<Double> getDouble(String key);

  @CheckReturnValue
  ListenableFuture<Double> put(String key, double value);

  /**
   * Retrieves a #{@link java.nio.charset.StandardCharsets#UTF_16BE} string.
   *
   * @param key to fetch from
   * @return value if present, otherwise ""
   */
  @CheckReturnValue
  @Override
  ListenableFuture<String> getString(String key);

  /**
   * Store string as #{@link java.nio.charset.StandardCharsets#UTF_16BE}.
   *
   * <p>Putting "" will remove the value from disk.
   *
   * @param key name
   * @param value to store
   * @return stored value
   */
  @CheckReturnValue
  ListenableFuture<String> put(String key, String value);

  @CheckReturnValue
  @Override
  ListenableFuture<Void> remove(String key);
}
