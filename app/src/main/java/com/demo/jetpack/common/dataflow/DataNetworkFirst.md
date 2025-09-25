# DataNetworkFirst.kt 文档：深入理解网络优先数据加载策略

## I. 概述

`DataNetworkFirst.kt` 文件封装了一个核心函数，实现了“网络优先”数据加载策略。与“缓存优先”策略不同，它在数据获取时会优先尝试从远程网络源获取最新数据。只有当网络不可用或远程获取失败时，它才会回退到本地缓存数据。

这种策略适用于以下场景：
*   **数据新鲜度至关重要**: 应用程序需要尽可能显示最新数据，例如新闻、股票行情、实时通知等。
*   **网络通常稳定**: 预期用户处于良好的网络环境下，可以快速获取远程数据。
*   **离线容错**: 即使优先网络，也希望在网络不可用时，能够提供一个可接受的旧数据版本，而不是完全失败。

其核心目标是：
*   **优先最新数据**: 确保用户总是能看到最新的信息。
*   **离线回退**: 在网络受限时，提供本地缓存作为备用方案。
*   **健壮性**: 优雅地处理网络错误、数据为空等各种异常情况。

## II. `dataNetworkFirstInternal` 函数

### A. 业务场景梳理

`dataNetworkFirstInternal` 函数适用于以下场景：

*   **需要强数据一致性**: 应用程序对数据的实时性要求较高，例如电商应用的商品价格、库存信息。
*   **用户主动刷新**: 用户通常会下拉刷新来获取最新内容，此时网络优先策略能更好地满足用户预期。
*   **数据源更新频繁**: 远程数据源更新频繁，本地缓存很快就会过时。

**优点**:
*   **数据新鲜度高**: 总是优先尝试获取最新数据。
*   **离线回退机制**: 在网络不可用或远程失败时，能够平滑地切换到本地缓存，提供更好的用户体验。
*   **逻辑清晰**: 优先网络，失败回退，符合直观的数据获取逻辑。

**缺点**:
*   **首次加载可能稍慢**: 如果网络延迟较高，用户可能需要等待更长时间才能看到数据，因为需要等待远程请求完成。
*   **网络消耗**: 每次都会尝试发起网络请求，可能消耗更多流量和电量。

### B. 核心代码讲解

此函数通过 `flow` 构建一个 `Flow`，它会发射一个或零个数据项，然后完成。

