# Android Jetpack DataStore vs simple-store: A Comprehensive Comparison

## Executive Summary

Both Android Jetpack DataStore and Uber's simple-store were inspired by the **Store library** (originally developed by Shopify, now maintained by Mobile Native Foundation). While they share similar goals of providing better data persistence solutions than SharedPreferences, they've evolved in significantly different directions. This document analyzes their differences and assesses the feasibility of creating a migration guide between them.

## Shared Heritage: The Store Library

The Store library (MobileNativeFoundation/Store) is a Kotlin Multiplatform library that pioneered several concepts:
- Async-first API design
- Strong consistency guarantees
- Built on Kotlin coroutines and Flow
- Repository pattern for network-resilient applications
- Caching strategies with network fallback

Both simple-store and DataStore borrowed these foundational concepts but adapted them for different use cases.

## Library Overview

### simple-store (Uber)
- **Purpose**: Simple, performant asynchronous file storage for Android
- **API Style**: ListenableFuture-based (Google's Guava)
- **Storage**: File-based key-value storage
- **Data Types**: byte[], String, primitives
- **Platform**: Android-only
- **Dependencies**: Minimal (Android primitives + Guava's ListenableFuture)

### Android Jetpack DataStore
- **Purpose**: Modern data storage solution replacing SharedPreferences
- **API Style**: Kotlin coroutines and Flow
- **Storage**: Two implementations (Preferences and Proto)
- **Data Types**: Primitives, custom types via Protocol Buffers
- **Platform**: Android (with multiplatform support in progress)
- **Dependencies**: Kotlin coroutines, optional Protocol Buffers

## Key Differences

### 1. **API Design Philosophy**

**simple-store:**
```java
// ListenableFuture-based API
ListenableFuture<String> put = simpleStore.putString("key", "value");
ListenableFuture<String> get = simpleStore.getString("key");
```

**DataStore:**
```kotlin
// Coroutines and Flow-based API
// Preferences DataStore
val value = dataStore.data.map { prefs -> prefs[key] }
suspend fun setValue(value: String) {
    dataStore.edit { prefs -> prefs[key] = value }
}

// Proto DataStore
val settings = dataStore.data
dataStore.updateData { currentSettings ->
    currentSettings.toBuilder().setSomeValue(value).build()
}
```

### 2. **Type Safety**

**simple-store:**
- No type safety guarantees
- Operates on raw strings and byte arrays
- Type conversions are caller's responsibility

**DataStore:**
- Preferences DataStore: Type-safe keys with generics
- Proto DataStore: Full type safety with Protocol Buffers
- Compile-time type checking

### 3. **Data Structure Support**

**simple-store:**
- Flat key-value structure
- No built-in support for complex objects
- Serialization handled externally

**DataStore:**
- Preferences: Simple key-value pairs
- Proto: Complex nested data structures
- Built-in serialization support

### 4. **Consistency Guarantees**

**simple-store:**
- In-order execution within namespace
- File-based atomicity
- No transactional updates across keys

**DataStore:**
- ACID guarantees with atomic read-modify-write
- Transactional API for updates
- Read-after-write consistency

### 5. **Error Handling**

**simple-store:**
- Failures delivered to callbacks
- Namespace closure on errors
- No automatic recovery

**DataStore:**
- Flow-based error signaling
- Corruption handling with recovery options
- Built-in migration support

### 6. **Performance Characteristics**

**simple-store:**
- Optimized for startup-critical storage
- Minimal overhead
- Executor-based threading model

**DataStore:**
- Coroutine-based with Dispatchers.IO
- Potential for more overhead
- Better integration with modern Android architecture

### 7. **Migration Support**

**simple-store:**
- No built-in migration mechanism
- Manual migration required

**DataStore:**
- Built-in migration APIs
- SharedPreferences migration support
- Version-aware data migration

## Migration Feasibility Assessment

### From simple-store to DataStore

**Feasibility: MODERATE TO HIGH**

A migration path is feasible with the following considerations:

#### Advantages:
1. Both use file-based storage under the hood
2. Similar async patterns (Future → Coroutines)
3. DataStore's migration API can be leveraged

#### Challenges:
1. **API Paradigm Shift**: ListenableFuture to Coroutines/Flow
2. **Namespace mapping**: simple-store's namespaces to DataStore instances
3. **Type conversion**: String/byte[] to typed preferences or proto

#### Migration Strategy:
```kotlin
// 1. Create a migration adapter
class SimpleStoreToDataStoreMigration(
    private val simpleStore: SimpleStore,
    private val namespace: String
) : DataMigration<Preferences> {
    
    override suspend fun migrate(currentData: Preferences): Preferences {
        val keys = simpleStore.getAllKeys(namespace).get()
        return currentData.toMutablePreferences().apply {
            keys.forEach { key ->
                val value = simpleStore.getString(key).get()
                this[stringPreferencesKey(key)] = value
            }
        }
    }
}

// 2. Initialize DataStore with migration
val dataStore = PreferenceDataStoreFactory.create(
    migrations = listOf(
        SimpleStoreToDataStoreMigration(simpleStore, "namespace")
    )
)
```

### From DataStore to simple-store

**Feasibility: LOW**

This migration direction is not recommended due to:

1. **Feature Loss**: Type safety, transactions, complex data structures
2. **API Downgrade**: Modern coroutines to older ListenableFuture
3. **No Migration Support**: simple-store lacks migration infrastructure
4. **Against Platform Direction**: Moving away from Google's recommended solution

## Fundamental Gaps

### 1. **Architectural Mismatch**
- simple-store: Focused on minimal, startup-critical storage
- DataStore: Comprehensive data persistence solution

### 2. **Type System Incompatibility**
- simple-store's untyped approach vs DataStore's type safety
- No direct mapping for Proto DataStore content

### 3. **Concurrency Models**
- Different threading/async models require significant adapter code
- Flow-based observability vs one-shot Futures

### 4. **Feature Parity**
- DataStore features (corruption handling, multi-process support) have no simple-store equivalent
- simple-store's namespace concept doesn't map cleanly to DataStore

## Compatibility Layer Design

A theoretical compatibility layer could provide:

```kotlin
interface DataStorageCompat {
    // Common API surface
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    fun observeString(key: String): Flow<String?>
    
    // Implementation adapters
    class SimpleStoreAdapter(private val simpleStore: SimpleStore) : DataStorageCompat
    class DataStoreAdapter(private val dataStore: DataStore<Preferences>) : DataStorageCompat
}
```

However, this would:
- Lose type safety benefits of DataStore
- Add complexity without clear benefits
- Create a lowest-common-denominator API

## Recommendations

### When Migration Makes Sense

**simple-store → DataStore**: Recommended when:
- Modernizing Android architecture
- Need for type safety or complex data structures
- Want to align with Android platform direction
- Can afford the migration effort

### When Migration Doesn't Make Sense

**DataStore → simple-store**: Not recommended unless:
- Absolutely need ListenableFuture API
- Have strict size/dependency constraints
- Working with legacy code that can't use coroutines

### Alternative Approaches

1. **Gradual Migration**: Run both systems in parallel, migrate feature by feature
2. **Facade Pattern**: Create an abstraction layer over both during transition
3. **Direct Store Library Usage**: Consider using the original Store library for true multiplatform needs

## Conclusion

While both libraries share DNA from the Store library, they've evolved to serve different needs. A migration from simple-store to DataStore is feasible and often beneficial, following Android's modern development practices. The reverse migration is technically possible but inadvisable.

The fundamental differences in type safety, API design, and feature sets mean that any migration will require careful planning and potentially significant code changes. A compatibility layer is possible but would sacrifice the unique benefits of each library.

For new projects, DataStore is the clear choice. For existing simple-store users, migration to DataStore should be considered as part of a broader modernization effort rather than a drop-in replacement.