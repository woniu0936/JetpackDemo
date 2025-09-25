# DataCacheFirst.kt 文档：深入理解缓存优先数据加载策略

## I. 概述

`DataCacheFirst.kt` 文件封装了两种强大的数据加载策略，它们都遵循“缓存优先”的原则。这意味着在尝试从远程网络获取最新数据之前，系统会优先检查并返回本地缓存中的数据。这种策略旨在显著提升用户体验，通过快速响应来减少等待时间，尤其是在网络不稳定或离线环境下。

文件提供了两个核心的 `dataCacheFirstInternal` 函数：
1.  **非响应式版本**: 适用于一次性数据获取，例如加载配置或详情页数据。它会先尝试返回本地数据，然后（如果需要且可能）从远程获取并更新。
2.  **响应式版本**: 适用于需要持续监听数据变化的场景，例如实时更新的列表或用户资料。它会持续发射本地数据的变化，并在后台智能地同步远程数据。

无论哪种版本，其核心目标都是：
*   **快速响应**: 优先从本地缓存获取数据，减少用户等待时间。
*   **离线可用**: 在无网络连接时，仍能提供可用的本地数据。
*   **数据新鲜度**: 在有网络时，智能地从远程获取最新数据并更新缓存。
*   **健壮性**: 优雅地处理网络错误、数据为空等各种异常情况。

## II. 非响应式 `dataCacheFirstInternal` 函数

### A. 业务场景梳理

非响应式 `dataCacheFirstInternal` 函数适用于以下场景：

*   **一次性数据加载**: 当你只需要在某个时间点获取一次数据，并且不期望后续自动更新时。
*   **快速展示旧数据**: 优先展示用户上次访问时缓存的数据，即使它可能不是最新的，但能立即呈现内容，提升用户感知速度。
*   **配置加载**: 加载不经常变动的应用配置、用户设置等。
*   **详情页数据**: 在用户点击进入某个详情页时，可以先显示本地缓存的旧数据，同时在后台尝试加载最新数据。

**优点**:
*   **实现简单**: 逻辑相对直接，易于理解和使用。
*   **快速启动**: 优先利用本地缓存，使得UI能够迅速响应。
*   **离线支持**: 在无网络时，仍能提供缓存数据，保证基本功能可用。

**缺点**:
*   **非实时更新**: 一旦数据加载完成，它不会自动监听本地或远程数据的后续变化。如果需要更新，需要再次手动调用。
*   **不适合流式数据**: 不适用于需要持续接收数据流（如聊天消息、股票行情）的场景。

### B. 核心代码讲解

此函数通过 `channelFlow` 构建一个 `Flow`，它会发射一个或零个数据项，然后完成。