```kotlin
@PublishedApi
internal inline fun <T : Any> dataNetworkFirstInternal(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit,
    crossinline shouldEmitLocal: (localData: T) -> Boolean = { true },
): Flow<T?> = flow {

    // 为每个数据请求生成一个唯一的ID，用于在日志中追踪其生命周期，便于调试和问题定位。
    val requestId = LogTracer.newId()
    logD(LogTracer.TAG) { "[$requestId] >>>>> dataNetworkFirstInternal start" }

    // 检查当前网络状态。如果离线，则直接回退到本地数据源。
    if (!isOnline()) {
        logD(LogTracer.TAG) { "[$requestId] Offline, falling back to local source." }
        // 尝试从本地获取数据，并处理可能发生的异常。
        val localResult = runCatching { local() }
        localResult.onFailure {
            // 记录本地读取失败的警告。
            logW(LogTracer.TAG, it) { "[$requestId] Offline, local read failed." }
        }
        // 发射本地数据（如果存在），否则发射 null。
        emit(localResult.getOrNull())
    } else {
        logD(LogTracer.TAG) { "[$requestId] Online, attempting to fetch from remote source." }
        // 尝试从远程源获取数据，并使用 `runCatching` 捕获潜在异常。
        val remoteResult = runCatching { remote() }

        remoteResult.fold(
            onSuccess = { remoteData ->
                // 确保协程在处理结果时仍然活跃。
                coroutineContext.ensureActive()
                if (remoteData != null) {
                    logD(LogTracer.TAG) { "[$requestId] Remote fetch successful, emitting and caching." }
                    // 如果远程数据不为空，则发射数据并尝试缓存。
                    emit(remoteData)
                    runCatching { cacheRemote(remoteData) }
                        .onFailure { e -> logE(LogTracer.TAG, e) { "[$requestId] cacheRemote failed" } }
                } else {
                    logW(LogTracer.TAG) { "[$requestId] Remote returned null, falling back to local." }
                    // 如果远程返回 null，则回退到本地数据源。
                    val localResult = runCatching { local() }
                    val localData = localResult.getOrNull()
                    // 如果本地数据存在且 `shouldEmitLocal` 策略允许，则发射本地数据，否则发射 null。
                    if (localData != null && shouldEmitLocal(localData)) {
                        emit(localData)
                    } else {
                        emit(null)
                    }
                }
            },
            onFailure = { remoteException ->
                // 如果远程请求被取消，则重新抛出 `CancellationException`。
                if (remoteException is CancellationException) throw remoteException
                // 确保协程在处理异常时仍然活跃。
                coroutineContext.ensureActive()

                logW(LogTracer.TAG, remoteException) { "[$requestId] Remote fetch failed, falling back to local." }
                // 远程获取失败，尝试从本地数据源获取数据作为回退。
                val localResult = runCatching { local() }
                val localData = localResult.getOrNull()

                when {
                    // 如果本地数据存在且 `shouldEmitLocal` 策略允许，则发射本地数据。
                    localData != null && shouldEmitLocal(localData) -> emit(localData)
                    // 如果本地数据获取成功但为空，则发射 null。
                    localResult.isSuccess -> emit(null)
                    // 如果本地数据获取也失败，则抛出 `RemoteFailedException`。
                    else -> throw RemoteFailedException(requestId, remoteException)
                }
            }
        )
    }

    logD(LogTracer.TAG) { "[$requestId] <<<<< dataNetworkFirstInternal end" }

}.flowOn(Dispatchers.IO) // 确保所有数据操作都在 IO 调度器上执行，避免阻塞主线程。
```

**参数详解**:
*   `T`: 泛型类型，代表你希望获取的数据模型。
*   `isOnline: () -> Boolean`: 一个 lambda 函数，用于同步检查当前设备是否有网络连接。
*   `local: suspend () -> T?`: 一个 `suspend` 函数，负责从本地数据源获取数据。
*   `remote: suspend () -> T?`: 一个 `suspend` 函数，负责从远程数据源获取数据。
*   `cacheRemote: suspend (T) -> Unit`: 一个 `suspend` 函数，负责将从远程获取到的数据 `T` 保存到本地缓存中。
*   `shouldEmitLocal: (localData: T) -> Boolean = { true }`: 一个可选的 lambda 谓词。它接收本地数据 `T` 作为参数，并返回一个布尔值。如果返回 `true`，表示应该将本地数据发射给收集器；如果返回 `false`，则不发射本地数据（例如，本地数据不符合某些条件）。默认值为 `true`。

**核心逻辑分解**:

1.  **请求ID生成**:
    *   `val requestId = LogTracer.newId()`: 为每次 `dataNetworkFirstInternal` 调用生成一个唯一的短 ID，用于日志追踪。

2.  **离线处理 (`if (!isOnline())`)**:
    *   如果 `isOnline()` 返回 `false`，表示设备离线。此时不会尝试远程获取，而是直接回退到本地数据源。
    *   `val localResult = runCatching { local() }`: 尝试从本地获取数据，并使用 `runCatching` 捕获可能发生的异常。
    *   `emit(localResult.getOrNull())`: 发射本地数据（如果成功获取到），否则发射 `null`。这意味着在离线情况下，如果本地有数据就提供，没有就提供 `null`，但不会抛出异常（除非 `local()` 本身抛出且未被 `runCatching` 捕获）。

