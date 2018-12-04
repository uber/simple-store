package com.uber.simplestore.impl;

import android.content.Context;

import com.uber.simplestore.ScopeConfig;
import com.uber.simplestore.SimpleStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.concurrent.GuardedBy;

public final class SimpleStoreImplFactory {

    private static final Object scopesLock = new Object();

    @GuardedBy("scopesLock")
    private static Map<String, SimpleStoreImpl> scopes = new HashMap<>();

    static SimpleStore create(Context context) {
        return create(context, "", ScopeConfig.DEFAULT);
    }

    public static SimpleStore create(Context context, String scope, ScopeConfig config) {
        Context appContext = context.getApplicationContext();
        SimpleStoreImpl store;
        synchronized (scopesLock) {
            if (scopes.containsKey(scope)) {
                store = scopes.get(scope);
                //TODO: Couldn't we side step this exception? We could just return the one we found. That, or if we want to be explicit, we can have a boolean option to fail if already present.
                if (!Objects.requireNonNull(store).openIfClosed()) {
                    throw new IllegalStateException("scope '" + scope + "' already open");
                }
            } else {
                store = new SimpleStoreImpl(appContext, scope, config);
                scopes.put(scope, store);
            }
        }
        return store;
    }

    static void tombstone(SimpleStoreImpl store) {
        synchronized (scopesLock) {
            if (store.tombstone()) {
                scopes.remove(store.getScope());
            }
        }
    }

    static void crashIfAnyOpen() {
        synchronized (scopesLock) {
            for (Map.Entry<String, SimpleStoreImpl> e : scopes.entrySet()) {
                if (e.getValue().available.get() == 0) {
                    throw new IllegalStateException("Leaked scope " + e.getKey());
                }
            }
        }
    }
}
