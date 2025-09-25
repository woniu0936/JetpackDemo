# Exceptions.kt 文档

`Exceptions.kt` 文件定义了一系列用于数据加载过程中业务异常的密封类（`sealed class`），旨在提供清晰、可编程的错误处理机制。这些异常都继承自 `RuntimeException`，以便与 Kotlin 协程的错误处理最佳实践保持一致。

## `InitialDataLoadException` (密封类)

这是所有初始数据加载业务异常的基类。

### 特性

*   **密封类**: 确保所有可能的初始加载失败类型都在此文件中定义，从而可以在 `when` 表达式中进行详尽的处理，提高代码的健壮性。
*   **继承 `RuntimeException`**: 允许调用者使用 `Flow.catch` 操作符或 `runCatching` 优雅地、选择性地处理这些预期的业务异常，而无需强制使用 `try-catch` 块。
*   **`requestId`**: 每个异常都包含一个唯一的请求 ID，用于日志记录和追踪，方便问题定位。

## `NetworkUnavailableException`

当设备离线且没有可用的本地缓存作为回退时抛出。

### 业务场景

*   `local()` 返回 `null`（或抛出异常）。
*   **并且** `isOnline()` 返回 `false`。

### 特点

*   代表由环境因素（网络）引起的典型瞬时故障。
*   通常是可重试的。

## `RemoteEmptyException`

当远程获取成功但返回空结果，且没有可用的本地缓存时抛出。

### 业务场景

*   `local()` 返回 `null`（或抛出异常）。
*   **并且** `isOnline()` 返回 `true`。
*   **并且** `remote()` 成功执行但返回 `null`。

### 特点

*   这不是技术故障，而是业务层面的“未找到”状态（例如，查询不存在的用户 ID）。
*   这种情况通常不可重试，因为数据源本身就缺少数据。

## `RemoteFailedException`

当远程数据获取因技术原因（例如，服务器 5xx 错误、网络超时、DNS 问题）失败，且没有可用的本地缓存时抛出。

### 业务场景

*   `local()` 返回 `null`（或抛出异常）。
*   **并且** `isOnline()` 返回 `true`。
*   **并且** `remote()` 抛出异常（例如，IOException, HttpException）。

### 特点

*   表示与远程服务器通信期间的技术故障。
*   通常是瞬态且可重试的。
