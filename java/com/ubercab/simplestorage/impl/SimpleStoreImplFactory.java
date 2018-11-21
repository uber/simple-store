package com.ubercab.simplestorage.impl;

import android.content.Context;

import com.ubercab.simplestorage.ScopeConfig;
import com.ubercab.simplestorage.SimpleStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.GuardedBy;

public final class SimpleStoreImplFactory {

    private static final Object scopesLock = new Object();

    @GuardedBy("scopesLock")
    private static Map<String, SimpleStoreImpl> scopes = new HashMap<>();

    public static SimpleStore get(Context context) {
        return get(context, "", ScopeConfig.DEFAULT);
    }

    public static SimpleStore get(Context context, String scope, ScopeConfig config) {
        Context appContext = context.getApplicationContext();
        SimpleStoreImpl store;
        synchronized (scopesLock) {
            if (scopes.containsKey(scope)) {
                store = scopes.get(scope);
                if (Objects.requireNonNull(store).isClosed()) {
                    store.open();
                } else {
                    throw new IllegalStateException("scope '"+scope+"' already open");
                }
            } else {
                store = new SimpleStoreImpl(appContext, scope, config);
                scopes.put(scope, store);
            }
        }
        return store;
    }

    static void tombstone(String scope) {
        synchronized (scopesLock) {
            scopes.remove(scope);
        }
    }
}
