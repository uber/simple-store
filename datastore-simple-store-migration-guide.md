# Migration Guide: simple-store to Android Jetpack DataStore

## Overview

This guide provides practical steps for migrating from Uber's simple-store to Android Jetpack DataStore. While these libraries share common ancestry in the Store library, they have different APIs and capabilities that require careful migration planning.

## Prerequisites

- Understanding of Kotlin coroutines and Flow
- Existing simple-store implementation
- Android Gradle Plugin 7.0+ (for DataStore)

## Step 1: Add Dependencies

Add DataStore dependencies alongside your existing simple-store dependency:

```gradle
dependencies {
    // Keep simple-store during migration
    implementation 'com.uber.simplestore:simplestore:0.0.9'
    
    // Add DataStore
    implementation "androidx.datastore:datastore-preferences:1.0.0"
    
    // If using Protocol Buffers
    implementation "androidx.datastore:datastore:1.0.0"
    
    // For migration support
    implementation "androidx.datastore:datastore-core:1.0.0"
}
```

## Step 2: Create Migration Adapter

Create an adapter to migrate data from simple-store to DataStore:

```kotlin
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.uber.simplestore.SimpleStore
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimpleStoreDataMigration(
    private val simpleStore: SimpleStore,
    private val keyMappings: Map<String, KeyMapping> = emptyMap()
) : DataMigration<Preferences> {
    
    sealed class KeyMapping {
        data class StringKey(val key: Preferences.Key<String>) : KeyMapping()
        data class IntKey(val key: Preferences.Key<Int>) : KeyMapping()
        data class BooleanKey(val key: Preferences.Key<Boolean>) : KeyMapping()
        data class LongKey(val key: Preferences.Key<Long>) : KeyMapping()
    }
    
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        // Check if migration has already been performed
        return !currentData.contains(MIGRATION_COMPLETE_KEY)
    }
    
    override suspend fun migrate(currentData: Preferences): Preferences {
        return withContext(Dispatchers.IO) {
            currentData.toMutablePreferences().apply {
                // Migrate each mapped key
                keyMappings.forEach { (simpleStoreKey, mapping) ->
                    try {
                        when (mapping) {
                            is KeyMapping.StringKey -> {
                                val value = Futures.getUnchecked(
                                    simpleStore.getString(simpleStoreKey)
                                )
                                value?.let { this[mapping.key] = it }
                            }
                            is KeyMapping.IntKey -> {
                                val value = Futures.getUnchecked(
                                    simpleStore.getString(simpleStoreKey)
                                )?.toIntOrNull()
                                value?.let { this[mapping.key] = it }
                            }
                            is KeyMapping.BooleanKey -> {
                                val value = Futures.getUnchecked(
                                    simpleStore.getString(simpleStoreKey)
                                )?.toBooleanStrictOrNull()
                                value?.let { this[mapping.key] = it }
                            }
                            is KeyMapping.LongKey -> {
                                val value = Futures.getUnchecked(
                                    simpleStore.getString(simpleStoreKey)
                                )?.toLongOrNull()
                                value?.let { this[mapping.key] = it }
                            }
                        }
                    } catch (e: Exception) {
                        // Log migration error for individual key
                        println("Failed to migrate key: $simpleStoreKey - ${e.message}")
                    }
                }
                
                // Mark migration as complete
                this[MIGRATION_COMPLETE_KEY] = true
            }
        }
    }
    
    override suspend fun cleanUp() {
        // Optionally clean up simple-store data after successful migration
        // simpleStore.deleteAll()
    }
    
    companion object {
        private val MIGRATION_COMPLETE_KEY = booleanPreferencesKey("migration_from_simple_store_complete")
    }
}
```

## Step 3: Create DataStore with Migration

Initialize DataStore with the migration adapter:

```kotlin
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Define your DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
    produceMigrations = { context ->
        // Create simple-store instance for migration
        val simpleStore = SimpleStoreFactory.create(context, "app-namespace")
        
        // Define key mappings from simple-store to DataStore
        val keyMappings = mapOf(
            "user_id" to SimpleStoreDataMigration.KeyMapping.StringKey(
                stringPreferencesKey("user_id")
            ),
            "user_name" to SimpleStoreDataMigration.KeyMapping.StringKey(
                stringPreferencesKey("user_name")
            ),
            "is_logged_in" to SimpleStoreDataMigration.KeyMapping.BooleanKey(
                booleanPreferencesKey("is_logged_in")
            ),
            "last_sync_timestamp" to SimpleStoreDataMigration.KeyMapping.LongKey(
                longPreferencesKey("last_sync_timestamp")
            )
        )
        
        listOf(SimpleStoreDataMigration(simpleStore, keyMappings))
    }
)
```

## Step 4: Update Repository/Data Layer

Transform your data layer from simple-store to DataStore:

### Before (simple-store):
```kotlin
class UserRepository(private val simpleStore: SimpleStore) {
    
    fun saveUser(userId: String, userName: String): ListenableFuture<String> {
        return Futures.transform(
            simpleStore.putString("user_id", userId),
            { simpleStore.putString("user_name", userName) },
            executor
        )
    }
    
    fun getUser(): ListenableFuture<User?> {
        val userIdFuture = simpleStore.getString("user_id")
        val userNameFuture = simpleStore.getString("user_name")
        
        return Futures.whenAllSucceed(userIdFuture, userNameFuture).call({
            val userId = Futures.getUnchecked(userIdFuture)
            val userName = Futures.getUnchecked(userNameFuture)
            if (userId != null && userName != null) {
                User(userId, userName)
            } else null
        }, executor)
    }
}
```

### After (DataStore):
```kotlin
class UserRepository(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }
    
    suspend fun saveUser(userId: String, userName: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = userName
        }
    }
    
    fun getUser(): Flow<User?> = dataStore.data.map { preferences ->
        val userId = preferences[USER_ID_KEY]
        val userName = preferences[USER_NAME_KEY]
        if (userId != null && userName != null) {
            User(userId, userName)
        } else null
    }
}
```

## Step 5: Update UI Layer

Convert your UI layer to use coroutines and Flow:

### Before (simple-store):
```kotlin
class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
    
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user
    
    fun loadUser() {
        Futures.addCallback(
            userRepository.getUser(),
            object : FutureCallback<User?> {
                override fun onSuccess(result: User?) {
                    _user.postValue(result)
                }
                
                override fun onFailure(t: Throwable) {
                    // Handle error
                }
            },
            mainExecutor()
        )
    }
}
```

### After (DataStore):
```kotlin
class UserViewModel(private val userRepository: UserRepository) : ViewModel() {
    
    val user: StateFlow<User?> = userRepository.getUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    fun saveUser(userId: String, userName: String) {
        viewModelScope.launch {
            try {
                userRepository.saveUser(userId, userName)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

## Step 6: Handle Complex Data Types

For complex objects, consider migrating to Proto DataStore:

### Define Proto Schema:
```protobuf
syntax = "proto3";

option java_package = "com.example.app";
option java_multiple_files = true;

message UserSettings {
    string user_id = 1;
    string user_name = 2;
    bool is_logged_in = 3;
    int64 last_sync_timestamp = 4;
    repeated string favorite_items = 5;
    map<string, string> preferences = 6;
}
```

### Create Proto Migration:
```kotlin
class SimpleStoreToProtoMigration(
    private val simpleStore: SimpleStore
) : DataMigration<UserSettings> {
    
    override suspend fun migrate(currentData: UserSettings): UserSettings {
        return currentData.toBuilder()
            .setUserId(
                Futures.getUnchecked(simpleStore.getString("user_id")) ?: ""
            )
            .setUserName(
                Futures.getUnchecked(simpleStore.getString("user_name")) ?: ""
            )
            .setIsLoggedIn(
                Futures.getUnchecked(simpleStore.getString("is_logged_in"))
                    ?.toBooleanStrictOrNull() ?: false
            )
            .build()
    }
}
```

## Step 7: Testing Migration

Create tests to ensure data integrity during migration:

```kotlin
@RunWith(AndroidJUnit4::class)
class DataMigrationTest {
    
