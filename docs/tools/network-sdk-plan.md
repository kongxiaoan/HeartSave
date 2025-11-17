# 网络请求 SDK 封装计划

## 📋 项目概述

**目标**：创建一个专业的、跨平台的网络请求 SDK，类似 Retrofit 但适用于 KMP。

**核心特性**：
- ✅ 统一 API，使用方式简洁
- ✅ 支持标准 API 响应格式（code/data/msg）
- ✅ Flow 支持，响应式编程
- ✅ 拦截器体系（认证、日志等）
- ✅ 支持 HTTP/SSE/WebSocket
- ✅ 多种数据解析（JSON/PB/XML）
- ✅ 错误处理和重试机制
- ✅ 缓存支持
- ✅ 跨平台（Android/iOS/JVM/JS/Wasm）

---

## 🏗️ SDK 结构设计

### 目录结构

```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/
├── core/                          # 核心模块
│   ├── NetworkClient.kt          # 主客户端类
│   ├── ApiResult.kt              # 统一返回类型
│   ├── ApiResponse.kt            # 标准 API 响应格式
│   └── NetworkException.kt       # 网络异常定义
│
├── config/                        # 配置模块
│   ├── NetworkConfig.kt          # 网络配置
│   ├── RetryPolicy.kt            # 重试策略
│   ├── TimeoutConfig.kt          # 超时配置
│   └── CacheStrategy.kt          # 缓存策略接口
│
├── interceptors/                  # 拦截器模块
│   ├── Interceptor.kt            # 拦截器接口
│   ├── AuthInterceptor.kt        # 认证拦截器
│   ├── LoggingInterceptor.kt     # 日志拦截器
│   └── ErrorInterceptor.kt       # 错误拦截器
│
├── parser/                        # 解析器模块
│   ├── Parser.kt                 # 解析器接口
│   ├── JsonParser.kt             # JSON 解析器
│   ├── ProtoParser.kt            # ProtoBuf 解析器（可选）
│   └── XmlParser.kt               # XML 解析器（可选）
│
├── flow/                          # Flow 扩展模块
│   ├── NetworkFlow.kt            # Flow 扩展函数
│   └── ApiResultFlow.kt          # ApiResult Flow 工具
│
├── sse/                           # SSE 模块（可选）
│   └── SSEClient.kt              # SSE 客户端封装
│
├── websocket/                     # WebSocket 模块（可选）
│   └── WebSocketClient.kt        # WebSocket 客户端封装
│
└── factory/                       # 工厂模块
    └── HttpClientFactory.kt      # HttpClient 创建工厂（expect/actual）
```

### 平台特定实现

```
shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/network/
└── factory/
    └── HttpClientFactory.{platform}.kt  # 平台特定的 HttpClient 创建
```

---

## 📝 实施计划

### 阶段 1：核心基础（必须）

**目标**：搭建 SDK 的基础框架

#### 步骤 1.1：创建核心类型 ✅
- [ ] `ApiResult.kt` - 统一返回类型（Loading/Success/Error）
- [ ] `ApiResponse.kt` - 标准 API 响应格式（code/data/msg）
- [ ] `NetworkException.kt` - 网络异常定义

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/core/
```

**预计时间**：30 分钟

---

#### 步骤 1.2：创建 HttpClient 工厂（跨平台）✅
- [ ] `HttpClientFactory.kt` - expect 声明
- [ ] `HttpClientFactory.android.kt` - Android 实现
- [ ] `HttpClientFactory.ios.kt` - iOS 实现
- [ ] `HttpClientFactory.jvm.kt` - JVM 实现
- [ ] `HttpClientFactory.js.kt` - JS 实现
- [ ] `HttpClientFactory.wasmJs.kt` - WasmJs 实现

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/factory/
shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/network/factory/
```

**预计时间**：45 分钟

---

