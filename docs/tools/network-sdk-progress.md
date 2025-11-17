# 网络请求 SDK 实施进度

## ✅ 阶段 1：核心基础（已完成）

### 已完成的工作

#### 1. 核心类型 ✅
- ✅ `ApiResult.kt` - 统一返回类型（Loading/Success/Error）
- ✅ `ApiResponse.kt` - 标准 API 响应格式（code/data/msg）
- ✅ `NetworkException.kt` - 网络异常定义（HttpException/BusinessException/ParseException/NetworkConnectionException）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/core/`

#### 2. HttpClient 工厂（跨平台）✅
- ✅ `HttpClientFactory.kt` - expect 声明
- ✅ `HttpClientFactory.android.kt` - Android 实现
- ✅ `HttpClientFactory.ios.kt` - iOS 实现
- ✅ `HttpClientFactory.jvm.kt` - JVM 实现
- ✅ `HttpClientFactory.js.kt` - JS 实现
- ✅ `HttpClientFactory.wasmJs.kt` - WasmJs 实现

**位置**：`shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/network/factory/`

#### 3. 配置类 ✅
- ✅ `NetworkConfig.kt` - 网络配置（baseUrl、超时、重试、默认请求头）
- ✅ `TimeoutConfig.kt` - 超时配置（连接/Socket/请求超时）
- ✅ `RetryPolicy.kt` - 重试策略（最大重试次数、延迟、条件判断）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/config/`

#### 4. 解析器 ✅
- ✅ `Parser.kt` - 解析器接口
- ✅ `JsonParser.kt` - JSON 解析器实现（使用 Kotlinx Serialization）

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/parser/`

#### 5. NetworkClient 基础版本 ✅
- ✅ `NetworkClient.kt` - 主客户端类
  - Builder 模式创建
  - GET/POST 方法（返回 `ApiResponse<T>`）
  - `getData()` / `postData()` 方法（直接返回数据，自动处理业务错误）
  - 错误处理（HTTP 错误、业务错误、网络错误、解析错误）
  - 重试机制集成
  - 默认请求头支持

**位置**：`shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/core/`

#### 6. 依赖配置 ✅
- ✅ 添加 `kotlinx-coroutines-core` 依赖
- ✅ 更新 `shared/build.gradle.kts`
- ✅ 更新 `gradle/libs.versions.toml`

---

## 📋 当前可用功能

### 基础功能
1. ✅ 创建 NetworkClient
2. ✅ GET 请求（返回 `ApiResponse<T>`）
3. ✅ GET 请求（直接返回数据 `T`）
4. ✅ POST 请求（返回 `ApiResponse<T>`）
5. ✅ POST 请求（直接返回数据 `T`）
6. ✅ 错误处理（HTTP/业务/网络/解析错误）
7. ✅ 重试机制
8. ✅ 默认请求头
9. ✅ 跨平台支持（Android/iOS/JVM/JS/WasmJs）

### 使用示例

```kotlin
// 1. 创建客户端（最简单方式）
val client = NetworkClient.create("https://api.example.com")

// 2. 创建客户端（使用 Builder）
val client = NetworkClient.create("https://api.example.com") {
    timeout(TimeoutConfig.FAST)
    retryPolicy(RetryPolicy.NETWORK_ONLY)
    defaultHeader("User-Agent", "MyApp/1.0")
}

// 3. GET 请求（返回标准响应）
val response: ApiResponse<User> = client.get("/user/123")
if (response.isSuccess()) {
    val user = response.data
}

// 4. GET 请求（直接获取数据，自动处理错误）
try {
    val user: User = client.getData("/user/123")
    // 使用 user
} catch (e: BusinessException) {
    // 处理业务错误
    println("业务错误: ${e.code} - ${e.message}")
}

// 5. POST 请求
val loginRequest = LoginRequest("username", "password")
val response: ApiResponse<LoginResponse> = client.post("/login", loginRequest)

// 6. POST 请求（直接获取数据）
val loginResponse: LoginResponse = client.postData("/login", loginRequest)
```

---

## 🚀 下一步计划

### 阶段 2：拦截器体系（待实施）

**目标**：实现拦截器机制，支持认证、日志等

#### 步骤 2.1：实现 Ktor 拦截器机制
- [ ] 修改 `NetworkClient` 支持拦截器
- [ ] 使用 Ktor 的 `HttpRequestInterceptor` 和 `HttpResponseValidator`

#### 步骤 2.2：创建认证拦截器
- [ ] `AuthInterceptor.kt` - Token 认证拦截器
- [ ] 支持动态 Token 获取
- [ ] 支持 Token 刷新（可选）

#### 步骤 2.3：创建日志拦截器
- [ ] `LoggingInterceptor.kt` - 请求/响应日志
- [ ] 可配置日志级别
- [ ] 敏感信息过滤（可选）

#### 步骤 2.4：创建错误拦截器
- [ ] `ErrorInterceptor.kt` - 统一错误处理
- [ ] HTTP 错误转换
- [ ] 业务错误处理

---

### 阶段 3：Flow 支持和标准 API 格式（待实施）

**目标**：支持 Flow 和标准 API 响应格式

#### 步骤 3.1：实现 Flow 扩展
- [ ] `NetworkFlow.kt` - Flow 扩展函数
- [ ] `getFlow()` / `postFlow()` 方法
- [ ] 自动处理 `ApiResult` 状态

#### 步骤 3.2：集成标准 API 响应格式
- [ ] 修改 `NetworkClient` 支持 `ApiResponse<T>`
- [ ] 自动处理 `code/data/msg`
- [ ] 业务错误自动转换为 `BusinessError`

#### 步骤 3.3：添加 `getData()` 方法
- [ ] 直接获取数据，自动处理业务错误
- [ ] 抛出 `BusinessException` 异常

**注意**：步骤 3.2 和 3.3 实际上已经在阶段 1 中完成了！

---

### 阶段 4：高级功能（可选）

- [ ] 完善重试机制（条件重试）
- [ ] 实现缓存机制（内存缓存）
- [ ] SSE 支持
- [ ] WebSocket 支持

---

### 阶段 5：测试和文档（重要）

- [ ] 单元测试
- [ ] 集成测试
- [ ] 使用文档

---

## 📝 注意事项

1. **依赖已配置**：Ktor Client、Kotlinx Serialization、Kotlinx Coroutines 已添加到依赖中
2. **跨平台支持**：所有平台（Android/iOS/JVM/JS/WasmJs）都已实现 HttpClient 工厂
3. **标准 API 格式**：已支持标准的 `{code, data, msg}` 响应格式
4. **错误处理**：完整的错误处理体系已建立

---

## 🔄 工作流程

1. **当前状态**：阶段 1 已完成 ✅
2. **等待确认**：是否继续阶段 2（拦截器体系）？
3. **下一步**：实施阶段 2 或根据需求调整计划

---

**最后更新**：阶段 1 完成
