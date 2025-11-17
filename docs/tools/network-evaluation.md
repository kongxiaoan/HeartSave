# 网络请求工具封装方案评估

## 📊 总体评价

**评分：⭐⭐⭐⭐ (4/5)**

这是一个**架构清晰、功能全面**的网络封装方案，设计思路很好，但有一些需要改进的地方。

---

## ✅ 优点

### 1. 架构设计清晰
- ✅ 分层明确（config/http/sse/ws/interceptors/parser）
- ✅ 插件化设计，可扩展性强
- ✅ 符合单一职责原则

### 2. 功能全面
- ✅ 支持 HTTP/SSE/WebSocket
- ✅ 支持多种解析器（JSON/PB/XML）
- ✅ Flow 包装，符合现代 Kotlin 开发习惯
- ✅ 拦截器体系设计合理

### 3. 使用方式友好
- ✅ Builder 模式配置灵活
- ✅ API 简洁，类似 Retrofit
- ✅ Flow 支持，响应式编程

---

## ⚠️ 需要改进的地方

### 🔴 严重问题

#### 1. **ApiResult 设计不够完善**

**当前设计：**
```kotlin
sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val throwable: Throwable) : ApiResult<Nothing>()
}
```

**问题：**
- ❌ 缺少 HTTP 状态码
- ❌ 缺少错误消息
- ❌ 缺少错误类型区分（网络错误、业务错误、解析错误等）
- ❌ Loading 状态在 Flow 中可能不够灵活

**改进建议（符合标准 API 格式）：**
```kotlin
sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    
    // 成功：只包含业务数据，简洁明了
    data class Success<T>(val data: T) : ApiResult<T>()
    
    sealed class Error : ApiResult<Nothing>() {
        // 网络错误（连接失败、超时等）
        data class NetworkError(
            val throwable: Throwable,
            val message: String = throwable.message ?: "网络错误"
        ) : Error()
        
        // HTTP 错误（4xx, 5xx）
        data class HttpError(
            val statusCode: Int,
            val message: String,
            val body: String? = null
        ) : Error()
        
        // 业务错误（API 返回 code != 200）
        data class BusinessError(
            val code: Int,        // API 返回的业务状态码
            val message: String,   // API 返回的错误消息
            val data: Any? = null  // 可选的错误数据
        ) : Error()
        
        // 解析错误（JSON 解析失败等）
        data class ParseError(
            val throwable: Throwable,
            val message: String = "数据解析失败"
        ) : Error()
        
        // 未知错误
        data class UnknownError(
            val throwable: Throwable,
            val message: String = "未知错误"
        ) : Error()
    }
}
```

**说明：**
- ✅ `Success` 只包含业务数据 `data`，简洁明了
- ✅ 业务状态码和消息通过 `BusinessError` 处理（对应 API 的 `code` 和 `msg`）
- ✅ HTTP 状态码用于区分网络层错误（`HttpError`）
- ✅ 不需要 `headers`，因为：
  - Headers 中的信息（如 Token 刷新）可以通过拦截器处理
  - 分页信息通常包含在 `data` 中
  - 避免污染业务层的 ApiResult

#### 2. **拦截器实现方式有问题**

**当前设计：**
```kotlin
fun interface Interceptor {
    suspend fun <T> intercept(block: suspend () -> T): suspend () -> T
}

private suspend inline fun <T> execute(block: () -> T): T {
    var result = block
    interceptors.forEach { result = it.intercept(result) }
    return result()
}
```

**问题：**
- ❌ 这种方式无法访问 HttpRequest/HttpResponse
- ❌ 无法修改请求头、请求体
- ❌ 无法处理响应拦截
- ❌ 与 Ktor 的拦截器机制不兼容

**改进建议：使用 Ktor 的 HttpRequestInterceptor 和 HttpResponseValidator**