#### 步骤 1.3：创建配置类 ✅
- [ ] `NetworkConfig.kt` - 网络配置（baseUrl、超时等）
- [ ] `TimeoutConfig.kt` - 超时配置
- [ ] `RetryPolicy.kt` - 重试策略（可选，后续添加）

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/config/
```

**预计时间**：30 分钟

---

#### 步骤 1.4：创建解析器接口和 JSON 解析器 ✅
- [ ] `Parser.kt` - 解析器接口
- [ ] `JsonParser.kt` - JSON 解析器实现

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/parser/
```

**预计时间**：30 分钟

---

#### 步骤 1.5：创建 NetworkClient 基础版本 ✅
- [ ] `NetworkClient.kt` - 主客户端类
  - Builder 模式
  - 基础 GET/POST 方法
  - 错误处理

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/core/
```

**预计时间**：1.5 小时

---

**阶段 1 总计**：约 3.5 小时

---

### 阶段 2：拦截器体系（重要）

**目标**：实现拦截器机制，支持认证、日志等

#### 步骤 2.1：实现 Ktor 拦截器机制 ✅
- [ ] 修改 `NetworkClient` 支持拦截器
- [ ] 使用 Ktor 的 `HttpRequestInterceptor` 和 `HttpResponseValidator`

**预计时间**：45 分钟

---

#### 步骤 2.2：创建认证拦截器 ✅
- [ ] `AuthInterceptor.kt` - Token 认证拦截器
- [ ] 支持动态 Token 获取
- [ ] 支持 Token 刷新（可选）

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/interceptors/
```

**预计时间**：1 小时

---

#### 步骤 2.3：创建日志拦截器 ✅
- [ ] `LoggingInterceptor.kt` - 请求/响应日志
- [ ] 可配置日志级别
- [ ] 敏感信息过滤（可选）

**预计时间**：45 分钟

---

#### 步骤 2.4：创建错误拦截器 ✅
- [ ] `ErrorInterceptor.kt` - 统一错误处理
- [ ] HTTP 错误转换
- [ ] 业务错误处理

**预计时间**：30 分钟

---

**阶段 2 总计**：约 3 小时

---

### 阶段 3：Flow 支持和标准 API 格式（重要）

**目标**：支持 Flow 和标准 API 响应格式

#### 步骤 3.1：实现 Flow 扩展 ✅
- [ ] `NetworkFlow.kt` - Flow 扩展函数
- [ ] `getFlow()` / `postFlow()` 方法
- [ ] 自动处理 `ApiResult` 状态

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/flow/
```

**预计时间**：1 小时

---

#### 步骤 3.2：集成标准 API 响应格式 ✅
- [ ] 修改 `NetworkClient` 支持 `ApiResponse<T>`
- [ ] 自动处理 `code/data/msg`
- [ ] 业务错误自动转换为 `BusinessError`

**预计时间**：1.5 小时

---

#### 步骤 3.3：添加 `getData()` 方法 ✅
- [ ] 直接获取数据，自动处理业务错误
- [ ] 抛出 `BusinessException` 异常

**预计时间**：30 分钟

---

**阶段 3 总计**：约 3 小时

---

### 阶段 4：高级功能（可选）

**目标**：添加重试、缓存等高级功能

#### 步骤 4.1：实现重试机制 ✅
- [ ] 完善 `RetryPolicy.kt`
- [ ] 在 `NetworkClient` 中集成重试
- [ ] 支持条件重试

**预计时间**：1.5 小时

---

#### 步骤 4.2：实现缓存机制 ✅
- [ ] `CacheStrategy.kt` - 缓存策略接口
- [ ] `MemoryCacheStrategy.kt` - 内存缓存实现
- [ ] 在 `NetworkClient` 中集成缓存

**预计时间**：2 小时

---

#### 步骤 4.3：实现 SSE 支持 ✅
- [ ] `SSEClient.kt` - SSE 客户端封装
- [ ] Flow 支持

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/sse/
```

**预计时间**：1.5 小时

---

