# KMP 网络请求指南

## 📋 目录

1. [网络库选择](#网络库选择)
2. [Ktor Client 推荐](#ktor-client-推荐)
3. [配置 Ktor Client](#配置-ktor-client)
4. [使用示例](#使用示例)
5. [架构集成](#架构集成)
6. [最佳实践](#最佳实践)

---

## 网络库选择

### KMP 网络请求库对比

| 库名 | 跨平台支持 | 推荐度 | 说明 |
|------|-----------|--------|------|
| **Ktor Client** | ✅ Android/iOS/JVM/JS/Wasm | ⭐⭐⭐⭐⭐ | JetBrains 官方推荐，功能最全 |
| OkHttp | ❌ 仅 Android/JVM | ⭐⭐⭐ | 不支持 iOS，需要 expect/actual |
| NSURLSession | ❌ 仅 iOS | ⭐⭐ | 需要 expect/actual |
| Apollo GraphQL | ✅ 全平台 | ⭐⭐⭐⭐ | 仅适用于 GraphQL |
| Fuel | ✅ 全平台 | ⭐⭐⭐ | 功能较少，维护不活跃 |

### 推荐：Ktor Client

**为什么选择 Ktor Client？**

1. ✅ **官方支持**：JetBrains 官方维护，与 KMP 完美集成
2. ✅ **全平台支持**：Android、iOS、JVM、JS、Wasm 全部支持
3. ✅ **功能强大**：支持 HTTP/HTTPS、WebSocket、序列化、拦截器等
4. ✅ **协程友好**：完全基于 Kotlin Coroutines，异步编程简单
5. ✅ **易于测试**：支持 Mock 引擎，方便单元测试
6. ✅ **统一代码**：一套代码，多平台运行

---

## 配置 Ktor Client

### 1. 添加依赖

在 `gradle/libs.versions.toml` 中添加版本：

```toml
[versions]
ktor = "3.3.1"  # 已存在
kotlinx-serialization = "1.7.3"  # 用于 JSON 序列化

[libraries]
# Ktor Client 核心
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# 平台特定引擎
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-ios = { module = "io.ktor:ktor-client-ios", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }  # JVM
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
ktor-client-curl = { module = "io.ktor:ktor-client-curl", version.ref = "ktor" }  # iOS 备选

# Kotlinx Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

[plugins]
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

### 2. 在 shared 模块配置依赖

修改 `shared/build.gradle.kts`：

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)  // 添加序列化插件
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // Ktor Client 核心
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.ios)
            // 或者使用 curl 引擎（备选）
            // implementation(libs.ktor.client.curl)
        }
        
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}
```

---

## 使用示例

### 1. 创建 HttpClient

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.kt
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun createHttpClient(): HttpClient

// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.android.kt
import io.ktor.client.*
import io.ktor.client.engine.android.*

actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
}

// shared/src/iosMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.ios.kt
import io.ktor.client.*
import io.ktor.client.engine.ios.*

actual fun createHttpClient(): HttpClient = HttpClient(Ios) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
}

// shared/src/jvmMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.jvm.kt
import io.ktor.client.*
import io.ktor.client.engine.cio.*

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
}

// shared/src/jsMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.js.kt
import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createHttpClient(): HttpClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
}
```

### 2. 定义数据模型（使用 Serialization）

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/data/dto/UserDto.kt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    
    @SerialName("name")
    val name: String,
    
    @SerialName("email")
    val email: String,
    
    @SerialName("created_at")
    val createdAt: Long
)

// shared/src/commonMain/kotlin/com/tcm/heartsave/data/dto/ApiResponse.kt
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    @SerialName("code")
    val code: Int,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("data")
    val data: T?
)
```

### 3. 创建 API 服务

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/data/datasource/remote/UserApi.kt
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface UserApi {
    suspend fun getUserById(id: String): Result<UserDto>
    suspend fun createUser(user: UserDto): Result<UserDto>
    suspend fun updateUser(user: UserDto): Result<UserDto>
    suspend fun deleteUser(id: String): Result<Unit>
}

class UserApiImpl(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.example.com"
) : UserApi {
    
    override suspend fun getUserById(id: String): Result<UserDto> {
        return try {
            val response = client.get("$baseUrl/api/users/$id") {
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json)
                }
            }
            Result.success(response.body<UserDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createUser(user: UserDto): Result<UserDto> {
        return try {
            val response = client.post("$baseUrl/api/users") {
                contentType(ContentType.Application.Json)
                setBody(user)
            }
            Result.success(response.body<UserDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUser(user: UserDto): Result<UserDto> {
        return try {
            val response = client.put("$baseUrl/api/users/${user.id}") {
                contentType(ContentType.Application.Json)
                setBody(user)
            }
            Result.success(response.body<UserDto>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(id: String): Result<Unit> {
        return try {
            client.delete("$baseUrl/api/users/$id")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4. 添加拦截器（认证、日志等）

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.kt
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun createHttpClient(): HttpClient

// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/network/HttpClientFactory.android.kt
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.logging.*

actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    // JSON 序列化
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
    
    // 日志（仅 Debug 模式）
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.ALL
    }
    
    // 默认请求配置
    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }
    
    // 认证拦截器
    install(HttpRequestInterceptor) {
        intercept { request ->
            // 添加认证 Token
            val token = getAuthToken() // 从存储中获取
            token?.let {
                request.headers.append(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }
    
    // 响应拦截器（处理错误）
    HttpResponseValidator {
        validateResponse { response ->
            when (response.status.value) {
                in 400..499 -> throw ClientRequestException(response, "客户端错误")
                in 500..599 -> throw ServerResponseException(response, "服务器错误")
            }
        }
    }
}

// 获取认证 Token 的函数（需要实现）
expect fun getAuthToken(): String?
```

### 5. 完整示例：集成到 Repository

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/data/repository/UserRepositoryImpl.kt
import com.tcm.heartsave.data.datasource.remote.UserApi
import com.tcm.heartsave.data.datasource.local.UserLocalDataSource
import com.tcm.heartsave.data.mapper.UserMapper
import com.tcm.heartsave.domain.model.User
import com.tcm.heartsave.domain.repository.UserRepository

class UserRepositoryImpl(
    private val userApi: UserApi,  // 远程数据源
    private val localDataSource: UserLocalDataSource,  // 本地数据源
    private val mapper: UserMapper = UserMapper()
) : UserRepository {
    
    override suspend fun getUserById(id: String): Result<User> {
        // 1. 先尝试从本地获取
        localDataSource.getUserById(id)?.let { dto ->
            return Result.success(mapper.toDomain(dto))
        }
        
        // 2. 从远程获取
        return userApi.getUserById(id)
            .map { dto ->
                // 3. 保存到本地
                localDataSource.saveUser(dto)
                mapper.toDomain(dto)
            }
    }
    
    override suspend fun createUser(user: User): Result<Unit> {
        return userApi.createUser(mapper.toDto(user))
            .map { dto ->
                localDataSource.saveUser(dto)
            }
            .map { }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        return userApi.updateUser(mapper.toDto(user))
            .map { dto ->
                localDataSource.saveUser(dto)
            }
            .map { }
    }
    
    override suspend fun deleteUser(id: String): Result<Unit> {
        return userApi.deleteUser(id)
            .map {
                localDataSource.deleteUser(id)
            }
    }
}
```

---

## 架构集成

### 按照架构文档集成

根据 `docs/architecture/Architecture.md` 的架构设计，网络请求应该这样组织：

```
shared/src/commonMain/kotlin/com/tcm/heartsave/
├── platform/
│   └── network/
│       └── HttpClientFactory.kt  # HttpClient 创建
├── data/
│   ├── datasource/
│   │   └── remote/
│   │       └── UserApi.kt        # API 接口定义和实现
│   └── repository/
│       └── UserRepositoryImpl.kt  # Repository 实现
└── di/
    └── AppContainer.kt           # 依赖注入容器
```

### 依赖注入配置

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/di/AppContainer.kt
import com.tcm.heartsave.data.datasource.local.UserLocalDataSource
import com.tcm.heartsave.data.datasource.remote.UserApi
import com.tcm.heartsave.data.datasource.remote.UserApiImpl
import com.tcm.heartsave.data.repository.UserRepositoryImpl
import com.tcm.heartsave.domain.repository.UserRepository
import com.tcm.heartsave.domain.usecase.GetUserUseCase
import com.tcm.heartsave.domain.usecase.CreateUserUseCase
import com.tcm.heartsave.platform.network.createHttpClient
import io.ktor.client.HttpClient

class AppContainer {
    // Platform Layer
    private val httpClient: HttpClient = createHttpClient()
    
    // Data Layer - Remote
    private val userApi: UserApi = UserApiImpl(httpClient)
    
    // Data Layer - Local (需要实现)
    private val userLocalDataSource: UserLocalDataSource = UserLocalDataSourceImpl()
    
    // Data Layer - Repository
    private val userRepository: UserRepository = UserRepositoryImpl(
        userApi = userApi,
        localDataSource = userLocalDataSource
    )
    
    // Domain Layer - UseCase
    val getUserUseCase: GetUserUseCase = GetUserUseCase(userRepository)
    val createUserUseCase: CreateUserUseCase = CreateUserUseCase(userRepository)
}
```

---

## 最佳实践

### ✅ 应该做的

1. **使用单例 HttpClient**
   ```kotlin
   // ✅ 好的做法：复用 HttpClient
   val httpClient = createHttpClient()
   val api1 = Api1(httpClient)
   val api2 = Api2(httpClient)
   ```

2. **统一错误处理**
   ```kotlin
   // ✅ 好的做法：使用 Result 类型
   suspend fun getUser(id: String): Result<User>
   ```

3. **配置超时时间**
   ```kotlin
   HttpClient(Android) {
       engine {
           connectTimeout = 10_000
           socketTimeout = 10_000
       }
   }
   ```

4. **使用拦截器处理通用逻辑**
   ```kotlin
   install(HttpRequestInterceptor) {
       intercept { request ->
           // 添加通用 Headers
           // 添加认证 Token
       }
   }
   ```

5. **序列化配置要一致**
   ```kotlin
   json(Json {
       ignoreUnknownKeys = true  // 忽略未知字段
       isLenient = true          // 宽松模式
       encodeDefaults = false     // 不编码默认值
   })
   ```

### ❌ 不应该做的

1. **不要每次都创建新的 HttpClient**
   ```kotlin
   // ❌ 避免：每次都创建新实例
   suspend fun getUser() {
       val client = HttpClient()  // 浪费资源
   }
   ```

2. **不要忽略错误处理**
   ```kotlin
   // ❌ 避免：直接抛出异常
   suspend fun getUser(id: String): User {
       return client.get("...").body()  // 可能崩溃
   }
   
   // ✅ 好的做法：使用 Result
   suspend fun getUser(id: String): Result<User> {
       return try {
           Result.success(client.get("...").body())
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```

3. **不要在 commonMain 中使用平台特定代码**
   ```kotlin
   // ❌ 避免：在 commonMain 中使用平台特定 API
   import android.content.Context  // 不能在 commonMain 中使用
   ```

4. **不要硬编码 URL**
   ```kotlin
   // ❌ 避免：硬编码
   client.get("https://api.example.com/users")
   
   // ✅ 好的做法：使用配置
   private const val BASE_URL = "https://api.example.com"
   client.get("$BASE_URL/users")
   ```

---

## 测试

### Mock HttpClient 进行测试

```kotlin
// shared/src/commonTest/kotlin/com/tcm/heartsave/data/datasource/remote/UserApiTest.kt
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserApiTest {
    @Test
    fun `should return user when request succeeds`() = runTest {
        // Given: Mock HttpClient
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/users/1" -> {
                    respond(
                        content = """{"id":"1","name":"Test","email":"test@test.com","created_at":0}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        
        val api = UserApiImpl(httpClient)
        
        // When
        val result = api.getUserById("1")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Test", result.getOrNull()?.name)
    }
    
    @Test
    fun `should return failure when request fails`() = runTest {
        // Given: Mock HttpClient 返回错误
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound
            )
        }
        
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json())
            }
        }
        
        val api = UserApiImpl(httpClient)
        
        // When
        val result = api.getUserById("1")
        
        // Then
        assertTrue(result.isFailure)
    }
}
```

---

## 总结

### 推荐方案

1. **使用 Ktor Client** 作为网络请求库
2. **使用 Kotlinx Serialization** 进行 JSON 序列化
3. **按照架构文档** 组织代码结构
4. **使用依赖注入** 管理 HttpClient 和 API 实例
5. **统一错误处理** 使用 Result 类型
6. **编写测试** 使用 MockEngine

### 快速开始

1. 添加依赖到 `gradle/libs.versions.toml` 和 `shared/build.gradle.kts`
2. 创建 `HttpClientFactory` 使用 expect/actual
3. 创建 API 接口和实现
4. 集成到 Repository 中
5. 在 AppContainer 中配置依赖注入

---

## 参考资源

- [Ktor Client 官方文档](https://ktor.io/docs/client.html)
- [Kotlinx Serialization 文档](https://kotlinlang.org/docs/serialization.html)
- [KMP 网络请求最佳实践](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-connect-to-apis.html)

---

*最后更新：2024年*