```kotlin
// 请求拦截器
class AuthInterceptor(private val tokenProvider: () -> String?) : HttpRequestInterceptor {
    override suspend fun intercept(request: HttpRequestBuilder) {
        tokenProvider()?.let { token ->
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

// 响应拦截器
class ErrorInterceptor : HttpResponseValidator {
    override suspend fun validateResponse(response: HttpResponse) {
        when (response.status.value) {
            in 400..499 -> throw HttpException(response, "客户端错误")
            in 500..599 -> throw HttpException(response, "服务器错误")
        }
    }
}

// 在 NetworkClient 中使用
HttpClient {
    install(HttpRequestInterceptor) {
        interceptors.forEach { it.intercept(this) }
    }
    HttpResponseValidator {
        validateResponse { response ->
            // 错误处理
        }
    }
}
```

#### 3. **缺少跨平台实现（expect/actual）**

**问题：**
- ❌ 没有考虑不同平台的 HttpClient 引擎
- ❌ 没有平台特定的配置

**改进建议：**
```kotlin
// commonMain
expect fun createHttpClientEngine(): HttpClientEngineFactory<*>

// androidMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Android

// iosMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Ios

// jvmMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = CIO
```

---

### 🟡 中等问题

#### 4. **错误处理不够完善**

**问题：**
- ❌ 没有统一的错误处理机制
- ❌ 没有错误重试机制
- ❌ 没有超时处理

**改进建议：**
```kotlin
class NetworkClient {
    var retryPolicy: RetryPolicy? = null
    var timeoutConfig: TimeoutConfig? = null
    
    suspend inline fun <reified T: Any> get(path: String): T {
        return retryPolicy?.retry {
            execute { httpClient.get(baseUrl + path).body<T>() }
        } ?: execute { httpClient.get(baseUrl + path).body<T>() }
    }
}

data class RetryPolicy(
    val maxRetries: Int = 3,
    val retryDelay: Long = 1000,
    val retryCondition: (Throwable) -> Boolean = { it is IOException }
)
```

#### 5. **SSE 和 WebSocket 实现不完整**

**当前设计：**
```kotlin
fun NetworkClient.sse(path: String): Flow<String> = flow {
    httpClient.sse(baseUrl + path) {
        onEvent { event, data -> emit(data) }
    }
}
```

**问题：**
- ❌ Ktor 的 SSE 插件使用方式不正确
- ❌ 缺少错误处理
- ❌ 缺少连接状态管理

**改进建议：**
```kotlin
fun NetworkClient.sse(path: String): Flow<SSEEvent> = callbackFlow {
    val client = httpClient.get(baseUrl + path) {
        headers.append(HttpHeaders.Accept, "text/event-stream")
    }
    
    client.bodyAsChannel().consumeAsFlow()
        .map { it.decodeUTF8Line() }
        .collect { line ->
            val event = parseSSEEvent(line)
            trySend(event)
        }
}.catch { e ->
    emit(SSEEvent.Error(e))
}
```

#### 6. **缺少缓存机制**

**改进建议：**
```kotlin
interface CacheStrategy {
    suspend fun <T> get(key: String): T?
    suspend fun <T> put(key: String, value: T, ttl: Long = 0)
    suspend fun clear()
}

class NetworkClient {
    var cacheStrategy: CacheStrategy? = null
    
    suspend inline fun <reified T: Any> get(path: String, useCache: Boolean = true): T {
        val cacheKey = "$baseUrl$path"
        
        if (useCache) {
            cacheStrategy?.get<T>(cacheKey)?.let { return it }
        }
        
        val result = execute { httpClient.get(baseUrl + path).body<T>() }
        
        if (useCache) {
            cacheStrategy?.put(cacheKey, result)
        }
        
        return result
    }
}
```

#### 7. **Parser 接口设计不够灵活**

**当前设计：**
```kotlin
interface Parser {
    fun install(config: ContentNegotiation.Config)
}
```

**问题：**
- ❌ 无法自定义序列化配置
- ❌ 无法支持多种 Content-Type