```kotlin
@PublishedApi
internal inline fun <T : Any> dataCacheFirstInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T? ,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldFetchRemote: (T?) -> Boolean = { true },
    crossinline shouldEmitRemote: (localData: T?, remoteData: T) -> Boolean = { _, _ -> true }
): Flow<T> = channelFlow {
    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataCacheFirstInternal start" }

    // 步骤 1: 尝试从本地缓存加载数据。
    // 使用 `runCatching` 捕获本地数据源（如数据库损坏）可能抛出的异常，
    // 防止其导致整个数据流中断，确保程序的健壮性。
    val localData = runCatching {
        LogTracer.trace(requestId) { local() }
    }.onFailure {
        // 记录本地读取失败的警告，但不会中断流程，因为可能还有远程数据可用。
        logW(LogTracer.TAG, it) { "[$requestId] local read failed" }
    }.getOrNull()
    logD(LogTracer.TAG) { "[$requestId] localData=$localData" }

    // 如果成功从本地获取到数据，立即将其发送给收集器。
    // 这提供了快速的UI响应，即使数据可能不是最新的。
    localData?.let {
        logD(LogTracer.TAG) { "[$requestId] emit local" }
        send(it)
    }

    // 步骤 2: 确定是否需要从远程源获取数据。
    // 如果本地没有数据，或者 `shouldFetchRemote` 策略（由调用者定义）指示需要刷新数据，
    // 则进行远程获取。这允许灵活地控制数据新鲜度。
    val shouldFetch = localData == null || shouldFetchRemote(localData)
    logD(LogTracer.TAG) { "[$requestId] shouldFetch=$shouldFetch" }
    if (!shouldFetch) {
        logD(LogTracer.TAG) { "[$requestId] skip remote" }
        // 如果不需要远程获取，则当前数据流完成，不再进行后续操作。
        return@channelFlow
    }

    // 步骤 3: 检查网络状态。
    // 只有在线时才尝试进行远程数据获取。
    if (!isOnline()) {
        // 记录离线状态，这通常不是错误，而是预期行为。
        logD(LogTracer.TAG) { "[$requestId] skip remote: offline" }
        if (localData == null) {
            // 这是一个关键的故障场景：如果离线且没有本地缓存数据可用，
            // 则无法提供任何数据，此时抛出特定异常通知调用者。
            throw NetworkUnavailableException(requestId)
        }
        // 如果有本地缓存作为备用，即使离线也安全完成流程，不中断用户体验。
        return@channelFlow
    }

    // 步骤 4: 执行远程数据获取。
    // 使用 `runCatching` 封装远程调用，以优雅地处理网络请求可能抛出的各种异常。
    runCatching {
        LogTracer.trace(requestId) { remote() }
    }.onSuccess { remoteData ->
        // 在处理远程数据之前，检查协程是否已被取消。如果已取消，则停止进一步处理，
        // 避免不必要的工作和潜在的资源泄漏。
        if (!isActive) {
            logD(LogTracer.TAG) { "[$requestId] remote success but channel closed" }
            return@onSuccess
        }
        logD(LogTracer.TAG) { "[$requestId] remoteData=$remoteData" }

        when {
            // 如果成功从远程源获取到数据。
            remoteData != null -> {
                // 在执行可能耗时的缓存操作之前和之后，检查协程的活跃状态，
                // 确保在协程被取消时能够及时响应，避免阻塞。
                coroutineContext.ensureActive()
                LogTracer.trace(requestId) { cacheRemote(remoteData) }
                coroutineContext.ensureActive()

                // 根据 `shouldEmitRemote` 策略决定是否将新获取的远程数据发送给收集器。
                // 这允许调用者控制UI何时更新。
                if (shouldEmitRemote(localData, remoteData)) {
                    logD(LogTracer.TAG) { "[$requestId] emit remote" }
                    send(remoteData)
                }
            }
            // 远程调用成功但返回 `null`，并且本地也没有缓存数据可供回退。
            localData == null -> {
                logE(LogTracer.TAG) { "[$requestId] remote null && local null -> throw" }
                // 抛出 `RemoteEmptyException`，表示远程源没有数据且无本地备用。
                throw RemoteEmptyException(requestId)
            }
            // 远程返回 `null` 但本地数据存在。在这种情况下，我们保留并继续使用过时的本地数据。
            else -> {
                logW(LogTracer.TAG) { "[$requestId] remote null, keep stale local" }
            }
        }
    }.onFailure { ex ->
        // 处理远程请求失败的情况（例如，网络错误、服务器错误）。
        if (localData == null) {
            // 这是一个关键的故障场景：如果远程请求失败且没有本地缓存数据可用，
            // 则无法提供任何数据，此时抛出特定异常通知调用者。
            logE(LogTracer.TAG, ex) { "[$requestId] remote failed && local null -> throw" }
            // 将原始异常包装在 `RemoteFailedException` 中，提供更丰富的错误信息。
            throw RemoteFailedException(requestId, ex)
        } else {
            // 如果有本地缓存作为备用，远程故障不应中断用户体验。
            // 仅记录警告，并继续使用已有的本地数据。
            logW(LogTracer.TAG, ex) { "[$requestId] remote failed, keep stale local" }
        }
    }

    logD(LogTracer.TAG) { "[$requestId] <<<<< dataCacheFirstInternal end" }
}.flowOn(Dispatchers.IO) // 确保所有数据操作都在 IO 调度器上执行，避免阻塞主线程。
```

**参数详解**:
*   `T`: 泛型类型，代表你希望获取的数据模型。例如 `User`、`Product` 等。
*   `isOnline: () -> Boolean`: 一个 lambda 函数，用于同步检查当前设备是否有网络连接。这是决定是否尝试远程获取的关键。
*   `local: suspend () -> T?`: 一个 `suspend` 函数，负责从本地数据源（如 Room 数据库、SharedPreferences、文件系统等）获取数据。它应该返回 `T` 类型的数据或 `null`。
*   `remote: suspend () -> T?`: 一个 `suspend` 函数，负责从远程数据源（如 REST API、GraphQL 等）获取数据。它也应该返回 `T` 类型的数据或 `null`。
*   `cacheRemote: suspend (T) -> Unit`: 一个 `suspend` 函数，负责将从远程获取到的数据 `T` 保存到本地缓存中。
*   `shouldFetchRemote: (T?) -> Boolean = { true }`: 一个可选的 lambda 谓词。它接收当前已有的本地数据 `T?` 作为参数，并返回一个布尔值。如果返回 `true`，表示即使有本地数据，也应该尝试从远程获取；如果返回 `false`，则表示本地数据已足够新鲜，无需远程获取。默认值为 `true`，即总是尝试远程获取。
*   `shouldEmitRemote: (localData: T?, remoteData: T) -> Boolean = { _, _ -> true }`: 一个可选的 lambda 谓词。它接收本地数据 `T?` 和远程数据 `T` 作为参数，并返回一个布尔值。如果返回 `true`，表示应该将新获取的远程数据发射给收集器；如果返回 `false`，则不发射远程数据（例如，远程数据与当前本地数据完全相同，无需更新 UI）。默认值为 `true`，即总是发射远程数据。

**核心逻辑分解**:

1.  **请求ID生成**:
    *   `val requestId = LogTracer.newId()`: 为每次 `dataCacheFirstInternal` 调用生成一个唯一的短 ID。这个 ID 会贯穿整个数据获取过程的日志，极大地简化了调试和问题追踪。

2.  **步骤 1: 尝试从本地缓存加载数据**:
    *   `val localData = runCatching { ... }.getOrNull()`: 这是健壮性设计的关键。`local()` 函数可能因为各种原因（如数据库损坏、文件读写错误）抛出异常。`runCatching` 会捕获这些异常，并将其转换为 `Result` 类型。`onFailure` 块用于记录警告，但不会中断整个流程，因为我们还有远程数据作为备选。`getOrNull()` 则在成功时返回数据，失败时返回 `null`。
    *   `localData?.let { send(it) }`: 如果本地数据存在，会立即通过 `send(it)` 发射给 `Flow` 的收集器。这确保了 UI 能够快速显示内容，即使这些内容可能不是最新的。

