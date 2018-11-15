package com.ubercab.simplestorage;

import java.util.concurrent.Executor;


public interface SimpleStore {

    SimpleStore scope(String name);
    SimpleStore scope(String name, ScopeConfig config);

    void getString(String key, Callback<String> cb, Executor executor);
    void putString(String key, String value, Callback<String> cb, Executor executor);

    void get(String key, Callback<byte[]> cb, Executor executor);
    void put(String key, byte[] value, Callback<byte[]> cb, Executor executor);

    void deleteAll(Callback<Void> cb, Executor executor);

    void close();

    interface Callback<T> {
        void onSuccess(T msg);

        void onError(Throwable t);
    }
}