    @Test
    fun testSimpleStoreToDataStoreMigration() = runTest {
        // Setup simple-store with test data
        val context = ApplicationProvider.getApplicationContext<Context>()
        val simpleStore = SimpleStoreFactory.create(context, "test")
        
        Futures.getUnchecked(simpleStore.putString("user_id", "12345"))
        Futures.getUnchecked(simpleStore.putString("user_name", "TestUser"))
        
        // Create DataStore with migration
        val dataStore = PreferenceDataStoreFactory.create(
            migrations = listOf(
                SimpleStoreDataMigration(
                    simpleStore,
                    mapOf(
                        "user_id" to SimpleStoreDataMigration.KeyMapping.StringKey(
                            stringPreferencesKey("user_id")
                        ),
                        "user_name" to SimpleStoreDataMigration.KeyMapping.StringKey(
                            stringPreferencesKey("user_name")
                        )
                    )
                )
            ),
            scope = TestScope()
        )
        
        // Verify migration
        val preferences = dataStore.data.first()
        assertEquals("12345", preferences[stringPreferencesKey("user_id")])
        assertEquals("TestUser", preferences[stringPreferencesKey("user_name")])
    }
}
```

## Step 8: Gradual Rollout Strategy

For production apps, consider a gradual rollout:

```kotlin
class HybridDataStore(
    private val simpleStore: SimpleStore,
    private val dataStore: DataStore<Preferences>,
    private val useDataStore: Boolean = false // Feature flag
) {
    suspend fun getString(key: String): String? {
        return if (useDataStore) {
            dataStore.data.first()[stringPreferencesKey(key)]
        } else {
            withContext(Dispatchers.IO) {
                Futures.getUnchecked(simpleStore.getString(key))
            }
        }
    }
    
    suspend fun putString(key: String, value: String) {
        if (useDataStore) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        } else {
            withContext(Dispatchers.IO) {
                Futures.getUnchecked(simpleStore.putString(key, value))
            }
        }
    }
}
```

## Common Pitfalls and Solutions

### 1. **Namespace Handling**
simple-store uses namespaces, while DataStore uses separate instances:
```kotlin
// simple-store namespaces
val userStore = SimpleStoreFactory.create(context, "user")
val settingsStore = SimpleStoreFactory.create(context, "settings")

// DataStore equivalent
val Context.userDataStore by preferencesDataStore("user_preferences")
val Context.settingsDataStore by preferencesDataStore("settings_preferences")
```

### 2. **Thread Safety**
DataStore is thread-safe by default, but be careful with migrations:
```kotlin
// Ensure migration runs on IO dispatcher
override suspend fun migrate(currentData: Preferences): Preferences {
    return withContext(Dispatchers.IO) {
        // Migration logic
    }
}
```

### 3. **Error Handling**
DataStore uses exceptions instead of failed Futures:
```kotlin
// Handle DataStore exceptions
try {
    dataStore.edit { /* ... */ }
} catch (e: IOException) {
    // Handle IO errors
} catch (e: Exception) {
    // Handle other errors
}
```

## Cleanup

After successful migration and verification:

1. Remove simple-store dependency
2. Delete migration code
3. Clean up old simple-store files:

```kotlin
fun cleanupSimpleStore(context: Context) {
    val simpleStoreDir = File(context.filesDir, "simplestore")
    if (simpleStoreDir.exists()) {
        simpleStoreDir.deleteRecursively()
    }
}
```

## Conclusion

Migrating from simple-store to DataStore requires careful planning but provides significant benefits including type safety, better error handling, and modern coroutine-based APIs. The migration can be done gradually using the patterns shown above, allowing for safe rollout and rollback if needed.