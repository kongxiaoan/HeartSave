# KMP 公用组件（SDK）组织指南

## 📋 目录

1. [组件分类](#组件分类)
2. [存放位置](#存放位置)
3. [目录结构](#目录结构)
4. [实现方式](#实现方式)
5. [具体示例](#具体示例)
6. [最佳实践](#最佳实践)

---

## 组件分类

### SDK 组件类型

KMP 项目中的公用组件（SDK）主要分为以下几类：

| 组件类型 | 示例 | 是否需要平台特定实现 |
|---------|------|-------------------|
| **网络请求** | Ktor Client、HTTP Client | ✅ 是（引擎不同） |
| **数据库** | SQLDelight、Room、Realm | ✅ 是（平台实现不同） |
| **日志工具** | Logger、Logging | ✅ 是（平台API不同） |
| **存储工具** | SharedPreferences、UserDefaults | ✅ 是（平台API不同） |
| **文件系统** | File Manager | ✅ 是（平台API不同） |
| **加密工具** | Crypto | ✅ 是（平台API不同） |
| **权限管理** | Permissions | ✅ 是（平台API不同） |
| **图片处理** | Image Loader | ✅ 是（平台API不同） |

---

## 存放位置

### 🎯 核心原则

**所有公用组件（SDK）都应该放在 `shared` 模块中，属于 Platform Layer（平台层）**

### 📁 目录结构

```
shared/
├── src/
│   ├── commonMain/kotlin/com/tcm/heartsave/
│   │   └── platform/              # Platform Layer 接口定义
│   │       ├── network/          # 网络请求接口（expect）
│   │       ├── database/         # 数据库接口（expect）
│   │       ├── storage/          # 存储接口（expect）
│   │       ├── logging/          # 日志接口（expect）
│   │       ├── file/             # 文件系统接口（expect）
│   │       ├── crypto/           # 加密接口（expect）
│   │       └── permissions/      # 权限接口（expect）
│   │
│   ├── androidMain/kotlin/com/tcm/heartsave/
│   │   └── platform/            # Android 平台实现（actual）
│   │       ├── network/
│   │       ├── database/
│   │       ├── storage/
│   │       ├── logging/
│   │       ├── file/
│   │       ├── crypto/
│   │       └── permissions/
│   │
│   ├── iosMain/kotlin/com/tcm/heartsave/
│   │   └── platform/             # iOS 平台实现（actual）
│   │       ├── network/
│   │       ├── database/
│   │       ├── storage/
│   │       ├── logging/
│   │       ├── file/
│   │       ├── crypto/
│   │       └── permissions/
│   │
│   ├── jvmMain/kotlin/com/tcm/heartsave/
│   │   └── platform/             # JVM 平台实现（actual）
│   │       └── ...
│   │
│   ├── jsMain/kotlin/com/tcm/heartsave/
│   │   └── platform/             # JS 平台实现（actual）
│   │       └── ...
│   │
│   └── wasmJsMain/kotlin/com/tcm/heartsave/
│       └── platform/             # WasmJs 平台实现（actual）
│           └── ...
```

---

## 目录结构

### 推荐的完整目录结构

```
shared/src/commonMain/kotlin/com/tcm/heartsave/
├── platform/                      # Platform Layer - 平台抽象层
│   ├── network/                  # 网络请求 SDK
│   │   ├── HttpClientFactory.kt  # HttpClient 创建工厂（expect）
│   │   └── NetworkConfig.kt      # 网络配置
│   │
│   ├── database/                 # 数据库 SDK
│   │   ├── Database.kt          # 数据库接口（expect）
│   │   ├── DatabaseDriver.kt   # 数据库驱动（expect）
│   │   └── DatabaseConfig.kt    # 数据库配置
│   │
│   ├── storage/                 # 存储 SDK
│   │   ├── KeyValueStorage.kt   # 键值存储（expect）
│   │   ├── SecureStorage.kt     # 安全存储（expect）
│   │   └── StorageConfig.kt    # 存储配置
│   │
│   ├── logging/                 # 日志 SDK
│   │   ├── Logger.kt            # 日志接口（expect）
│   │   ├── LogLevel.kt          # 日志级别
│   │   └── LogConfig.kt         # 日志配置
│   │
│   ├── file/                    # 文件系统 SDK
│   │   ├── FileManager.kt      # 文件管理（expect）
│   │   └── FilePath.kt         # 文件路径工具
│   │
│   ├── crypto/                  # 加密 SDK
│   │   ├── Crypto.kt           # 加密接口（expect）
│   │   └── Hash.kt             # 哈希工具
│   │
│   └── permissions/             # 权限 SDK
│       ├── Permissions.kt      # 权限管理（expect）
│       └── PermissionType.kt   # 权限类型
│
├── data/                        # Data Layer - 数据层
│   ├── datasource/             # 数据源
│   ├── repository/             # Repository 实现
│   └── mapper/                 # 数据映射
│
├── domain/                      # Domain Layer - 领域层
│   ├── model/                  # 领域模型
│   ├── repository/             # Repository 接口
│   └── usecase/                # UseCase
│
└── di/                          # 依赖注入
    └── AppContainer.kt         # 依赖容器
```

---

## 实现方式

### expect/actual 机制

所有需要平台特定实现的 SDK 都应该使用 `expect/actual` 机制：

**步骤 1：在 commonMain 中定义 expect 接口**

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/logging/Logger.kt
expect class Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
```

**步骤 2：在各平台实现 actual**

```kotlin
// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/logging/Logger.android.kt
import android.util.Log

actual class Logger actual constructor() {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    
    actual fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    
    actual fun w(tag: String, message: String) {
        Log.w(tag, message)
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
```

```kotlin
// shared/src/iosMain/kotlin/com/tcm/heartsave/platform/logging/Logger.ios.kt
import platform.Foundation.NSLog

actual class Logger actual constructor() {
    actual fun d(tag: String, message: String) {
        NSLog("[$tag] DEBUG: $message")
    }
    
    actual fun i(tag: String, message: String) {
        NSLog("[$tag] INFO: $message")
    }
    
    actual fun w(tag: String, message: String) {
        NSLog("[$tag] WARN: $message")
    }
    
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}
```

---

## 具体示例

### 1. 网络请求 SDK（Ktor Client）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/`

```kotlin
// commonMain - 接口定义
expect fun createHttpClient(): HttpClient

// androidMain - Android 实现
actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// iosMain - iOS 实现
actual fun createHttpClient(): HttpClient = HttpClient(Ios) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
```

### 2. 数据库 SDK（SQLDelight）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/database/`

```kotlin
// commonMain - 接口定义
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain - Android 实现
actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = getApplicationContext(),
            name = "app.db"
        )
    }
}

// iosMain - iOS 实现
actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "app.db"
        )
    }
}
```

### 3. 存储 SDK（KeyValue Storage）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/storage/`

```kotlin
// commonMain - 接口定义
expect class KeyValueStorage {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun clear()
}

// androidMain - Android 实现（SharedPreferences）
actual class KeyValueStorage actual constructor() {
    private val prefs = getApplicationContext()
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    actual suspend fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    actual suspend fun get(key: String): String? {
        return prefs.getString(key, null)
    }
    
    actual suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
    
    actual suspend fun clear() {
        prefs.edit().clear().apply()
    }
}

// iosMain - iOS 实现（UserDefaults）
actual class KeyValueStorage actual constructor() {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual suspend fun put(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }
    
    actual suspend fun get(key: String): String? {
        return userDefaults.stringForKey(key)
    }
    
    actual suspend fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }
    
    actual suspend fun clear() {
        userDefaults.removePersistentDomainForName(NSBundle.mainBundle.bundleIdentifier!!)
    }
}
```

### 4. 日志 SDK

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/logging/`

```kotlin
// commonMain - 接口定义
expect class Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

// androidMain - Android 实现
actual class Logger actual constructor() {
    actual fun d(tag: String, message: String) = Log.d(tag, message)
    actual fun i(tag: String, message: String) = Log.i(tag, message)
    actual fun w(tag: String, message: String) = Log.w(tag, message)
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}

// iosMain - iOS 实现
actual class Logger actual constructor() {
    actual fun d(tag: String, message: String) {
        NSLog("[$tag] DEBUG: $message")
    }
    actual fun i(tag: String, message: String) {
        NSLog("[$tag] INFO: $message")
    }
    actual fun w(tag: String, message: String) {
        NSLog("[$tag] WARN: $message")
    }
    actual fun e(tag: String, message: String, throwable: Throwable?) {
        NSLog("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}
```

### 5. 文件系统 SDK

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/file/`

```kotlin
// commonMain - 接口定义
expect class FileManager {
    suspend fun readFile(path: String): ByteArray?
    suspend fun writeFile(path: String, data: ByteArray): Boolean
    suspend fun deleteFile(path: String): Boolean
    suspend fun exists(path: String): Boolean
}

// androidMain - Android 实现
actual class FileManager actual constructor() {
    private val context = getApplicationContext()
    
    actual suspend fun readFile(path: String): ByteArray? {
        return try {
            context.openFileInput(path).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun writeFile(path: String, data: ByteArray): Boolean {
        return try {
            context.openFileOutput(path, Context.MODE_PRIVATE).use {
                it.write(data)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun deleteFile(path: String): Boolean {
        return context.deleteFile(path)
    }
    
    actual suspend fun exists(path: String): Boolean {
        return context.getFileStreamPath(path).exists()
    }
}

// iosMain - iOS 实现
actual class FileManager actual constructor() {
    private val fileManager = NSFileManager.defaultManager
    
    actual suspend fun readFile(path: String): ByteArray? {
        return NSData.dataWithContentsOfFile(path)?.toByteArray()
    }
    
    actual suspend fun writeFile(path: String, data: ByteArray): Boolean {
        return NSData.create(data).writeToFile(path, atomically = true)
    }
    
    actual suspend fun deleteFile(path: String): Boolean {
        return fileManager.removeItemAtPath(path, null)
    }
    
    actual suspend fun exists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }
}
```

---

## 最佳实践

### ✅ 应该做的

1. **统一放在 shared 模块的 platform 包下**
   ```
   shared/src/commonMain/kotlin/com/tcm/heartsave/platform/
   ```

2. **使用 expect/actual 机制**
   - `commonMain` 定义接口（expect）
   - 各平台实现（actual）

3. **保持接口一致性**
   - 所有平台的接口签名应该一致
   - 行为应该尽可能一致

4. **提供工厂方法或单例**
   ```kotlin
   // ✅ 好的做法：提供工厂方法
   expect fun createHttpClient(): HttpClient
   
   // ✅ 或者：提供单例
   expect object Logger {
       fun d(tag: String, message: String)
   }
   ```

5. **在 AppContainer 中统一管理**
   ```kotlin
   class AppContainer {
       // Platform Layer
       private val httpClient = createHttpClient()
       private val logger = Logger()
       private val storage = KeyValueStorage()
       
       // Data Layer
       private val userApi = UserApiImpl(httpClient)
       // ...
   }
   ```

### ❌ 不应该做的

1. **不要放在 composeApp 模块**
   ```kotlin
   // ❌ 避免：放在 composeApp
   composeApp/src/commonMain/kotlin/com/tcm/heartsave/database/
   ```

2. **不要在 commonMain 中使用平台特定 API**
   ```kotlin
   // ❌ 避免：在 commonMain 中使用平台 API
   import android.util.Log  // 不能在 commonMain 中使用
   ```

3. **不要直接依赖第三方 SDK**
   ```kotlin
   // ❌ 避免：直接依赖平台特定 SDK
   class Database {
       private val room = Room.databaseBuilder(...)  // 不能在 commonMain 中使用
   }
   
   // ✅ 好的做法：使用 expect/actual
   expect class Database {
       fun query(sql: String)
   }
   ```

4. **不要忽略错误处理**
   ```kotlin
   // ❌ 避免：忽略错误
   actual suspend fun readFile(path: String): ByteArray {
       return file.readBytes()  // 可能崩溃
   }
   
   // ✅ 好的做法：返回可空类型或 Result
   actual suspend fun readFile(path: String): ByteArray? {
       return try {
           file.readBytes()
       } catch (e: Exception) {
           null
       }
   }
   ```

---

## 常用 SDK 推荐

### 网络请求
- **Ktor Client** ⭐⭐⭐⭐⭐（推荐）
- OkHttp（仅 Android/JVM）

### 数据库
- **SQLDelight** ⭐⭐⭐⭐⭐（推荐，跨平台）
- Room（仅 Android）
- Realm（跨平台，但较复杂）

### 日志
- **自定义 Logger** ⭐⭐⭐⭐⭐（推荐，使用 expect/actual）
- Timber（仅 Android）
- Napier（KMP 日志库）

### 存储
- **自定义 KeyValueStorage** ⭐⭐⭐⭐⭐（推荐）
- DataStore（仅 Android）
- MMKV（仅 Android）

### 序列化
- **Kotlinx Serialization** ⭐⭐⭐⭐⭐（推荐）
- Gson（仅 JVM）
- Moshi（仅 JVM）

---

## 总结

### 核心原则

1. **所有 SDK 放在 `shared` 模块**
2. **使用 `platform/` 包组织**
3. **使用 expect/actual 机制**
4. **接口定义在 commonMain，实现在 platformMain**
5. **在 AppContainer 中统一管理**

### 目录结构模板

```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/
├── {sdk-name}/           # SDK 名称
│   ├── {SdkName}.kt     # expect 接口定义
│   └── {SdkName}Config.kt  # 配置类（可选）

shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/
└── {sdk-name}/
    └── {SdkName}.{platform}.kt  # actual 实现
```

---

## 参考资源

- [Kotlin Multiplatform expect/actual 文档](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
- [Ktor Client 文档](https://ktor.io/docs/client.html)
- [SQLDelight 文档](https://cashapp.github.io/sqldelight/)
- [Kotlinx Serialization 文档](https://kotlinlang.org/docs/serialization.html)

---

*最后更新：2024年*