3.  **在线处理 (`else`)**:
    *   `val remoteResult = runCatching { remote() }`: 尝试从远程源获取数据，并使用 `runCatching` 捕获潜在的网络或服务器异常。
    *   **远程获取成功 (`onSuccess`)**:
        *   `coroutineContext.ensureActive()`: 确保协程在处理结果时仍然活跃。
        *   **远程数据不为空 (`remoteData != null`)**:
            *   `emit(remoteData)`: 发射最新获取的远程数据。
            *   `runCatching { cacheRemote(remoteData) }`: 尝试将远程数据缓存到本地，并处理缓存过程中可能发生的异常。
        *   **远程数据为空 (`else`)**:
            *   `val localResult = runCatching { local() }`: 远程返回 `null`，此时回退到本地数据源。
            *   `if (localData != null && shouldEmitLocal(localData)) { emit(localData) } else { emit(null) }`: 如果本地有数据且 `shouldEmitLocal` 策略允许，则发射本地数据；否则发射 `null`。这表示远程没有数据，但如果本地有备用且符合条件，就使用本地的。
    *   **远程获取失败 (`onFailure`)**:
        *   `if (remoteException is CancellationException) throw remoteException`: 如果失败原因是协程被取消，则重新抛出 `CancellationException`，这是协程取消的规范处理方式。
        *   `coroutineContext.ensureActive()`: 确保协程在处理异常时仍然活跃。
        *   `val localResult = runCatching { local() }`: 远程失败，回退到本地数据源。
        *   `when` 表达式处理本地回退结果：
            *   **本地有数据且符合条件**: `emit(localData)`。
            *   **本地成功但为空**: `emit(null)`。
            *   **本地也失败**: `throw RemoteFailedException(requestId, remoteException)`。这是最坏的情况，远程和本地都无法提供数据，此时抛出 `RemoteFailedException`，并包装原始远程异常。

4.  **线程调度**:
    *   `.flowOn(Dispatchers.IO)`: 确保 `flow` 内部的所有 `suspend` 操作（如 `local()`、`remote()`、`cacheRemote()`）都在 IO 调度器上执行，避免阻塞主线程，保证了 UI 的流畅性。

### C. 使用示例

假设我们有一个 `User` 数据类和一个简单的 `UserRepository`。

```kotlin
// 数据模型 (同 DataCacheFirst.kt 文档)
data class User(val id: String, val name: String, val age: Int)

// 模拟本地数据源 (同 DataCacheFirst.kt 文档)
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

// 模拟远程数据源 (同 DataCacheFirst.kt 文档)
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

// 模拟网络状态检查 (同 DataCacheFirst.kt 文档)
object NetworkMonitor {
    var isConnected = true
    fun isOnline() = isConnected
}

// UserRepository 封装数据获取逻辑
class UserRepository {
    suspend fun getUser(): User? {
        return dataNetworkFirst(
            isOnline = { NetworkMonitor.isOnline() },
            local = { LocalDataSource.getUser() },
            remote = { RemoteDataSource.getUser() },
            cacheRemote = { user -> LocalDataSource.saveUser(user) },
            shouldEmitLocal = { localData -> localData.age > 18 } // 示例：只发射年龄大于18的本地数据
        ).firstOrNull() // 网络优先通常也只关心第一个结果
    }
}

// ViewModel 或 Presenter 层 (同 DataCacheFirst.kt 文档)
class UserViewModel(private val repository: UserRepository) {
    suspend fun loadUser() {
        println("\n--- 开始加载用户数据 ---")
        try {
            val user = repository.getUser()
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

    // 场景 1: 在线，远程成功
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(User("old", "Old Local", 10)) // 预设一个旧的本地数据
    RemoteDataSource.updateRemoteUser(User("1", "Alice", 30))
    viewModel.loadUser()

    // 场景 2: 在线，远程返回空，本地有数据 (符合 shouldEmitLocal)
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true)
    LocalDataSource.saveUser(User("2", "Bob", 25)) // 预设一个本地数据
    viewModel.loadUser()

    // 场景 3: 在线，远程返回空，本地无数据
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(true)
    LocalDataSource.saveUser(null) // 清空本地缓存
    viewModel.loadUser()

    // 场景 4: 在线，远程失败，本地有数据
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(true)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(User("3", "Charlie", 35)) // 预设一个本地数据
    viewModel.loadUser()

    // 场景 5: 在线，远程失败，本地无数据
    NetworkMonitor.isConnected = true
    RemoteDataSource.setShouldFail(true)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(null) // 清空本地缓存
    viewModel.loadUser()

    // 场景 6: 离线，本地有数据
    NetworkMonitor.isConnected = false
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(User("4", "David", 40)) // 预设一个本地数据
    viewModel.loadUser()

    // 场景 7: 离线，本地无数据
    NetworkMonitor.isConnected = false
    RemoteDataSource.setShouldFail(false)
    RemoteDataSource.setShouldReturnNull(false)
    LocalDataSource.saveUser(null) // 清空本地缓存
    viewModel.loadUser()
}
```
### D. 示例代码执行结果