#### 步骤 4.4：实现 WebSocket 支持 ✅
- [ ] `WebSocketClient.kt` - WebSocket 客户端封装
- [ ] Flow 支持

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/websocket/
```

**预计时间**：2 小时

---

**阶段 4 总计**：约 7 小时

---

### 阶段 5：测试和文档（重要）

**目标**：编写测试和文档

#### 步骤 5.1：单元测试 ✅
- [ ] `NetworkClientTest.kt` - 客户端测试
- [ ] `ApiResultTest.kt` - ApiResult 测试
- [ ] `InterceptorTest.kt` - 拦截器测试
- [ ] 使用 MockEngine 进行测试

**文件位置**：
```
shared/src/commonTest/kotlin/com/tcm/heartsave/platform/network/
```

**预计时间**：2 小时

---

#### 步骤 5.2：集成测试 ✅
- [ ] 真实 API 测试（可选）
- [ ] 跨平台测试

**预计时间**：1 小时

---

#### 步骤 5.3：使用文档 ✅
- [ ] README.md - 使用指南
- [ ] 示例代码
- [ ] 最佳实践

**文件位置**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/README.md
```

**预计时间**：1 小时

---

**阶段 5 总计**：约 4 小时

---

## 📊 总体时间估算

| 阶段 | 内容 | 预计时间 | 优先级 |
|------|------|---------|--------|
| 阶段 1 | 核心基础 | 3.5 小时 | 🔴 必须 |
| 阶段 2 | 拦截器体系 | 3 小时 | 🟡 重要 |
| 阶段 3 | Flow 和标准格式 | 3 小时 | 🟡 重要 |
| 阶段 4 | 高级功能 | 7 小时 | 🟢 可选 |
| 阶段 5 | 测试和文档 | 4 小时 | 🟡 重要 |
| **总计** | | **20.5 小时** | |

---

## 🎯 实施建议

### 最小可用版本（MVP）

**包含内容**：
- ✅ 阶段 1：核心基础
- ✅ 阶段 2：拦截器体系（至少认证拦截器）
- ✅ 阶段 3：Flow 支持和标准 API 格式

**预计时间**：约 9.5 小时

**可以做什么**：
- 基本的 HTTP GET/POST 请求
- 标准 API 响应格式处理
- Flow 支持
- 认证拦截器
- 错误处理

---

### 完整版本

**包含内容**：
- ✅ 所有阶段

**预计时间**：约 20.5 小时

**额外功能**：
- 重试机制
- 缓存支持
- SSE 支持
- WebSocket 支持
- 完整的测试和文档

---

## 📋 下一步行动

### 当前状态：**计划制定完成** ✅

### 等待确认：

1. **是否同意这个结构设计？**
   - [ ] 同意，开始实施
   - [ ] 需要调整结构

2. **选择实施方式：**
   - [ ] MVP 版本（先实现核心功能）
   - [ ] 完整版本（一次性实现所有功能）
   - [ ] 分阶段实施（按阶段逐步实施）

3. **优先级确认：**
   - [ ] 阶段 1（核心基础）- 必须
   - [ ] 阶段 2（拦截器）- 重要
   - [ ] 阶段 3（Flow 和标准格式）- 重要
   - [ ] 阶段 4（高级功能）- 可选
   - [ ] 阶段 5（测试和文档）- 重要

---

## 🔄 工作流程

1. **你确认计划** → 回复 "开始阶段 X" 或 "调整计划"
2. **我实施代码** → 创建对应的文件
3. **你测试验证** → 确认功能正常
4. **继续下一步** → 重复流程

---

## 📝 注意事项

1. **依赖管理**：确保已添加 Ktor Client 和 Kotlinx Serialization 依赖
2. **跨平台实现**：每个平台都需要实现 `HttpClientFactory`
3. **测试优先**：建议先写测试，再写实现
4. **文档同步**：代码完成后及时更新文档

---

**等待你的确认，然后开始实施！** 🚀

---

*计划制定日期：2024年*