**改进建议：**
```kotlin
interface Parser {
    fun install(config: ContentNegotiation.Config)
    fun canParse(contentType: ContentType): Boolean
    suspend fun <T> deserialize(content: ByteArray, type: KType): T
    suspend fun <T> serialize(data: T): ByteArray
}
```

---

### 🟢 小问题

#### 8. **缺少日志配置**

**改进建议：**
```kotlin
class NetworkClient {
    var logLevel: LogLevel = LogLevel.NONE
    var logger: Logger? = null
    
    init {
        install(Logging) {
            level = logLevel
            logger = this@NetworkClient.logger
        }
    }
}
```

#### 9. **缺少测试支持**

**改进建议：**
```kotlin
class NetworkClient {
    // 测试时可以注入 MockEngine
    constructor(engine: HttpClientEngine) {
        httpClient = HttpClient(engine) { ... }
    }
}
```

#### 10. **与架构文档的集成方式不明确**

**改进建议：**
- 明确 NetworkClient 在 Platform Layer 的位置
- 提供与 Data Layer 的集成示例
- 提供依赖注入的配置方式

---

## 🔧 改进后的完整设计

### 1. 改进后的 ApiResult（符合标准 API 格式）

```kotlin
// 标准 API 响应格式
@Serializable
data class ApiResponse<T>(
    @SerialName("code")
    val code: Int,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("data")
    val data: T?
)

// ApiResult：用于 UI 层的状态管理
sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    
    // 成功：只包含业务数据
    data class Success<T>(val data: T) : ApiResult<T>()
    
    sealed class Error : ApiResult<Nothing>() {
        // 网络错误（连接失败、超时等）
        data class NetworkError(
            val throwable: Throwable,
            val message: String = throwable.message ?: "网络错误"
        ) : Error()
        
        // HTTP 错误（4xx, 5xx）
        data class HttpError(
            val statusCode: Int,
            val message: String,
            val body: String? = null
        ) : Error()
        
        // 业务错误（API 返回 code != 200）
        data class BusinessError(
            val code: Int,        // API 返回的业务状态码
            val message: String,   // API 返回的错误消息
            val data: Any? = null  // 可选的错误数据
        ) : Error()
        
        // 解析错误（JSON 解析失败等）
        data class ParseError(
            val throwable: Throwable,
            val message: String = "数据解析失败"
        ) : Error()
        
        // 未知错误
        data class UnknownError(
            val throwable: Throwable,
            val message: String = "未知错误"
        ) : Error()
    }
}
```

### 2. 改进后的 NetworkClient