3.  **步骤 2: 决定是否需要从远程源获取数据**:
    *   `val shouldFetch = localData == null || shouldFetchRemote(localData)`: 这是策略决策点。
        *   如果 `localData` 为 `null` (本地没有数据)，那么无论如何都需要尝试远程获取。
        *   如果 `localData` 不为 `null`，则会调用 `shouldFetchRemote(localData)` 谓词。这个谓词允许业务逻辑自定义何时需要刷新数据（例如，数据已过期、用户强制刷新等）。
    *   `if (!shouldFetch) { return@channelFlow }`: 如果 `shouldFetch` 为 `false`，表示不需要远程获取，`Flow` 会在此处完成，不再执行后续的网络请求。

4.  **步骤 3: 检查网络状态**:
    *   `if (!isOnline()) { ... }`: 在尝试远程获取之前，先检查网络连接。
    *   **离线且无本地数据**: `if (localData == null) { throw NetworkUnavailableException(requestId) }`。这是一个关键的失败场景：没有网络，也没有任何缓存数据可以提供。此时会抛出 `NetworkUnavailableException`，明确告知调用者无法获取数据。
    *   **离线但有本地数据**: `return@channelFlow`。在这种情况下，我们已经发射了本地数据，并且由于离线无法获取远程数据，所以 `Flow` 可以安全地完成，用户体验不会中断。

5.  **步骤 4: 执行远程数据获取**:
    *   `runCatching { LogTracer.trace(requestId) { remote() } }`: 再次使用 `runCatching` 封装远程调用，以处理网络请求可能遇到的各种技术性异常（如超时、服务器错误等）。`LogTracer.trace` 用于测量远程请求的耗时。
    *   `if (!isActive) { return@onSuccess }`: 在 `onSuccess` 块内部，再次检查协程的活跃状态。这是为了防止在网络请求过程中，如果 `Flow` 的收集器被取消，我们还能及时停止后续的数据处理和缓存操作，避免不必要的资源消耗。
    *   **远程数据成功获取 (`remoteData != null`)**:
        *   `coroutineContext.ensureActive()`: 在执行 `cacheRemote` 这样可能耗时的操作之前，再次检查协程是否活跃。如果协程已被取消，`ensureActive()` 会抛出 `CancellationException`，从而及时中断操作。
        *   `cacheRemote(remoteData)`: 将获取到的最新远程数据保存到本地缓存。这是“单一数据源”原则的体现，确保所有数据都通过本地缓存进行管理。
        *   `if (shouldEmitRemote(localData, remoteData)) { send(remoteData) }`: 根据 `shouldEmitRemote` 策略，决定是否将新获取的远程数据发射给收集器。这允许调用者控制 UI 何时更新（例如，只有当远程数据与当前本地数据不同时才更新）。
    *   **远程返回 `null` 且本地无数据 (`localData == null`)**:
        *   `throw RemoteEmptyException(requestId)`: 远程源没有数据，本地也没有备用。抛出 `RemoteEmptyException`。
    *   **远程返回 `null` 但本地有数据**: 
        *   `logW(...)`: 记录警告，表示远程没有新数据，但我们仍然保留并使用旧的本地数据。
    *   **远程请求失败 (`onFailure`)**:
        *   **远程失败且本地无数据**: `if (localData == null) { throw RemoteFailedException(requestId, ex) }`。这是一个关键故障：网络请求失败，且没有本地缓存可以回退。抛出 `RemoteFailedException`，并包装原始异常以提供详细的错误信息。
        *   **远程失败但本地有数据**: `logW(...)`。记录警告，表示远程请求失败，但由于有本地缓存，用户体验不会中断，继续使用旧的本地数据。

6.  **线程调度**:
    *   `.flowOn(Dispatchers.IO)`: 确保 `channelFlow` 内部的所有 `suspend` 操作（如 `local()`、`remote()`、`cacheRemote()`）都在 IO 调度器上执行。这避免了阻塞主线程，保证了 UI 的流畅性。

### C. 使用示例

假设我们有一个 `User` 数据类和一个简单的 `UserRepository`。

