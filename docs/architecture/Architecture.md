# HeartSave 架构设计文档

## 📋 目录

1. [架构概述](#架构概述)
2. [分层架构](#分层架构)
3. [依赖关系](#依赖关系)
4. [设计原则](#设计原则)
5. [模块职责](#模块职责)
6. [依赖注入策略](#依赖注入策略)
7. [测试策略](#测试策略)
8. [代码组织](#代码组织)
9. [最佳实践](#最佳实践)

---

## 架构概述

### 核心设计目标

1. **低耦合**：各层之间通过接口通信，减少直接依赖
2. **可替换**：任何实现都可以被替换，不影响其他层
3. **高测试性**：每层都可以独立测试，使用 Mock 对象

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  composeApp/                                          │ │
│  │  ├── UI Components (Compose)                           │ │
│  │  ├── ViewModels                                       │ │
│  │  └── Navigation                                       │ │
│  └───────────────────────────────────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │ 依赖接口
┌───────────────────────▼─────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  shared/src/commonMain/domain/                       │ │
│  │  ├── UseCases (业务用例)                              │ │
│  │  ├── Models (领域模型)                                │ │
│  │  ├── Repository Interfaces (仓库接口)                 │ │
│  │  └── Business Rules (业务规则)                        │ │
│  └───────────────────────────────────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │ 依赖接口
┌───────────────────────▼─────────────────────────────────────┐
│                       Data Layer                            │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  shared/src/commonMain/data/                         │ │
│  │  ├── Repository Implementations (仓库实现)            │ │
│  │  ├── Data Sources (数据源接口)                        │ │
│  │  ├── DTOs (数据传输对象)                              │ │
│  │  └── Mappers (数据映射)                               │ │
│  └───────────────────────────────────────────────────────┘ │
└───────────────────────┬─────────────────────────────────────┘
                        │ 依赖接口
┌───────────────────────▼─────────────────────────────────────┐
│                    Platform Layer                           │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  shared/src/{platform}Main/                          │ │
│  │  ├── Network (网络实现)                               │ │
│  │  ├── Storage (存储实现)                               │ │
│  │  ├── File System (文件系统)                           │ │
│  │  └── Device APIs (设备 API)                           │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 分层架构

### 1. Presentation Layer (表现层)

**位置**：`composeApp/src/commonMain/kotlin/`

**职责**：
- UI 组件渲染（Compose）
- 用户交互处理
- UI 状态管理（ViewModel）
- 导航逻辑

**设计原则**：
- ✅ **只依赖 Domain Layer 的接口**
- ✅ **不包含业务逻辑**
- ✅ **通过 ViewModel 访问业务逻辑**
- ✅ **UI 组件应该是纯函数式组件**

**包结构**：
```
composeApp/src/commonMain/kotlin/com/tcm/heartsave/
├── ui/
│   ├── components/          # 可复用的 UI 组件
│   ├── screens/             # 屏幕级组件
│   └── theme/               # 主题配置
├── viewmodel/               # ViewModel 类
│   ├── HomeViewModel.kt
│   └── ProfileViewModel.kt
└── navigation/              # 导航配置
    └── NavGraph.kt
```

**示例代码**：
```kotlin
// composeApp/src/commonMain/kotlin/com/tcm/heartsave/viewmodel/HomeViewModel.kt
class HomeViewModel(
    private val getUserUseCase: GetUserUseCase  // 依赖 Domain Layer 接口
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = getUserUseCase.invoke(userId)
                _uiState.update { it.copy(user = user, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}

// composeApp/src/commonMain/kotlin/com/tcm/heartsave/ui/screens/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        else -> UserContent(uiState.user)
    }
}
```

**测试性**：
- ✅ ViewModel 可以注入 Mock UseCase 进行测试
- ✅ UI 组件可以独立测试，不依赖真实 ViewModel

---

### 2. Domain Layer (领域层)

**位置**：`shared/src/commonMain/kotlin/domain/`

**职责**：
- 业务逻辑封装（UseCase）
- 领域模型定义
- 业务规则验证
- Repository 接口定义（依赖倒置）

**设计原则**：
- ✅ **不依赖任何其他层**
- ✅ **只定义接口，不包含实现**
- ✅ **包含核心业务逻辑**
- ✅ **使用 UseCase 模式封装业务用例**

**包结构**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/domain/
├── model/                   # 领域模型
│   ├── User.kt
│   └── Product.kt
├── repository/              # Repository 接口（依赖倒置）
│   ├── UserRepository.kt
│   └── ProductRepository.kt
├── usecase/                 # UseCase 类
│   ├── GetUserUseCase.kt
│   ├── CreateUserUseCase.kt
│   └── UpdateUserUseCase.kt
└── exception/               # 领域异常
    └── DomainException.kt
```

**示例代码**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/domain/model/User.kt
data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Long
) {
    init {
        require(name.isNotBlank()) { "用户名不能为空" }
        require(email.contains("@")) { "邮箱格式不正确" }
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/domain/repository/UserRepository.kt
interface UserRepository {
    suspend fun getUserById(id: String): Result<User>
    suspend fun createUser(user: User): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteUser(id: String): Result<Unit>
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/domain/usecase/GetUserUseCase.kt
class GetUserUseCase(
    private val userRepository: UserRepository  // 依赖接口，不依赖实现
) {
    suspend operator fun invoke(userId: String): User {
        return userRepository.getUserById(userId)
            .getOrElse { throw UserNotFoundException(userId) }
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/domain/usecase/CreateUserUseCase.kt
class CreateUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, email: String): Result<User> {
        // 业务规则验证
        if (name.isBlank()) {
            return Result.failure(ValidationException("用户名不能为空"))
        }
        if (!email.contains("@")) {
            return Result.failure(ValidationException("邮箱格式不正确"))
        }
        
        val user = User(
            id = generateId(),
            name = name,
            email = email,
            createdAt = currentTimeMillis()
        )
        
        return userRepository.createUser(user)
    }
}
```

**测试性**：
- ✅ UseCase 可以注入 Mock Repository 进行测试
- ✅ 业务逻辑可以完全独立测试，不依赖外部
- ✅ 领域模型可以独立验证

---

### 3. Data Layer (数据层)

**位置**：`shared/src/commonMain/kotlin/data/`

**职责**：
- Repository 接口的实现
- 数据源抽象（Remote、Local）
- DTO 与 Domain Model 的转换
- 数据缓存策略

**设计原则**：
- ✅ **实现 Domain Layer 定义的接口**
- ✅ **依赖 Platform Layer 的接口**
- ✅ **处理数据转换（DTO ↔ Domain Model）**
- ✅ **可以包含多个数据源（Remote、Local、Cache）**

**包结构**：
```
shared/src/commonMain/kotlin/com/tcm/heartsave/data/
├── repository/              # Repository 实现
│   └── UserRepositoryImpl.kt
├── datasource/              # 数据源接口和实现
│   ├── remote/             # 远程数据源
│   │   ├── UserRemoteDataSource.kt
│   │   └── UserApi.kt
│   ├── local/              # 本地数据源
│   │   └── UserLocalDataSource.kt
│   └── cache/              # 缓存数据源
│       └── UserCacheDataSource.kt
├── dto/                    # 数据传输对象
│   └── UserDto.kt
└── mapper/                 # 数据映射器
    └── UserMapper.kt
```

**示例代码**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/data/dto/UserDto.kt
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("created_at") val createdAt: Long
)

// shared/src/commonMain/kotlin/com/tcm/heartsave/data/mapper/UserMapper.kt
object UserMapper {
    fun UserDto.toDomain(): User {
        return User(
            id = id,
            name = name,
            email = email,
            createdAt = createdAt
        )
    }
    
    fun User.toDto(): UserDto {
        return UserDto(
            id = id,
            name = name,
            email = email,
            createdAt = createdAt
        )
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/data/datasource/remote/UserRemoteDataSource.kt
interface UserRemoteDataSource {
    suspend fun getUserById(id: String): Result<UserDto>
    suspend fun createUser(userDto: UserDto): Result<UserDto>
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/data/datasource/local/UserLocalDataSource.kt
interface UserLocalDataSource {
    suspend fun getUserById(id: String): UserDto?
    suspend fun saveUser(userDto: UserDto)
    suspend fun deleteUser(id: String)
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,  // 依赖接口
    private val localDataSource: UserLocalDataSource,   // 依赖接口
    private val mapper: UserMapper = UserMapper()
) : UserRepository {  // 实现 Domain Layer 的接口
    
    override suspend fun getUserById(id: String): Result<User> {
        // 1. 先尝试从本地获取
        localDataSource.getUserById(id)?.let { dto ->
            return Result.success(mapper.toDomain(dto))
        }
        
        // 2. 从远程获取
        return remoteDataSource.getUserById(id)
            .map { dto ->
                // 3. 保存到本地
                localDataSource.saveUser(dto)
                mapper.toDomain(dto)
            }
    }
    
    override suspend fun createUser(user: User): Result<Unit> {
        return remoteDataSource.createUser(mapper.toDto(user))
            .map { dto ->
                localDataSource.saveUser(dto)
            }
    }
    
    override suspend fun updateUser(user: User): Result<Unit> {
        // 实现更新逻辑
        return Result.success(Unit)
    }
    
    override suspend fun deleteUser(id: String): Result<Unit> {
        // 实现删除逻辑
        return Result.success(Unit)
    }
}
```

**测试性**：
- ✅ Repository 实现可以注入 Mock DataSource 进行测试
- ✅ 数据转换逻辑可以独立测试
- ✅ 可以测试不同数据源的组合策略

---

### 4. Platform Layer (平台层)

**位置**：`shared/src/{platform}Main/kotlin/`

**职责**：
- 平台特定的 API 实现
- 网络请求实现
- 本地存储实现
- 文件系统操作
- 设备功能访问

**设计原则**：
- ✅ **实现 Data Layer 定义的接口**
- ✅ **使用 expect/actual 机制**
- ✅ **封装平台特定细节**
- ✅ **提供统一的接口给上层使用**

**包结构**：
```
shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/
├── network/                # 网络实现
│   ├── HttpClient.kt
│   └── NetworkClient.kt
├── storage/                # 存储实现
│   ├── KeyValueStorage.kt
│   └── DatabaseStorage.kt
└── file/                   # 文件系统
    └── FileManager.kt
```

**示例代码**：
```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/network/NetworkClient.kt
expect class NetworkClient {
    suspend fun get(url: String): String
    suspend fun post(url: String, body: String): String
}

// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/network/NetworkClient.android.kt
actual class NetworkClient actual constructor() {
    private val client = OkHttpClient()
    
    actual suspend fun get(url: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response.body?.string() ?: "")
            }
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
    
    actual suspend fun post(url: String, body: String): String {
        // Android 实现
    }
}

// shared/src/iosMain/kotlin/com/tcm/heartsave/platform/network/NetworkClient.ios.kt
actual class NetworkClient actual constructor() {
    actual suspend fun get(url: String): String {
        // iOS URLSession 实现
    }
    
    actual suspend fun post(url: String, body: String): String {
        // iOS 实现
    }
}

// shared/src/commonMain/kotlin/com/tcm/heartsave/platform/storage/KeyValueStorage.kt
expect class KeyValueStorage {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
}

// shared/src/androidMain/kotlin/com/tcm/heartsave/platform/storage/KeyValueStorage.android.kt
actual class KeyValueStorage actual constructor() {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    actual suspend fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    actual suspend fun get(key: String): String? {
        return prefs.getString(key, null)
    }
    
    actual suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
```

**测试性**：
- ✅ 平台实现可以在各平台独立测试
- ✅ 可以通过 Mock 替换平台实现进行测试

---

## 依赖关系

### 依赖方向规则

```
Presentation Layer
    ↓ (依赖)
Domain Layer
    ↓ (依赖)
Data Layer
    ↓ (依赖)
Platform Layer
```

**关键原则**：
- ✅ **单向依赖**：上层依赖下层，下层不依赖上层
- ✅ **接口依赖**：所有依赖都通过接口，不依赖具体实现
- ✅ **依赖倒置**：Domain Layer 定义接口，Data Layer 实现接口

### 依赖注入

**推荐方式**：构造函数注入

```kotlin
// ✅ 好的做法：通过构造函数注入
class GetUserUseCase(
    private val userRepository: UserRepository  // 接口类型
)

// ❌ 避免：直接创建依赖
class GetUserUseCase {
    private val userRepository = UserRepositoryImpl()  // 具体实现
}

// ❌ 避免：使用单例或全局变量
object UserRepositorySingleton  // 难以测试和替换
```

---

## 设计原则

### 1. 单一职责原则 (SRP)

每个类/模块只负责一个职责：

```kotlin
// ✅ 好的做法：职责分离
class GetUserUseCase { }  // 只负责获取用户
class CreateUserUseCase { }  // 只负责创建用户

// ❌ 避免：职责混合
class UserUseCase {  // 包含所有用户相关操作
    fun getUser() { }
    fun createUser() { }
    fun updateUser() { }
    fun deleteUser() { }
}
```

### 2. 依赖倒置原则 (DIP)

高层模块不依赖低层模块，都依赖抽象：

```kotlin
// ✅ 好的做法：Domain Layer 定义接口
// domain/repository/UserRepository.kt
interface UserRepository { }

// data/repository/UserRepositoryImpl.kt
class UserRepositoryImpl : UserRepository { }

// ❌ 避免：Domain Layer 依赖具体实现
class GetUserUseCase(
    private val repository: UserRepositoryImpl  // 依赖具体实现
)
```

### 3. 接口隔离原则 (ISP)

接口应该小而专一：

```kotlin
// ✅ 好的做法：接口分离
interface UserReader {
    suspend fun getUser(id: String): User
}

interface UserWriter {
    suspend fun createUser(user: User)
    suspend fun updateUser(user: User)
}

// ❌ 避免：大而全的接口
interface UserRepository {
    suspend fun getUser(id: String): User
    suspend fun createUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun deleteUser(id: String)
    suspend fun getAllUsers(): List<User>
    suspend fun searchUsers(query: String): List<User>
    // ... 太多方法
}
```

### 4. 开闭原则 (OCP)

对扩展开放，对修改关闭：

```kotlin
// ✅ 好的做法：通过接口扩展
interface UserRepository { }
class DatabaseUserRepository : UserRepository { }
class ApiUserRepository : UserRepository { }  // 新增实现，不需要修改现有代码

// ❌ 避免：修改现有代码来支持新功能
class UserRepository {
    fun getUser(source: String) {  // 通过参数扩展
        when (source) {
            "database" -> { }
            "api" -> { }  // 新增时需要修改代码
        }
    }
}
```

---

## 模块职责

### composeApp 模块

**职责**：
- UI 渲染
- 用户交互
- ViewModel 管理
- 导航

**依赖**：
- Domain Layer (UseCase 接口)
- Compose Multiplatform
- Material3

**不包含**：
- ❌ 业务逻辑
- ❌ 数据访问代码
- ❌ 平台特定代码（除了入口点）

### shared 模块

**职责**：
- Domain Layer（业务逻辑）
- Data Layer（数据访问）
- Platform Layer（平台抽象）

**依赖**：
- Kotlin 标准库
- Coroutines
- Serialization（如需要）

**不包含**：
- ❌ UI 代码
- ❌ 平台特定的 UI 框架

### server 模块

**职责**：
- API 端点
- 路由配置
- 服务器中间件

**依赖**：
- Ktor
- shared 模块（共享业务逻辑）

---

## 依赖注入策略

### 推荐方案：手动依赖注入

**优点**：
- ✅ 无需额外依赖
- ✅ 编译时安全
- ✅ 易于理解

**实现方式**：

```kotlin
// shared/src/commonMain/kotlin/com/tcm/heartsave/di/AppContainer.kt
class AppContainer {
    // Platform Layer
    private val networkClient: NetworkClient = NetworkClient()
    private val storage: KeyValueStorage = KeyValueStorage()
    
    // Data Layer
    private val userRemoteDataSource: UserRemoteDataSource = 
        UserRemoteDataSourceImpl(networkClient)
    private val userLocalDataSource: UserLocalDataSource = 
        UserLocalDataSourceImpl(storage)
    private val userRepository: UserRepository = 
        UserRepositoryImpl(userRemoteDataSource, userLocalDataSource)
    
    // Domain Layer
    val getUserUseCase: GetUserUseCase = GetUserUseCase(userRepository)
    val createUserUseCase: CreateUserUseCase = CreateUserUseCase(userRepository)
}

// composeApp/src/commonMain/kotlin/com/tcm/heartsave/App.kt
@Composable
fun App() {
    val container = remember { AppContainer() }
    
    MaterialTheme {
        NavGraph(
            getUserUseCase = container.getUserUseCase,
            createUserUseCase = container.createUserUseCase
        )
    }
}
```

### 可选方案：Koin（如果需要）

如果项目复杂度增加，可以考虑使用 Koin：

```kotlin
// 添加 Koin 依赖
val koinVersion = "3.5.0"

// 定义模块
val appModule = module {
    // Platform
    single<NetworkClient> { NetworkClient() }
    single<KeyValueStorage> { KeyValueStorage() }
    
    // Data
    single<UserRemoteDataSource> { UserRemoteDataSourceImpl(get()) }
    single<UserLocalDataSource> { UserLocalDataSourceImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    
    // Domain
    factory<GetUserUseCase> { GetUserUseCase(get()) }
    factory<CreateUserUseCase> { CreateUserUseCase(get()) }
}

// 初始化
startKoin {
    modules(appModule)
}

// 使用
class HomeViewModel : ViewModel() {
    private val getUserUseCase: GetUserUseCase by inject()
}
```

---

## 测试策略

### 1. Domain Layer 测试

**目标**：测试业务逻辑，不依赖外部

```kotlin
// shared/src/commonTest/kotlin/com/tcm/heartsave/domain/usecase/GetUserUseCaseTest.kt
class GetUserUseCaseTest {
    @Test
    fun `should return user when repository returns success`() = runTest {
        // Given
        val mockRepository = mockk<UserRepository>()
        val expectedUser = User(id = "1", name = "Test", email = "test@test.com", createdAt = 0)
        every { mockRepository.getUserById("1") } returns Result.success(expectedUser)
        
        val useCase = GetUserUseCase(mockRepository)
        
        // When
        val result = useCase("1")
        
        // Then
        assertEquals(expectedUser, result)
        verify { mockRepository.getUserById("1") }
    }
    
    @Test
    fun `should throw exception when repository returns failure`() = runTest {
        // Given
        val mockRepository = mockk<UserRepository>()
        every { mockRepository.getUserById("1") } returns Result.failure(Exception("Not found"))
        
        val useCase = GetUserUseCase(mockRepository)
        
        // When & Then
        assertFailsWith<UserNotFoundException> {
            useCase("1")
        }
    }
}
```

### 2. Data Layer 测试

**目标**：测试数据转换和 Repository 实现

```kotlin
// shared/src/commonTest/kotlin/com/tcm/heartsave/data/repository/UserRepositoryImplTest.kt
class UserRepositoryImplTest {
    @Test
    fun `should return user from local when available`() = runTest {
        // Given
        val mockLocalDataSource = mockk<UserLocalDataSource>()
        val mockRemoteDataSource = mockk<UserRemoteDataSource>()
        val userDto = UserDto("1", "Test", "test@test.com", 0)
        
        every { mockLocalDataSource.getUserById("1") } returns userDto
        
        val repository = UserRepositoryImpl(mockRemoteDataSource, mockLocalDataSource)
        
        // When
        val result = repository.getUserById("1")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("Test", result.getOrNull()?.name)
        verify(exactly = 0) { mockRemoteDataSource.getUserById(any()) }
    }
    
    @Test
    fun `should fetch from remote when local not available`() = runTest {
        // Given
        val mockLocalDataSource = mockk<UserLocalDataSource>()
        val mockRemoteDataSource = mockk<UserRemoteDataSource>()
        val userDto = UserDto("1", "Test", "test@test.com", 0)
        
        every { mockLocalDataSource.getUserById("1") } returns null
        every { mockRemoteDataSource.getUserById("1") } returns Result.success(userDto)
        every { mockLocalDataSource.saveUser(any()) } just Runs
        
        val repository = UserRepositoryImpl(mockRemoteDataSource, mockLocalDataSource)
        
        // When
        val result = repository.getUserById("1")
        
        // Then
        assertTrue(result.isSuccess)
        verify { mockRemoteDataSource.getUserById("1") }
        verify { mockLocalDataSource.saveUser(userDto) }
    }
}
```

### 3. Presentation Layer 测试

**目标**：测试 ViewModel 和 UI 逻辑

```kotlin
// composeApp/src/commonTest/kotlin/com/tcm/heartsave/viewmodel/HomeViewModelTest.kt
class HomeViewModelTest {
    @Test
    fun `should load user successfully`() = runTest {
        // Given
        val mockUseCase = mockk<GetUserUseCase>()
        val expectedUser = User("1", "Test", "test@test.com", 0)
        coEvery { mockUseCase("1") } returns expectedUser
        
        val viewModel = HomeViewModel(mockUseCase)
        
        // When
        viewModel.loadUser("1")
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedUser, state.user)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }
    
    @Test
    fun `should handle error when use case fails`() = runTest {
        // Given
        val mockUseCase = mockk<GetUserUseCase>()
        coEvery { mockUseCase("1") } throws Exception("Network error")
        
        val viewModel = HomeViewModel(mockUseCase)
        
        // When
        viewModel.loadUser("1")
        
        // Then
        val state = viewModel.uiState.value
        assertNull(state.user)
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }
}
```

### 4. 集成测试

**目标**：测试多个层的协作

```kotlin
// shared/src/commonTest/kotlin/com/tcm/heartsave/integration/UserFlowTest.kt
class UserFlowIntegrationTest {
    @Test
    fun `should create and retrieve user`() = runTest {
        // Given: 使用真实实现（但可以替换数据源）
        val storage = InMemoryKeyValueStorage()  // 测试用的内存存储
        val localDataSource = UserLocalDataSourceImpl(storage)
        val remoteDataSource = FakeUserRemoteDataSource()  // 测试用的假数据源
        val repository = UserRepositoryImpl(remoteDataSource, localDataSource)
        val createUseCase = CreateUserUseCase(repository)
        val getUserUseCase = GetUserUseCase(repository)
        
        // When: 创建用户
        val createResult = createUseCase("Test User", "test@test.com")
        assertTrue(createResult.isSuccess)
        val user = createResult.getOrNull()!!
        
        // Then: 可以获取用户
        val retrievedUser = getUserUseCase(user.id)
        assertEquals(user, retrievedUser)
    }
}
```

---

## 代码组织

### 推荐的包结构

```
shared/src/commonMain/kotlin/com/tcm/heartsave/
├── domain/                  # 领域层
│   ├── model/              # 领域模型
│   ├── repository/         # Repository 接口
│   ├── usecase/            # UseCase
│   └── exception/          # 领域异常
├── data/                   # 数据层
│   ├── repository/         # Repository 实现
│   ├── datasource/         # 数据源
│   │   ├── remote/         # 远程数据源
│   │   ├── local/          # 本地数据源
│   │   └── cache/          # 缓存数据源
│   ├── dto/                # DTO
│   └── mapper/             # 映射器
├── platform/               # 平台层（接口定义）
│   ├── network/            # 网络接口
│   ├── storage/            # 存储接口
│   └── file/               # 文件接口
└── di/                     # 依赖注入
    └── AppContainer.kt

shared/src/{platform}Main/kotlin/com/tcm/heartsave/platform/
├── network/                # 网络实现
├── storage/                # 存储实现
└── file/                   # 文件实现

composeApp/src/commonMain/kotlin/com/tcm/heartsave/
├── ui/                     # UI 组件
│   ├── components/         # 可复用组件
│   ├── screens/            # 屏幕
│   └── theme/              # 主题
├── viewmodel/              # ViewModel
└── navigation/             # 导航
```

---

## 最佳实践

### ✅ 应该做的

1. **使用接口抽象**
   ```kotlin
   // ✅ 好的做法
   interface UserRepository { }
   class UserRepositoryImpl : UserRepository { }
   ```

2. **通过构造函数注入依赖**
   ```kotlin
   // ✅ 好的做法
   class GetUserUseCase(private val repository: UserRepository)
   ```

3. **使用 Result 类型处理错误**
   ```kotlin
   // ✅ 好的做法
   suspend fun getUser(id: String): Result<User>
   ```

4. **分离关注点**
   - UI 只负责展示
   - UseCase 只负责业务逻辑
   - Repository 只负责数据访问

5. **编写测试**
   - 每层都要有测试
   - 使用 Mock 对象隔离依赖

### ❌ 不应该做的

1. **不要直接依赖具体实现**
   ```kotlin
   // ❌ 避免
   class GetUserUseCase(private val repository: UserRepositoryImpl)
   ```

2. **不要在 Domain Layer 依赖 Data Layer**
   ```kotlin
   // ❌ 避免
   // domain/usecase/GetUserUseCase.kt
   import com.tcm.heartsave.data.repository.UserRepositoryImpl
   ```

3. **不要在 UI 层写业务逻辑**
   ```kotlin
   // ❌ 避免
   @Composable
   fun HomeScreen() {
       val user = remember {
           // 业务逻辑不应该在这里
           if (condition) { ... } else { ... }
       }
   }
   ```

4. **不要使用全局变量或单例**
   ```kotlin
   // ❌ 避免
   object UserRepositorySingleton {
       fun getUser() { }
   }
   ```

5. **不要忽略错误处理**
   ```kotlin
   // ❌ 避免
   suspend fun getUser(id: String): User {
       return repository.getUser(id)  // 可能抛出异常
   }
   
   // ✅ 好的做法
   suspend fun getUser(id: String): Result<User> {
       return repository.getUser(id)
   }
   ```

---

## 总结

### 架构优势

1. **低耦合**：通过接口实现层间解耦
2. **可替换**：任何实现都可以被替换
3. **高测试性**：每层都可以独立测试
4. **可维护性**：清晰的职责划分
5. **可扩展性**：易于添加新功能

### 关键要点

- ✅ **依赖倒置**：Domain Layer 定义接口，Data Layer 实现
- ✅ **单向依赖**：上层依赖下层，通过接口
- ✅ **构造函数注入**：所有依赖通过构造函数注入
- ✅ **测试驱动**：每层都要有测试

---

*最后更新：2024年*