```kotlin
class NetworkClient private constructor(
    val baseUrl: String,
    val parser: Parser,
    val interceptors: List<HttpRequestInterceptor>,
    val cacheStrategy: CacheStrategy?,
    val retryPolicy: RetryPolicy?,
    val timeoutConfig: TimeoutConfig,
    val logLevel: LogLevel,
    val engineFactory: HttpClientEngineFactory<*>
) {
    val httpClient: HttpClient
    
    init {
        httpClient = HttpClient(engineFactory.create()) {
            // 超时配置
            install(HttpTimeout) {
                requestTimeoutMillis = timeoutConfig.requestTimeout
                connectTimeoutMillis = timeoutConfig.connectTimeout
            }
            
            // 日志
            install(Logging) {
                level = logLevel
            }
            
            // 内容协商
            install(ContentNegotiation) {
                parser.install(this)
            }
            
            // 请求拦截器
            install(HttpRequestInterceptor) {
                interceptors.forEach { interceptor ->
                    interceptor.intercept(this)
                }
            }
            
            // 响应验证
            HttpResponseValidator {
                validateResponse { response ->
                    when (response.status.value) {
                        in 400..499 -> throw HttpException(response, "客户端错误")
                        in 500..599 -> throw HttpException(response, "服务器错误")
                    }
                }
            }
        }
    }
    
    // 标准 API 调用：返回 ApiResponse<T>，自动处理 code/data/msg
    suspend inline fun <reified T: Any> get(
        path: String,
        useCache: Boolean = true
    ): ApiResponse<T> {
        val cacheKey = "$baseUrl$path"
        
        // 尝试从缓存获取
        if (useCache) {
            cacheStrategy?.get<ApiResponse<T>>(cacheKey)?.let { return it }
        }
        
        // 执行请求（带重试）
        val response = retryPolicy?.retry {
            execute { httpClient.get(baseUrl + path).body<ApiResponse<T>>() }
        } ?: execute { httpClient.get(baseUrl + path).body<ApiResponse<T>>() }
        
        // 缓存结果
        if (useCache) {
            cacheStrategy?.put(cacheKey, response)
        }
        
        return response
    }
    
    // 直接获取数据（自动处理业务错误）
    suspend inline fun <reified T: Any> getData(
        path: String,
        useCache: Boolean = true
    ): T {
        val response = get<T>(path, useCache)
        
        // 检查业务状态码
        if (response.code != 200) {
            throw BusinessException(response.code, response.message, response.data)
        }
        
        // 检查数据是否为空
        return response.data ?: throw ParseException("响应数据为空")
    }
    
    suspend inline fun <reified T: Any> post(
        path: String,
        body: Any
    ): T {
        return execute {
            httpClient.post(baseUrl + path) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<T>()
        }
    }
    
    // Flow 版本：自动处理标准 API 响应格式
    inline fun <reified T: Any> getFlow(path: String): Flow<ApiResult<T>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = get<T>(path)
            
            // 检查业务状态码
            if (response.code == 200) {
                // 成功：提取 data
                val data = response.data ?: throw ParseException("响应数据为空")
                emit(ApiResult.Success(data))
            } else {
                // 业务错误
                emit(ApiResult.Error.BusinessError(
                    code = response.code,
                    message = response.message,
                    data = response.data
                ))
            }
        } catch (e: HttpException) {
            emit(ApiResult.Error.HttpError(
                statusCode = e.response.status.value,
                message = e.message ?: "HTTP错误",
                body = e.response.bodyAsText()
            ))
        } catch (e: SerializationException) {
            emit(ApiResult.Error.ParseError(e))
        } catch (e: IOException) {
            emit(ApiResult.Error.NetworkError(e))
        } catch (e: Throwable) {
            emit(ApiResult.Error.UnknownError(e))
        }
    }
    
    class Builder {
        var baseUrl: String = ""
        var parser: Parser = JsonParser()
        val interceptors: MutableList<HttpRequestInterceptor> = mutableListOf()
        var cacheStrategy: CacheStrategy? = null
        var retryPolicy: RetryPolicy? = null
        var timeoutConfig: TimeoutConfig = TimeoutConfig()
        var logLevel: LogLevel = LogLevel.NONE
        var engineFactory: HttpClientEngineFactory<*> = createHttpClientEngine()
        
        fun build() = NetworkClient(
            baseUrl, parser, interceptors, cacheStrategy,
            retryPolicy, timeoutConfig, logLevel, engineFactory
        )
    }
}
```

### 3. 跨平台实现

```kotlin
// commonMain
expect fun createHttpClientEngine(): HttpClientEngineFactory<*>

// androidMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Android

// iosMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = Ios

// jvmMain
actual fun createHttpClientEngine(): HttpClientEngineFactory<*> = CIO
```

---

## 💡 使用示例（标准 API 格式）

### 1. 定义 API 响应模型

```kotlin
// 标准 API 响应格式
@Serializable
data class ApiResponse<T>(
    @SerialName("code")
    val code: Int,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("data")
    val data: T?
)

// 业务数据模型
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String
)
```

### 2. 使用方式