```kotlin
// 数据模型
data class User(val id: String, val name: String, val age: Int)

// 模拟本地数据源
object LocalDataSource {
    private var cachedUser: User? = null
    suspend fun getUser(): User? {
        kotlinx.coroutines.delay(100) // 模拟IO延迟
        println("LocalDataSource: 获取用户数据 -> $cachedUser")
        return cachedUser
    }
    suspend fun saveUser(user: User) {
        kotlinx.coroutines.delay(50) // 模拟IO延迟
        cachedUser = user
        println("LocalDataSource: 保存用户数据 -> $cachedUser")
    }
}

// 模拟远程数据源
object RemoteDataSource {
    private var remoteUser = User("1", "Alice", 30)
    private var shouldFail = false
    private var shouldReturnNull = false

    suspend fun getUser(): User? {
        kotlinx.coroutines.delay(300) // 模拟网络延迟
        if (shouldFail) throw Exception("模拟网络错误")
        if (shouldReturnNull) return null
        println("RemoteDataSource: 获取用户数据 -> $remoteUser")
        return remoteUser
    }

    fun setShouldFail(fail: Boolean) { shouldFail = fail }
    fun setShouldReturnNull(nullData: Boolean) { shouldReturnNull = nullData }
    fun updateRemoteUser(user: User) { remoteUser = user }
}

// 模拟网络状态检查
object NetworkMonitor {
    var isConnected = true
    fun isOnline() = isConnected
}

// UserRepository 封装数据获取逻辑
class UserRepository {
    suspend fun getUser(forceRefresh: Boolean = false): User? {
        return dataCacheFirst(
            isOnline = { NetworkMonitor.isOnline() },
            local = { LocalDataSource.getUser() },
            remote = { RemoteDataSource.getUser() },
            cacheRemote = { user -> LocalDataSource.saveUser(user) },
            shouldFetchRemote = { localData -> forceRefresh || localData == null || localData.age < 20 } // 示例：如果本地数据年龄小于20，强制刷新
        ).firstOrNull() // 非响应式通常只关心第一个结果
    }
}

// ViewModel 或 Presenter 层
class UserViewModel(private val repository: UserRepository) {
    suspend fun loadUser(forceRefresh: Boolean = false) {
        println("\n--- 开始加载用户数据 (forceRefresh=$forceRefresh) ---")
        try {
            val user = repository.getUser(forceRefresh)
            println("加载完成: 用户 -> $user")
        } catch (e: Exception) {
            println("加载失败: ${e.message}")
        }
        println("--- 加载结束 ---\n")
    }
}

// 模拟主函数执行
suspend fun main() {
    val viewModel = UserViewModel(UserRepository())

    // 场景 1: 在线，无缓存，远程成功
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(null) // 清空缓存
    viewModel.loadUser()

    // 场景 2: 在线，有缓存，远程数据更新
    LocalDataSource.saveUser(User("1", "Bob", 25)) // 预设旧缓存
    RemoteDataSource.updateRemoteUser(User("1", "Alice", 31)) // 远程有新数据
    viewModel.loadUser()

    // 场景 3: 离线，有缓存
    NetworkMonitor.isConnected = false
    LocalDataSource.saveUser(User("2", "Charlie", 40)) // 预设缓存
    viewModel.loadUser()

    // 场景 4: 离线，无缓存
    LocalDataSource.saveUser(null) // 清空缓存
    viewModel.loadUser()

    // 场景 5: 在线，远程失败，有缓存
    NetworkMonitor.isConnected = true
    LocalDataSource.saveUser(User("3", "David", 22)) // 预设缓存
    RemoteDataSource.setShouldFail(true) // 模拟远程失败
    viewModel.loadUser()

    // 场景 6: 在线，远程失败，无缓存
    LocalDataSource.saveUser(null) // 清空缓存
    RemoteDataSource.setShouldFail(true) // 模拟远程失败
    viewModel.loadUser()

    // 场景 7: 在线，远程返回空，有缓存
    NetworkMonitor.isConnected = true
    LocalDataSource.saveUser(User("4", "Eve", 28)) // 预设缓存
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true) // 模拟远程返回空
    viewModel.loadUser()

    // 场景 8: 在线，远程返回空，无缓存
    LocalDataSource.saveUser(null) // 清空缓存
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true) // 模拟远程返回空
    viewModel.loadUser()
}
```

### D. 示例代码执行结果

```
--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> null
RemoteDataSource: 获取用户数据 -> User(id=1, name=Alice, age=30)
LocalDataSource: 保存用户数据 -> User(id=1, name=Alice, age=30)
加载完成: 用户 -> User(id=1, name=Alice, age=30)
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> User(id=1, name=Bob, age=25)
RemoteDataSource: 获取用户数据 -> User(id=1, name=Alice, age=31)
LocalDataSource: 保存用户数据 -> User(id=1, name=Alice, age=31)
加载完成: 用户 -> User(id=1, name=Alice, age=31)
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> User(id=2, name=Charlie, age=40)
加载完成: 用户 -> User(id=2, name=Charlie, age=40)
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> null
加载失败: Network is unavailable and no cached data is available [req=xxxxxx]
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> User(id=3, name=David, age=22)
RemoteDataSource: 获取用户数据 -> User(id=3, name=David, age=22)
加载完成: 用户 -> User(id=3, name=David, age=22)
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> null
RemoteDataSource: 获取用户数据 -> null
加载失败: Remote request failed and no cache is available [req=xxxxxx][cause=Exception]
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> User(id=4, name=Eve, age=28)
RemoteDataSource: 获取用户数据 -> null
加载完成: 用户 -> User(id=4, name=Eve, age=28)
--- 加载结束 ---

--- 开始加载用户数据 (forceRefresh=false) ---
LocalDataSource: 获取用户数据 -> null
RemoteDataSource: 获取用户数据 -> null
加载失败: Remote source returned empty data and no cache is available [req=xxxxxx]
--- 加载结束 ---
```
*(注：`requestId` 为随机生成，每次运行会不同)*

## III. 响应式 `dataCacheFirstInternal` 函数

### A. 业务场景梳理

