# KMP 项目开发流程指南

## 📋 目录

1. [开发流程概述](#开发流程概述)
2. [代码应该在哪里写](#代码应该在哪里写)
3. [开发工作流](#开发工作流)
4. [平台特定代码处理](#平台特定代码处理)
5. [最佳实践](#最佳实践)
6. [常见开发场景](#常见开发场景)

---

## 开发流程概述

### KMP 项目的核心思想

Kotlin Multiplatform (KMP) 项目的核心思想是：**一次编写，多平台运行**。通过共享业务逻辑，减少重复代码，提高开发效率。

### 项目架构层次

```
┌─────────────────────────────────────────┐
│         composeApp (UI 层)              │
│  ┌───────────────────────────────────┐  │
│  │  commonMain: 共享 UI 代码          │  │
│  │  platformMain: 平台特定 UI 入口    │  │
│  └───────────────────────────────────┘  │
└──────────────┬──────────────────────────┘
               │ 依赖
┌──────────────▼──────────────────────────┐
│         shared (业务逻辑层)              │
│  ┌───────────────────────────────────┐  │
│  │  commonMain: 共享业务逻辑          │  │
│  │  platformMain: 平台特定实现       │  │
│  └───────────────────────────────────┘  │
└──────────────┬──────────────────────────┘
               │ 依赖
┌──────────────▼──────────────────────────┐
│         server (后端服务层)              │
│  ┌───────────────────────────────────┐  │
│  │  独立的后端服务代码                │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 代码应该在哪里写

### 🎯 快速决策树

```
新功能开发
│
├─ UI 相关？
│  └─ 是 → composeApp/src/commonMain/kotlin/
│     └─ 需要平台特定 UI？ → composeApp/src/{platform}Main/kotlin/
│
├─ 业务逻辑？
│  └─ 是 → shared/src/commonMain/kotlin/
│     └─ 需要平台特定实现？ → shared/src/{platform}Main/kotlin/
│
└─ 后端服务？
   └─ 是 → server/src/main/kotlin/
```

### 📁 详细代码位置指南

#### 1. UI 代码（Compose Multiplatform）

##### ✅ 共享 UI 代码 → `composeApp/src/commonMain/kotlin/`

**适用场景**：
- Compose UI 组件（Screen、Composable 函数）
- UI 状态管理（ViewModel、State）
- 导航逻辑
- 主题配置
- 共享的 UI 工具函数

**示例**：
```kotlin
// composeApp/src/commonMain/kotlin/com/tcm/heartsave/App.kt
@Composable
fun App() {
    MaterialTheme {
        // 共享的 UI 代码
    }
}

// composeApp/src/commonMain/kotlin/com/tcm/heartsave/screens/HomeScreen.kt
@Composable
fun HomeScreen() {
    // 所有平台共享的屏幕
}
```

##### ⚠️ 平台特定 UI 代码 → `composeApp/src/{platform}Main/kotlin/`

**适用场景**：
- Android 特定的 UI 配置（如权限请求 UI）
- iOS 特定的 UI 行为
- Desktop 特定的窗口配置
- Web 特定的 HTML/CSS 集成

**示例**：
```kotlin
// composeApp/src/androidMain/kotlin/com/tcm/heartsave/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

// composeApp/src/jvmMain/kotlin/com/tcm/heartsave/Main.kt
fun main() {
    application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}
```

#### 2. 业务逻辑代码（Shared Module）

##### ✅ 共享业务逻辑 → `shared/src/commonMain/kotlin/`

**适用场景**：
- 数据模型（Data Class、Sealed Class）
- 业务逻辑类（Service、Repository、UseCase）
- 数据验证逻辑
- 算法实现
- 工具类和扩展函数
- 接口定义（expect 声明）

**示例**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/model/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String
)

// shared/src/commonMain/kotlin/com/tcm/heartsave/repository/UserRepository.kt
class UserRepository {
    suspend fun getUser(id: String): User {
        // 共享的业务逻辑
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/Platform.kt
expect class Platform() {
    val name: String
}
```

##### ⚠️ 平台特定实现 → `shared/src/{platform}Main/kotlin/`

**适用场景**：
- 平台特定的 API 调用（actual 实现）
- 平台特定的存储实现
- 平台特定的网络实现
- 平台特定的权限处理

**示例**：
```kotlin
// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/Platform.android.kt
actual class Platform actual constructor() {
    actual val name: String = "Android"
}

// shared/src/iosMain/kotlin/com/tcm/heartsave/platform/Platform.ios.kt
actual class Platform actual constructor() {
    actual val name: String = "iOS"
}
```

#### 3. 后端服务代码

##### ✅ 服务器代码 → `server/src/main/kotlin/`

**适用场景**：
- Ktor 路由定义
- API 端点实现
- 服务器配置
- 中间件设置

**示例**：
```kotlin
// server/src/main/kotlin/com/tcm/heartsave/Application.kt
fun Application.module() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
    }
}
```

#### 4. iOS 原生代码

##### ✅ Swift 代码 → `iosApp/iosApp/`

**适用场景**：
- iOS 应用入口点
- SwiftUI 集成（如果需要）
- iOS 特定的配置

**示例**：
```swift
// iosApp/iosApp/iOSApp.swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // iOS 特定初始化
                }
        }
    }
}
```

---

## 开发工作流

### 🔄 标准开发流程

#### 1. 需求分析阶段

```
需求分析
  ↓
确定功能类型（UI / 业务逻辑 / 后端）
  ↓
确定是否需要平台特定实现
```

#### 2. 代码编写阶段

**步骤 1：编写共享代码**
- 在 `commonMain` 中编写核心逻辑
- 使用 `expect` 声明平台特定接口

**步骤 2：实现平台特定代码（如需要）**
- 在各平台的 `{platform}Main` 中实现 `actual`
- 确保所有平台都有实现

**步骤 3：编写 UI（如果是 UI 功能）**
- 在 `composeApp/commonMain` 中编写 Compose UI
- 调用 shared 模块的业务逻辑

#### 3. 测试阶段

```
单元测试（commonTest）
  ↓
平台特定测试（{platform}Test）
  ↓
集成测试
```

#### 4. 构建和运行

```bash
# Android
./gradlew :composeApp:assembleDebug

# Desktop
./gradlew :composeApp:run

# Web
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# iOS
# 在 Xcode 中打开 iosApp/iosApp.xcodeproj
```

---

## 平台特定代码处理

### expect/actual 机制

#### 使用场景

当需要访问平台特定的 API 时，使用 `expect/actual` 机制：

**1. 在 commonMain 中声明 expect**
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/storage/Storage.kt
expect class Storage() {
    suspend fun save(key: String, value: String)
    suspend fun load(key: String): String?
}
```

**2. 在各平台实现 actual**
```kotlin
// shared/src/androidMain/kotlin/com/tcm/heartsave/storage/Storage.android.kt
actual class Storage actual constructor() {
    private val prefs = context.getSharedPreferences(...)
    
    actual suspend fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    actual suspend fun load(key: String): String? {
        return prefs.getString(key, null)
    }
}

// shared/src/iosMain/kotlin/com/tcm/heartsave/storage/Storage.ios.kt
actual class Storage actual constructor() {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual suspend fun save(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }
    
    actual suspend fun load(key: String): String? {
        return userDefaults.stringForKey(key)
    }
}
```

### 平台检测

如果需要运行时平台检测：

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/Platform.kt
expect fun getPlatformName(): String

// 使用
val platform = getPlatformName()
when {
    platform.contains("Android") -> { /* Android 逻辑 */ }
    platform.contains("iOS") -> { /* iOS 逻辑 */ }
    else -> { /* 其他平台 */ }
}
```

---

## 最佳实践

### ✅ 应该做的

1. **优先使用 commonMain**
   - 尽量将代码写在 `commonMain` 中
   - 只有真正需要平台特定实现时才使用 `{platform}Main`

2. **清晰的模块职责**
   - `composeApp`: 只负责 UI
   - `shared`: 只负责业务逻辑
   - `server`: 只负责后端服务

3. **使用 expect/actual 而非条件编译**
   ```kotlin
   // ✅ 好的做法
   expect class Platform()
   actual class Platform actual constructor() { ... }
   
   // ❌ 避免的做法
   #if ANDROID
   // Android 代码
   #elseif IOS
   // iOS 代码
   #endif
   ```

4. **保持包结构一致**
   - 所有平台的相同功能使用相同的包名
   - 例如：`com.tcm.heartsave.storage` 在所有平台都相同

5. **测试驱动开发**
   - 先写 `commonTest` 测试
   - 再写平台特定测试

### ❌ 不应该做的

1. **不要在 commonMain 中导入平台特定库**
   ```kotlin
   // ❌ 错误
   import android.content.Context  // 不能在 commonMain 中使用
   
   // ✅ 正确
   // 使用 expect/actual 机制
   ```

2. **不要重复代码**
   - 如果逻辑可以在 commonMain 中实现，就不要在各平台重复

3. **不要忽略平台差异**
   - 如果某个平台有特殊需求，使用 expect/actual 处理

4. **不要在 UI 层写业务逻辑**
   - UI 层只负责展示和用户交互
   - 业务逻辑应该在 shared 模块

---

## 常见开发场景

### 场景 1：添加新的 UI 屏幕

**步骤**：
1. 在 `composeApp/src/commonMain/kotlin/com/tcm/heartsave/screens/` 创建新屏幕
2. 在 `shared/src/commonMain/kotlin/` 创建对应的 ViewModel（如果需要）
3. 在导航配置中添加路由

**示例**：
```kotlin
// composeApp/src/commonMain/kotlin/com/tcm/heartsave/screens/ProfileScreen.kt
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    
    Column {
        Text("Profile: ${state.userName}")
    }
}
```

### 场景 2：添加数据存储功能

**步骤**：
1. 在 `shared/src/commonMain/kotlin/` 定义存储接口（expect）
2. 在各平台实现存储（actual）
3. 在业务逻辑中使用存储接口

**示例**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/storage/AppStorage.kt
expect class AppStorage() {
    suspend fun saveUser(user: User)
    suspend fun getUser(): User?
}

// 在业务逻辑中使用
class UserRepository(private val storage: AppStorage) {
    suspend fun saveUser(user: User) {
        storage.saveUser(user)
    }
}
```

### 场景 3：添加网络请求功能

**步骤**：
1. 在 `shared/src/commonMain/kotlin/` 定义网络客户端接口
2. 使用 Ktor 等跨平台网络库
3. 在业务逻辑中使用网络客户端

**示例**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/network/ApiClient.kt
class ApiClient {
    private val client = HttpClient {
        // Ktor 配置
    }
    
    suspend fun getUser(id: String): User {
        return client.get("https://api.example.com/users/$id")
    }
}
```

### 场景 4：添加平台特定功能（如相机）

**步骤**：
1. 在 `shared/src/commonMain/kotlin/` 定义 expect 接口
2. 在各平台实现 actual
3. 在 UI 中调用接口

**示例**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/camera/Camera.kt
expect class Camera() {
    suspend fun takePhoto(): ByteArray?
}

// shared/src/androidMain/kotlin/com/tcm/heartsave/camera/Camera.android.kt
actual class Camera actual constructor() {
    actual suspend fun takePhoto(): ByteArray? {
        // Android CameraX 实现
    }
}

// shared/src/iosMain/kotlin/com/tcm/heartsave/camera/Camera.ios.kt
actual class Camera actual constructor() {
    actual suspend fun takePhoto(): ByteArray? {
        // iOS AVFoundation 实现
    }
}
```

### 场景 5：添加后端 API

**步骤**：
1. 在 `server/src/main/kotlin/` 添加路由
2. 在 `shared/src/commonMain/kotlin/` 定义 API 客户端
3. 在 UI 中调用 API

**示例**：
```kotlin
// server/src/main/kotlin/com/tcm/heartsave/routes/UserRoutes.kt
fun Application.userRoutes() {
    routing {
        route("/api/users") {
            get {
                call.respond(listOf<User>())
            }
        }
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/api/UserApi.kt
class UserApi(private val client: HttpClient) {
    suspend fun getUsers(): List<User> {
        return client.get("/api/users")
    }
}
```

---

## 📝 开发检查清单

在开始开发新功能前，请确认：

- [ ] 功能类型已确定（UI / 业务逻辑 / 后端）
- [ ] 代码位置已确定（commonMain / platformMain）
- [ ] 是否需要平台特定实现
- [ ] 如果需要，expect/actual 已定义
- [ ] 测试代码已编写
- [ ] 所有平台都能正常编译

---

## 🔗 相关文档

- [项目结构解析](./项目结构解析.md)
- [Kotlin Multiplatform 官方文档](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform 文档](https://github.com/JetBrains/compose-multiplatform)

---

*最后更新：2024年*