```kotlin
// 方式1：使用 Flow（推荐，自动处理业务错误）
val userFlow = networkClient.getFlow<UserDto>("/api/user/123")
    .collect { result ->
        when (result) {
            is ApiResult.Loading -> {
                // 显示加载状态
            }
            is ApiResult.Success -> {
                // 使用 result.data（已经是 UserDto，不包含 code/msg）
                println("用户：${result.data.name}")
            }
            is ApiResult.Error.BusinessError -> {
                // 业务错误（API 返回 code != 200）
                println("业务错误：${result.code} - ${result.message}")
            }
            is ApiResult.Error.NetworkError -> {
                // 网络错误
                println("网络错误：${result.message}")
            }
            // ... 其他错误类型
        }
    }

// 方式2：直接获取数据（需要手动处理异常）
try {
    val user = networkClient.getData<UserDto>("/api/user/123")
    println("用户：${user.name}")
} catch (e: BusinessException) {
    println("业务错误：${e.code} - ${e.message}")
} catch (e: NetworkException) {
    println("网络错误：${e.message}")
}

// 方式3：获取完整响应（需要手动检查 code）
val response = networkClient.get<UserDto>("/api/user/123")
if (response.code == 200) {
    val user = response.data
    println("用户：${user?.name}")
} else {
    println("错误：${response.code} - ${response.message}")
}
```

### 3. 为什么 Success 不需要 headers？

**原因：**
1. ✅ **业务数据分离**：`Success` 只包含业务数据，保持简洁
2. ✅ **标准格式**：大多数 API 使用 `{code, data, msg}` 格式，不需要 headers
3. ✅ **Headers 处理**：如果需要 headers 信息（如 Token 刷新），通过拦截器处理：
   ```kotlin
   class TokenRefreshInterceptor : HttpRequestInterceptor {
       override suspend fun intercept(request: HttpRequestBuilder) {
           // 从响应 headers 中提取新 Token
           // 保存到存储中
       }
   }
   ```
4. ✅ **分页信息**：通常包含在 `data` 中：
   ```kotlin
   @Serializable
   data class PageData<T>(
       val list: List<T>,
       val total: Int,
       val page: Int
   )
   ```

---

## 📝 最终建议

### 优先级排序

1. **🔴 高优先级（必须修复）**
   - 修复拦截器实现方式（使用 Ktor 原生机制）
   - 完善 ApiResult 设计（添加状态码、错误类型）
   - 添加跨平台实现（expect/actual）

2. **🟡 中优先级（建议添加）**
   - 添加错误处理和重试机制
   - 完善 SSE 和 WebSocket 实现
   - 添加缓存机制

3. **🟢 低优先级（可选）**
   - 添加日志配置
   - 添加测试支持
   - 优化 Parser 接口

### 推荐的文件结构

```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/
├── NetworkClient.kt          # 主客户端类
├── ApiResult.kt              # 统一返回类型
├── config/
│   ├── RetryPolicy.kt        # 重试策略
│   ├── TimeoutConfig.kt      # 超时配置
│   └── CacheStrategy.kt      # 缓存策略
├── interceptors/
│   ├── AuthInterceptor.kt   # 认证拦截器
│   └── LoggingInterceptor.kt # 日志拦截器
├── parser/
│   ├── Parser.kt             # 解析器接口
│   ├── JsonParser.kt         # JSON 解析器
│   ├── ProtoParser.kt        # ProtoBuf 解析器
│   └── XmlParser.kt          # XML 解析器
└── flow/
    └── NetworkFlow.kt        # Flow 扩展函数

shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/network/
└── HttpClientFactory.kt      # 平台特定的 HttpClient 创建
```

---

## 🎯 总结

你的设计方案**整体思路很好**，架构清晰，功能全面。主要需要改进的是：

1. **使用 Ktor 原生的拦截器机制**，而不是自定义的拦截器
2. **完善 ApiResult 设计**，添加更多错误类型和状态信息
3. **添加跨平台实现**，使用 expect/actual
4. **添加错误处理和重试机制**

按照这些建议改进后，这将是一个非常优秀的 KMP 网络请求封装框架！

---

*评估日期：2024年*