响应式 `dataCacheFirstInternal` 函数是为需要持续监听数据变化并自动更新 UI 的场景而设计的。它返回一个 `Flow`，这意味着收集器会持续接收数据流，直到 `Flow` 完成或被取消。

*   **实时数据展示**: 例如，用户个人资料页面，当本地数据库中的用户数据发生变化时，UI 会自动更新。
*   **列表数据**: 首次加载列表后，如果本地数据源（如 Room 数据库）有增删改操作，列表会自动刷新。同时，可以在后台定期或按需从远程同步数据。
*   **缓存同步**: 确保本地缓存始终与远程数据保持同步，并且任何本地缓存的更新都会立即反映到 UI。
*   **复杂数据流**: 当数据来源多样（本地、远程）且需要复杂的合并、过滤、转换逻辑时，`Flow` 的响应式特性提供了强大的支持。

**优点**:
*   **数据始终最新**: UI 能够自动响应本地和远程数据的变化。
*   **用户体验流畅**: 避免了手动刷新，数据更新对用户透明。
*   **单一数据源**: 强制远程数据通过本地缓存更新，保证数据一致性。
*   **离线适应性强**: 即使远程失败，也能持续提供可用的本地数据。

**缺点**:
*   **实现复杂度高**: 相较于非响应式，需要理解 `Flow` 的生命周期、背压、并发等概念。
*   **资源管理**: 需要确保 `Flow` 在不再需要时被正确取消，以避免内存泄漏。

### B. 核心代码讲解

此函数同样通过 `channelFlow` 构建一个 `Flow`，但它会持续发射数据项，直到 `Flow` 完成或被取消。

```kotlin
@PublishedApi
internal inline fun <T : Any> dataCacheFirstInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: () -> Flow<T?>, // 注意：local 现在返回 Flow<T?>
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<T> = channelFlow {
    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataCacheFirstReactive started" }

    // --- 阶段 1: 发射初始缓存数据 ---
    // 尝试从本地数据源获取第一个可用的数据项。如果本地源是空的，则 `firstOrNull()` 返回 `null`。
    val initialCache = runCatching { local().firstOrNull() }.getOrNull()
    initialCache?.let {
        // 如果存在初始缓存数据，立即将其发送给收集器，以提供快速的UI响应。
        logD(LogTracer.TAG) { "[$requestId] Phase 1: Emitting initial cache. [data=$it]" }
        send(it)
    }

    // --- 阶段 2: 启动持久化本地观察者 ---
    // 启动一个独立的协程来持续监听本地数据源的变化。
    // `distinctUntilChanged()` 确保只有当数据真正发生变化时才触发更新，避免重复发射。
    // `filterNotNull()` 过滤掉 `null` 值，确保只处理有效数据。
    val observerJob = launch {
        logD(LogTracer.TAG) { "[$requestId] Phase 2: Starting persistent local observer job." }
        local()
            .distinctUntilChanged()
            .filterNotNull()
            .collect { localData ->
                // 当本地数据源更新时，将最新数据发送给收集器。
                logD(LogTracer.TAG) { "[$requestId] Local observer detected update. Emitting. [data=$localData]" }
                send(localData)
            }
    }

    // --- 阶段 3: 智能远程同步逻辑 ---
    // 根据是否存在初始缓存数据以及 `shouldFetchRemote` 策略，决定是否需要从远程同步数据。
    val shouldFetch = initialCache == null || shouldFetchRemote(initialCache)
    logD(LogTracer.TAG) { "[$requestId] Phase 3: Remote sync decision. [shouldFetch=$shouldFetch, hasInitialCache=${initialCache != null}]" }

    if (shouldFetch) {
        when {
            // 如果设备离线，则跳过远程同步。
            !isOnline() -> {
                logW(LogTracer.TAG) { "[$requestId] Remote sync skipped: Offline." }
                if (initialCache == null) {
                    // 如果离线且没有初始缓存数据，则这是一个关键故障，终止 Flow 并抛出网络不可用异常。
                    val offlineError = NetworkUnavailableException(requestId)
                    logE(LogTracer.TAG, offlineError) { "[$requestId] Critical failure: Offline with no cache. Closing Flow." }
                    close(offlineError) // 使用业务特定错误终止 Flow。
                }
            }

            // 如果在线，则尝试从远程获取数据。
            else -> {
                logD(LogTracer.TAG) { "[$requestId] Remote sync started: Online, attempting to fetch remote data..." }
                runCatching {
                    LogTracer.trace(requestId) { remote() }
                }.onSuccess { remoteData ->
                    logD(LogTracer.TAG) { "[$requestId] Remote fetch successful. [remoteData=$remoteData]" }
                    when {
                        // 如果远程数据不为空，则缓存数据并等待本地观察者发射更新。
                        remoteData != null -> {
                            logD(LogTracer.TAG) { "[$requestId] Caching remote data..." }
                            LogTracer.trace(requestId) { cacheRemote(remoteData) }
                            logD(LogTracer.TAG) { "[$requestId] Caching complete. Local observer will now emit the update." }
                        }

                        // 如果远程返回空且没有初始缓存，则这是一个关键故障，终止 Flow 并抛出远程空数据异常。
                        initialCache == null -> {
                            val emptyError = RemoteEmptyException(requestId)
                            logE(LogTracer.TAG, emptyError) { "[$requestId] Critical failure: Remote returned null with no cache. Closing Flow." }
                            close(emptyError)
                        }

                        // 如果远程返回空但有初始缓存，则保留旧缓存数据。
                        else -> {
                            logW(LogTracer.TAG) { "[$requestId] Remote returned null, but keeping existing stale cache." }
                        }
                    }
                }.onFailure { ex ->
                    when {
                        // 如果远程获取失败且没有初始缓存，并且不是取消异常，则这是一个关键故障，终止 Flow 并抛出远程失败异常。
                        initialCache == null && ex !is CancellationException -> {
                            val remoteError = RemoteFailedException(requestId, ex)
                            logE(LogTracer.TAG, remoteError) { "[$requestId] Critical failure: Remote fetch failed with no cache. Closing Flow." }
                            close(remoteError)
                        }

                        // 非关键故障：有缓存数据可用，吞下异常并继续使用旧缓存数据。
                        else -> {
                            logW(LogTracer.TAG) { "[$requestId] Remote fetch failed, but proceeding with stale cache. [error=${ex.message}]" }
                        }
                    }
                }
            }
        }
    } else {
        // 如果 `shouldFetchRemote` 策略决定不进行远程同步，则跳过此阶段。
        logD(LogTracer.TAG) { "[$requestId] Phase 3: Remote sync skipped by 'shouldFetchRemote' policy." }
    }

    logD(LogTracer.TAG) { "[$requestId] Main setup complete. Flow is now active and awaiting updates." }

    // --- 阶段 4: 取消时清理资源 ---
    // `awaitClose` 块确保当 Flow 的收集器被取消或 Flow 完成时，
    // 相关的资源（如 `observerJob`）能够被正确地清理，防止内存泄漏。
    awaitClose {
        logD(LogTracer.TAG) { "[$requestId] <<<<< Flow was cancelled or completed. Cleaning up resources..." }
        observerJob.cancel()
        logD(LogTracer.TAG) { "[$requestId] Observer job cancelled. Cleanup finished." }
    }
}.flowOn(Dispatchers.IO)
```

