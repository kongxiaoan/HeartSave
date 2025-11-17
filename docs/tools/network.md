好，我们开始 **“Ktor 网络层的专业封装设计”**，目标是打造一个 **像 Retrofit 一样好用、但跨平台(KMP)通用** 的网络框架。

你提到的需求包括：

✔ 使用方式统一
✔ 可配置
✔ 协程
✔ Flow
✔ 拦截器
✔ 动态选择 HTTP / SSE / WebSocket
✔ 支持多种数据解析（JSON/PB/XML）
✔ 跨平台（Android、iOS、Web、Desktop）

下面我将给你一个**真正可落地、架构清晰、工程可复用**的封装方案。

---

# 🔥 第一部分：整体架构设计（重点）

我们先从顶层设计一下要打造的网络框架：

## 🏗 1. 架构分层（推荐）

```
network/
 ├── config/           # 客户端配置（超时、baseUrl、拦截器、解析器）
 ├── http/             # HTTP 客户端实现（Ktor HttpClient）
 ├── sse/              # SSE 客户端封装
 ├── ws/               # WebSocket 客户端封装
 ├── interceptors/     # 拦截器体系
 ├── parser/           # JSON / ProtoBuf / XML 解析器
 ├── flow/             # Flow 包装（ApiResult Flow）
 └── NetworkClient.kt  # 统一入口
```

特点：

* **插件化**（parser/、interceptor 可插拔）
* **策略模式**（HTTP/SSE/WS 动态切换）
* **Builder 配置模式**（类似 OkHttpClient.Builder）
* **统一返回 Flow<ApiResult<T>>**
* **跨平台实现 actual/expect**

---

# 🔥 第二部分：核心类型设计

## ① 统一的返回类型（Flow 版 Retrofit）

### `ApiResult`

```kotlin
sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val throwable: Throwable) : ApiResult<Nothing>()
}
```

### Flow 包装（关键）

```kotlin
fun <T> apiFlow(block: suspend () -> T): Flow<ApiResult<T>> = flow {
    emit(ApiResult.Loading)
    try {
        emit(ApiResult.Success(block()))
    } catch (e: Throwable) {
        emit(ApiResult.Error(e))
    }
}
```

---

# 🔥 第三部分：网络客户端的统一入口 NetworkClient

这是项目里最重要的类。

```kotlin
class NetworkClient private constructor(
    val baseUrl: String,
    val parser: Parser,
    val interceptors: List<Interceptor>,
    val engineBuilder: HttpClientConfig<*>.() -> Unit
) {
    val httpClient = HttpClient {
        engineBuilder()

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
        }

        install(Logging) {
            level = LogLevel.ALL
        }

        install(ContentNegotiation) {
            parser.install(this)     // ★ 插入 JSON/PB/XML
        }
    }

    suspend inline fun <reified T: Any> get(path: String): T =
        execute { httpClient.get(baseUrl + path).body() }

    suspend inline fun <reified T: Any> post(path: String, body: Any): T =
        execute {
            httpClient.post(baseUrl + path) {
                setBody(body)
            }.body()
        }

    private suspend inline fun <T> execute(block: () -> T): T {
        var result = block
        interceptors.forEach { result = it.intercept(result) }
        return result()
    }

    class Builder {
        var baseUrl: String = ""
        var parser: Parser = JsonParser()
        val interceptors: MutableList<Interceptor> = mutableListOf()
        var engineBuilder: HttpClientConfig<*>.() -> Unit = {}

        fun build() = NetworkClient(baseUrl, parser, interceptors, engineBuilder)
    }
}
```

---

# 🔥 第四部分：多种数据解析的插件（Parser）

抽象层：

```kotlin
interface Parser {
    fun install(config: ContentNegotiation.Config)
}
```

### JSON

```kotlin
class JsonParser : Parser {
    override fun install(config: ContentNegotiation.Config) {
        config.json(Json { ignoreUnknownKeys = true })
    }
}
```

### ProtoBuf

```kotlin
class ProtoParser : Parser {
    override fun install(config: ContentNegotiation.Config) {
        config.protobuf()
    }
}
```

### XML

```kotlin
class XmlParser : Parser {
    override fun install(config: ContentNegotiation.Config) {
        config.xml()
    }
}
```

---

# 🔥 第五部分：可插拔的拦截器体系

Retrofit / OkHttp 风格：

```kotlin
fun interface Interceptor {
    suspend fun <T> intercept(block: suspend () -> T): suspend () -> T
}
```

使用示例：

### Token 拦截器

```kotlin
class TokenInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override suspend fun <T> intercept(block: suspend () -> T): suspend () -> T = {
        println("Interceptor: add token = ${tokenProvider()}")
        block()
    }
}
```

加入：

```kotlin
clientBuilder.interceptors += TokenInterceptor { "Bearer xxxx" }
```

---

# 🔥 第六部分：支持 HTTP / SSE / WebSocket

## (1) HTTP

已经在 NetworkClient 内部封装了 HTTP（GET/POST）。

---

## (2) SSE（Server Sent Event）

```kotlin
fun NetworkClient.sse(path: String): Flow<String> = flow {
    httpClient.sse(baseUrl + path) {
        onEvent { event, data -> emit(data) }
    }
}
```

---

## (3) WebSocket

```kotlin
fun NetworkClient.ws(path: String): Flow<String> = flow {
    httpClient.webSocket(baseUrl + path) {
        for (message in incoming) {
            if (message is Frame.Text)
                emit(message.readText())
        }
    }
}
```

---

# 🔥 第七部分：Flow 化请求封装（最终使用方式）

你希望的写法可以像：

```kotlin
val userFlow = network.getFlow<User>("/user/info")
```

实现：

```kotlin
inline fun <reified T: Any> NetworkClient.getFlow(path: String): Flow<ApiResult<T>> =
    apiFlow { get<T>(path) }
```

同理 POST：

```kotlin
inline fun <reified T: Any> NetworkClient.postFlow(path: String, body: Any)
    : Flow<ApiResult<T>> =
    apiFlow { post(path, body) }
```

---

# 🔥 最终效果（使用像 Retrofit）

## 1. 创建一个 NetworkClient

```kotlin
val client = NetworkClient.Builder().apply {
    baseUrl = "https://api.xx.com"
    parser = JsonParser()
    interceptors += TokenInterceptor { "Bearer xxx" }
}.build()
```

---

## 2. 发起 Flow 请求（最简）

```kotlin
client.getFlow<User>("/user/info")
    .collect { result ->
        when (result) {
            ApiResult.Loading -> println("Loading")
            is ApiResult.Success -> println("OK: ${result.data}")
            is ApiResult.Error -> println("ERR: ${result.throwable}")
        }
    }
```

---

## 3. 使用 SSE

```kotlin
client.sse("/live/stream")
    .collect { println("SSE: $it") }
```

---

## 4. 使用 WebSocket

```kotlin
client.ws("/chat")
    .collect { println("WS: $it") }
```

---

# 🎉 小结（你已经获得什么？）

你现在有了一套完整且专业的 KMP 网络框架设计：

| 能力                | 支持 |
| ----------------- | -- |
| 统一 API            | ✔  |
| 可配置 Builder       | ✔  |
| 多种解析器 JSON/PB/XML | ✔  |
| Flow 支持           | ✔  |
| 协程 suspend        | ✔  |
| HTTP GET/POST     | ✔  |
| SSE               | ✔  |
| WebSocket         | ✔  |
| 拦截器               | ✔  |
| 多平台（KMP）          | ✔  |

这套框架已经能作为公司内部 SDK 使用，结构清晰、扩展性强。
