package com.uber.simplestore;

/** Configure the store for a scope. */
public final class ScopeConfig {
  /** No-op currently. */
  public static final ScopeConfig CRITICAL = new ScopeConfig();

  /** Use the cache directory. */
  public static final ScopeConfig CACHE = new ScopeConfig();

  /** Default settings. */
  public static final ScopeConfig DEFAULT = new ScopeConfig();
}