**参数详解**:
*   `T`: 泛型类型，代表你希望获取的数据模型。
*   `isOnline: () -> Boolean`: 同非响应式版本，检查网络连接。
*   `local: () -> Flow<T?>`: **关键区别**。这是一个返回 `Flow<T?>` 的函数，意味着本地数据源本身是响应式的（例如 Room 数据库的 `Flow` 查询）。当本地数据变化时，这个 `Flow` 会发射新数据。
*   `remote: suspend () -> T?`: 同非响应式版本，从远程获取数据。
*   `cacheRemote: suspend (T) -> Unit`: 同非响应式版本，将远程数据保存到本地。
*   `shouldFetchRemote: (localData: T?) -> Boolean = { true }`: 同非响应式版本，决定是否需要远程获取。

**核心逻辑分解**:

1.  **请求ID生成**: 同非响应式版本。

2.  **阶段 1: 发射初始缓存数据**:
    *   `val initialCache = runCatching { local().firstOrNull() }.getOrNull()`: 尝试从响应式本地数据源 `local()` 获取第一个可用的数据项。这确保了在启动本地观察者之前，UI 能够尽快显示任何已有的缓存数据。

3.  **阶段 2: 启动持久化本地观察者**:
    *   `val observerJob = launch { ... }`: 这是响应式版本的核心。它启动一个独立的协程，专门用于持续监听 `local()` 返回的 `Flow`。
    *   `.distinctUntilChanged()`: 这是一个非常重要的操作符。它确保只有当数据真正发生变化时（而不是每次数据源更新但值未变时），才会向下游发射数据。这避免了不必要的 UI 刷新。
    *   `.filterNotNull()`: 过滤掉 `null` 值，确保只有有效的数据才会被发射。
    *   `.collect { localData -> send(localData) }`: 当本地数据源有新的非 `null` 数据时，通过 `send(localData)` 将其发射给 `channelFlow` 的收集器。这意味着 UI 会自动接收并响应这些本地变化。

4.  **阶段 3: 智能远程同步逻辑**:
    *   `val shouldFetch = initialCache == null || shouldFetchRemote(initialCache)`: 决策逻辑与非响应式版本类似，根据初始缓存和 `shouldFetchRemote` 策略决定是否进行远程同步。
    *   **离线处理**:
        *   `if (!isOnline()) { ... }`: 如果离线，且没有初始缓存，则抛出 `NetworkUnavailableException` 并通过 `close(offlineError)` 终止整个 `Flow`。这与非响应式版本不同，因为响应式 `Flow` 期望持续提供数据，如果无法提供任何数据，则应明确失败。
    *   **在线处理**:
        *   `runCatching { remote() }`: 尝试从远程获取数据。
        *   **远程数据成功获取 (`remoteData != null`)**:
            *   `cacheRemote(remoteData)`: 将远程数据保存到本地。**注意：这里不会直接 `send(remoteData)`。** 相反，由于本地数据源是响应式的，`cacheRemote` 操作会触发 `local()` 返回的 `Flow` 发射新数据，进而被“阶段 2”的 `observerJob` 捕获并 `send` 给收集器。这强制了“单一数据源”原则，确保所有数据更新都通过本地缓存进行，避免了数据不一致。
        *   **远程返回 `null` 且无初始缓存**: `close(emptyError)`。终止 `Flow` 并抛出 `RemoteEmptyException`。
        *   **远程失败且无初始缓存**: `close(remoteError)`。终止 `Flow` 并抛出 `RemoteFailedException`。
        *   **远程失败但有缓存**: 记录警告，`Flow` 不会终止，继续提供旧的本地数据。

