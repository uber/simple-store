package com.uber.simplestore.proto.impl;

import android.content.Context;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.impl.SimpleStoreFactory;
import com.uber.simplestore.proto.SimpleProtoStore;

/**
 * Obtain an instance of a storage scope with proto support. Only one instance per scope may exist
 * at any time.
 */
public final class SimpleProtoStoreFactory {

  public static SimpleProtoStore create(Context context, String scope) {
    return create(context, scope, ScopeConfig.DEFAULT);
  }

  public static SimpleProtoStore create(Context context, String scope, ScopeConfig config) {
    return new SimpleProtoStoreImpl(SimpleStoreFactory.create(context, scope, config), config);
  }
}
