package com.uber.simplestore.primitive;

import android.content.Context;
import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.impl.SimpleStoreFactory;

public final class PrimitiveSimpleStoreFactory {
  public static PrimitiveSimpleStore create(Context context, String scope) {
    return create(context, scope, ScopeConfig.DEFAULT);
  }

  public static PrimitiveSimpleStore create(Context context, String scope, ScopeConfig config) {
    return new PrimitiveSimpleStoreImpl(SimpleStoreFactory.create(context, scope, config));
  }
}