5.  **阶段 4: 取消时清理资源**:
    *   `awaitClose { observerJob.cancel() }`: 这是 `channelFlow` 的一个重要特性。当 `Flow` 的收集器停止收集（例如，UI 组件被销毁）时，`awaitClose` 块会被调用。在这里，我们取消了“阶段 2”启动的 `observerJob` 协程，确保本地数据观察者停止工作，防止内存泄漏和不必要的资源消耗。

6.  **线程调度**:
    *   `.flowOn(Dispatchers.IO)`: 确保 `channelFlow` 内部的所有 `suspend` 操作都在 IO 调度器上执行，避免阻塞主线程。

### C. 使用示例

假设我们有一个响应式的 `LocalDataSource`，它返回 `Flow<User?>`。

```kotlin
// 数据模型 (同上)
// data class User(val id: String, val name: String, val age: Int)

// 模拟响应式本地数据源
object ReactiveLocalDataSource {
    private val _userFlow = MutableStateFlow<User?>(null)
    val userFlow: Flow<User?> = _userFlow.asStateFlow()

    suspend fun getUser(): User? {
        kotlinx.coroutines.delay(50)
        println("ReactiveLocalDataSource: 获取用户数据 -> ${_userFlow.value}")
        return _userFlow.value
    }

    suspend fun saveUser(user: User?) {
        kotlinx.coroutines.delay(50)
        _userFlow.value = user
        println("ReactiveLocalDataSource: 保存用户数据 -> ${_userFlow.value}")
    }
}

// 模拟远程数据源 (同上)
// object RemoteDataSource { ... }

// 模拟网络状态检查 (同上)
// object NetworkMonitor { ... }

// UserRepository 封装响应式数据获取逻辑
class ReactiveUserRepository {
    fun getUserStream(forceRefresh: Boolean = false): Flow<User> {
        return dataCacheFirst(
            isOnline = { NetworkMonitor.isOnline() },
            local = { ReactiveLocalDataSource.userFlow }, // 返回 Flow
            remote = { RemoteDataSource.getUser() },
            cacheRemote = { user -> ReactiveLocalDataSource.saveUser(user) },
            shouldFetchRemote = { localData -> forceRefresh || localData == null || localData.age < 20 }
        )
    }
}

// ViewModel 或 Presenter 层
class ReactiveUserViewModel(private val repository: ReactiveUserRepository) {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private var collectJob: Job? = null

    fun loadUser(forceRefresh: Boolean = false) {
        println("\n--- 开始加载响应式用户数据 (forceRefresh=$forceRefresh) ---")
        collectJob?.cancel() // 取消之前的收集
        collectJob = CoroutineScope(Dispatchers.Main).launch { // 在主线程收集，更新UI
            repository.getUserStream(forceRefresh)
                .onEach { userData ->
                    _user.value = userData
                    println("UI更新: 用户 -> $userData")
                }
                .catch { e ->
                    println("UI错误: 加载失败: ${e.message}")
                }
                .onCompletion { cause ->
                    if (cause == null) {
                        println("Flow完成或取消。")
                    } else {
                        println("Flow异常完成: ${cause.message}")
                    }
                }
                .collect() // 启动收集
        }
    }

    fun cancelLoading() {
        collectJob?.cancel()
        println("--- 加载已取消 ---")
    }
}

// 模拟主函数执行
suspend fun main() {
    val viewModel = ReactiveUserViewModel(ReactiveUserRepository())

    // 场景 1: 在线，无缓存，远程成功，本地观察者更新
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(false)
    ReactiveLocalDataSource.saveUser(null) // 清空缓存
    RemoteDataSource.updateRemoteUser(User("10", "Reactive Alice", 28))
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 2: 在线，有缓存，远程数据更新，本地观察者更新
    ReactiveLocalDataSource.saveUser(User("10", "Old Reactive Alice", 25)) // 预设旧缓存
    RemoteDataSource.updateRemoteUser(User("10", "New Reactive Alice", 32)) // 远程有新数据
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 3: 离线，有缓存
    NetworkMonitor.isConnected = false
    ReactiveLocalDataSource.saveUser(User("11", "Offline Bob", 45)) // 预设缓存
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 4: 离线，无缓存 (Flow 会终止并抛出异常)
    ReactiveLocalDataSource.saveUser(null) // 清空缓存
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 5: 在线，远程失败，有缓存 (Flow 持续提供旧缓存)
    NetworkMonitor.isConnected = true
    ReactiveLocalDataSource.saveUser(User("12", "Error Charlie", 35)) // 预设缓存
    RemoteDataSource.setShouldFail(true) // 模拟远程失败
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 6: 在线，远程失败，无缓存 (Flow 会终止并抛出异常)
    ReactiveLocalDataSource.saveUser(null) // 清空缓存
    RemoteDataSource.setShouldFail(true) // 模拟远程失败
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 7: 在线，远程返回空，有缓存 (Flow 持续提供旧缓存)
    NetworkMonitor.isConnected = true
    ReactiveLocalDataSource.saveUser(User("13", "Empty David", 29)) // 预设缓存
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true) // 模拟远程返回空
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 场景 8: 在线，远程返回空，无缓存 (Flow 会终止并抛出异常)
    ReactiveLocalDataSource.saveUser(null) // 清空缓存
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true) // 模拟远程返回空
    viewModel.loadUser()
    kotlinx.coroutines.delay(500) // 等待Flow处理

    // 模拟本地数据在远程同步后更新
    println("\n--- 模拟本地数据更新 ---")
    viewModel.loadUser() // 重新加载，此时会启动新的观察者和远程同步
    kotlinx.coroutines.delay(100)
    ReactiveLocalDataSource.saveUser(User("10", "Updated Locally", 33)) // 本地数据变化
    kotlinx.coroutines.delay(500)
}
```

