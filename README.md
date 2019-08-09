# Simple Store

[![Build Status](https://travis-ci.com/uber/simple-store.svg?token=vUDcZtk6T5yr64PuQJP1&branch=master)](https://travis-ci.com/uber/simple-store)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/3000/badge)](https://bestpractices.coreinfrastructure.org/projects/3000)
[![Maven Central](https://img.shields.io/maven-central/v/com.uber.simplestore/simplestore.svg)](https://search.maven.org/artifact/com.uber.simplestore/simplestore)
[![Maven Central](https://img.shields.io/maven-central/v/com.uber.simplestore/simplestore-proto.svg)](https://search.maven.org/artifact/com.uber.simplestore/simplestore-proto)

This project is stable and being incubated for long-term support.

Simple yet performant asynchronous file storage for Android.

SimpleStore aims to provide developers an extremely robust and performant solution for storing key-value data on disk asynchronously. It is built using only Android and Java primitives and avoids taking on external dependencies making it ideal for critical startup storage. It has no opinion on how data is serialized, only storing `string`-`byte[]` pairs of small to moderate size. The core library only exposes a thread-safe, executor-explicit async API ensuring clear thread selection and no UI jank.

All values are stored on disk as plain files that are “namespaced” in a matching on-disk folder structure. The library also supports configuring a namespace to store data on a cache or transient partition.

## Basic usage

To include in a gradle project, add to your dependencies:

```groovy
dependencies {
    implementation 'com.uber.simplestore:simplestore:0.0.5'
    // If using protocol buffers, also add:
    implementation 'com.uber.simplestore:simplestore-proto:0.0.5'
}
```

Out of the box, SimpleStore uses `ListenableFuture` to store `byte[]`, `String`, primitives and protocol buffers on internal storage. 
```java
SimpleStore simpleStore = SimpleStoreFactory.create(this, "<some-uuid-or-name>");
ListenableFuture<String> put = simpleStore.putString("some_key", "Foo value");
Futures.addCallback(
        put,
        new FutureCallback<String>() {
          @Override
          public void onSuccess(@NonNull String s) {
            
          }

          @Override
          public void onFailure(@NonNull Throwable t) {
            Log.e("MyActivity", "Save failure", t);
          }
        },
        mainExecutor());
simpleStore.close();
```

Note that if you use RxJava, Rx comes with a `fromFuture` method that allows you to wrap `ListenableFuture`:

```java
Single<String> value = Single.fromFuture(simpleStore.getString("some_key"));

```

## Fundamentally Async
IO operations are fundamentally async, and any storage solution should be async all the way through.

The implementation is written using async work queues. This allows us to implement under-the-hood optimizations that do not block consumers such as prefetching and pruning old cached values.

`Futures.get` from Guava is available for consumers who wish to run synchronously.

## Interface
Only one interface is exposed for general use. Implementations of the interface provide a factory method for instantiating any variations.

Usage:
```java
SimpleStore store = SimpleStoreFactory.create(context, “feature/mystuff”, NamespaceConfig.DEFAULT);
ListenableFuture<String> value = store.putString("some_key", value);
```

The interface is designed to allow composition with higher level wrappers such as a protocol buffers, Rx, or ListenableFuture transforms. 

ListenableFuture was chosen over Rx for the implementation as: 
* Future transformations require explicit assignment to an Executor, making it difficult to accidentally perform IO operations in the incorrect pool. 
* Executors do not suffer from the round-robin scheduler design of Rx, making deadlock between IO work impossible.
* AndroidX and most Google libraries already ship ListenableFuture and associated Guava classes with them, so most Android apps can take on ListenableFuture without increasing binary size.
* Interop with Futures is built into Rx via `Observables.fromFuture`.

The base interface and implementation purposely leave out a synchronous API as disk IO is fundamentally async. A safe-ish synchronous API can be obtained via `Futures#getChecked` if absolutely needed for compatibility reasons, but most users who think they need sync will probably find the Futures helpers adequate for their needs.

## Closing a namespace

SimpleStore is closable per namespace, and may only have one open instance per namespace process-wide. When a namespace is closed, the in-memory cache is destroyed. The store will deliver failures to all pending callbacks when closed. This ensures that the consumer is always notified if data does not make it to disk and can handle the failure appropriately such as logging a non-fatal. Any reads or writes attempted on the store after closure will result in an exception.

In the future, we can arbitrarily clear portions of the memory cache of an open namespace when desired such as when the OS informs of a trim level. Since the API is fully async, consumers will not be janked and will just see original load latencies.

## Threading

All operations are guaranteed to be executed in-order within the same namespace. A singular cached thread pool backs all stores process wide, and can be replaced with a custom executor via a static configuration method. It is safe to enqueue any operation from any thread, including the main thread. All future callbacks are paired with an executor to be run on, this forces parsing or other processing actions to get out of the way of ordered disk I/O.

This model makes deadlock across namespaces impossible, as even a blockingGet cannot be issued on the ordered IO executor. Adopting this model leaves us room to experiment later with using explicit thread priority for different namespaces.

## License

    Copyright (C) 2019 Uber Technologies

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