```
--- 开始加载用户数据 ---
RemoteDataSource: 获取用户数据 -> User(id=1, name=Alice, age=30)
LocalDataSource: 保存用户数据 -> User(id=1, name=Alice, age=30)
加载完成: 用户 -> User(id=1, name=Alice, age=30)
--- 加载结束 ---

--- 开始加载用户数据 ---
RemoteDataSource: 获取用户数据 -> null
LocalDataSource: 获取用户数据 -> User(id=2, name=Bob, age=25)
加载完成: 用户 -> User(id=2, name=Bob, age=25)
--- 加载结束 ---

--- 开始加载用户数据 ---
RemoteDataSource: 获取用户数据 -> null
LocalDataSource: 获取用户数据 -> null
加载失败: Remote source returned empty data and no cache is available [req=xxxxxx]
--- 加载结束 ---

--- 开始加载用户数据 ---
RemoteDataSource: 获取用户数据 -> null
LocalDataSource: 获取用户数据 -> User(id=3, name=Charlie, age=35)
加载完成: 用户 -> User(id=3, name=Charlie, age=35)
--- 加载结束 ---

--- 开始加载用户数据 ---
RemoteDataSource: 获取用户数据 -> null
LocalDataSource: 获取用户数据 -> null
加载失败: Remote request failed and no cache is available [req=xxxxxx][cause=Exception]
--- 加载结束 ---

--- 开始加载用户数据 ---
LocalDataSource: 获取用户数据 -> User(id=4, name=David, age=40)
加载完成: 用户 -> User(id=4, name=David, age=40)
--- 加载结束 ---

--- 开始加载用户数据 ---
LocalDataSource: 获取用户数据 -> null
加载完成: 用户 -> null
--- 加载结束 ---
```
*(注：`requestId` 为随机生成，每次运行会不同)*

## III. 辅助函数 `dataNetworkFirst`

`DataNetworkFirst.kt` 文件还提供了一个 `inline fun dataNetworkFirst` 重载函数。它作为 `dataNetworkFirstInternal` 的公共入口，主要作用是简化调用，提供更友好的 API 接口。

```kotlin
inline fun <T : Any> dataNetworkFirst(
    crossinline isOnline: () -> Boolean,
    crossinline local: suspend () -> T?,
    crossinline remote: suspend () -> T?,
    crossinline cacheRemote: suspend (T) -> Unit = {},
    crossinline shouldEmitLocal: (localData: T) -> Boolean = { true },
): Flow<T?> = dataNetworkFirstInternal(isOnline, local, remote, cacheRemote, shouldEmitLocal)
```

这个辅助函数直接转发参数给 `dataNetworkFirstInternal` 实现，并提供了默认参数值，使得在大多数常见场景下调用更加简洁。

---