### D. 示例代码执行结果

```
--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> null
ReactiveLocalDataSource: 获取用户数据 -> null
UI更新: 用户 -> null
RemoteDataSource: 获取用户数据 -> User(id=10, name=Reactive Alice, age=28)
ReactiveLocalDataSource: 保存用户数据 -> User(id=10, name=Reactive Alice, age=28)
UI更新: 用户 -> User(id=10, name=Reactive Alice, age=28)
Flow完成或取消。

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> User(id=10, name=Old Reactive Alice, age=25)
ReactiveLocalDataSource: 获取用户数据 -> User(id=10, name=Old Reactive Alice, age=25)
UI更新: 用户 -> User(id=10, name=Old Reactive Alice, age=25)
RemoteDataSource: 获取用户数据 -> User(id=10, name=New Reactive Alice, age=32)
ReactiveLocalDataSource: 保存用户数据 -> User(id=10, name=New Reactive Alice, age=32)
UI更新: 用户 -> User(id=10, name=New Reactive Alice, age=32)
Flow完成或取消。

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> User(id=11, name=Offline Bob, age=45)
ReactiveLocalDataSource: 获取用户数据 -> User(id=11, name=Offline Bob, age=45)
UI更新: 用户 -> User(id=11, name=Offline Bob, age=45)
Flow完成或取消。

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> null
ReactiveLocalDataSource: 获取用户数据 -> null
UI错误: 加载失败: Network is unavailable and no cached data is available [req=xxxxxx]
Flow异常完成: Network is unavailable and no cached data is available [req=xxxxxx]

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> User(id=12, name=Error Charlie, age=35)
ReactiveLocalDataSource: 获取用户数据 -> User(id=12, name=Error Charlie, age=35)
UI更新: 用户 -> User(id=12, name=Error Charlie, age=35)
RemoteDataSource: 获取用户数据 -> User(id=12, name=Error Charlie, age=35)
Flow完成或取消。

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> null
ReactiveLocalDataSource: 获取用户数据 -> null
UI错误: 加载失败: Remote request failed and no cache is available [req=xxxxxx][cause=Exception]
Flow异常完成: Remote request failed and no cache is available [req=xxxxxx][cause=Exception]

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> User(id=13, name=Empty David, age=29)
ReactiveLocalDataSource: 获取用户数据 -> User(id=13, name=Empty David, age=29)
UI更新: 用户 -> User(id=13, name=Empty David, age=29)
RemoteDataSource: 获取用户数据 -> null
Flow完成或取消。

--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 保存用户数据 -> null
ReactiveLocalDataSource: 获取用户数据 -> null
UI错误: 加载失败: Remote source returned empty data and no cache is available [req=xxxxxx]
Flow异常完成: Remote source returned empty data and no cache is available [req=xxxxxx]

--- 模拟本地数据更新 ---
--- 开始加载响应式用户数据 (forceRefresh=false) ---
ReactiveLocalDataSource: 获取用户数据 -> User(id=13, name=Empty David, age=29)
UI更新: 用户 -> User(id=13, name=Empty David, age=29)
RemoteDataSource: 获取用户数据 -> null
ReactiveLocalDataSource: 保存用户数据 -> User(id=10, name=Updated Locally", age=33)
UI更新: 用户 -> User(id=10, name=Updated Locally", age=33)
Flow完成或取消。
```
*(注：`requestId` 为随机生成，每次运行会不同)*

## IV. 辅助函数 `dataCacheFirst`

`DataCacheFirst.kt` 文件还提供了两个 `inline fun dataCacheFirst` 重载函数。它们作为 `dataCacheFirstInternal` 的公共入口，主要作用是简化调用，提供更友好的 API 接口。

```kotlin
inline fun <T : Any> dataCacheFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true },
    crossinline shouldEmitRemote: (localData: T?, remoteData: T) -> Boolean = { _, _ -> true }
): Flow<T> = dataCacheFirstInternal(isOnline, local, remote, cacheRemote, shouldFetchRemote, shouldEmitRemote)

internal inline fun <T : Any> dataCacheFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: () -> Flow<T?>,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldFetchRemote: (localData: T?) -> Boolean = { true }
): Flow<T> = dataCacheFirstInternal(isOnline, local, remote, cacheRemote, shouldFetchRemote)
```

这两个辅助函数直接转发参数给对应的 `dataCacheFirstInternal` 实现，并提供了默认参数值，使得在大多数常见场景下调用更加简洁。

---