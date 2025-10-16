# 崩溃处理模块快速上手指南

## 1. 简介

本崩溃处理模块提供了一个**统一、可配置且生命周期安全**的解决方案，用于捕获和处理 Android 应用程序中的未捕获异常（崩溃）。它旨在简化崩溃报告服务的集成，并允许在不同构建变体下采取灵活的崩溃处理策略。

### 核心特性:

*   **统一入口**：通过 `CrashManager` 提供简洁的初始化和管理 API。
*   **高度可配置**：允许开发者自定义崩溃发生时的回调逻辑，例如上传日志、报告给第三方服务。
*   **生命周期安全**：自动管理崩溃处理器的注册与注销。
*   **构建变体支持**：可根据 Debug/Release 版本提供不同的崩溃处理行为。
*   **线程安全**：确保初始化过程的健壮性。

## 2. 快速集成

本模块通常作为库集成到 Android 项目中。

### 第一步：添加依赖 (假设已配置 Gradle)

在你的 `app` 模块或其他需要崩溃处理的模块的 `build.gradle.kts` 文件中添加依赖：

```kotlin
dependencies {
    implementation(project(":core:crash"))
    // 如果需要，可以添加第三方崩溃报告库的依赖，例如 Firebase Crashlytics, Bugly 等
    // implementation("com.google.firebase:firebase-crashlytics:x.y.z")
}
```

### 第二步：在 `Application` 类中初始化

在你的 `Application` 类的 `onCreate()` 方法中调用 `CrashManager.init()` 进行初始化。**此方法必须且只能调用一次。**

```kotlin
// MyApplication.kt
import android.app.Application
import com.demo.core.crash.CrashManager
import com.demo.core.logger.AppLogger // 假设你集成了日志模块

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 推荐在 Application.onCreate() 中尽早初始化崩溃处理模块
        CrashManager.init(this) {
            // 在这里配置你的崩溃回调逻辑
            onCrash { throwable ->
                // --- 自定义崩溃处理逻辑 ---
                // 1. 记录崩溃日志 (例如使用你的日志模块)
                AppLogger.e(throwable) { "应用程序发生未捕获异常！" }

                // 2. 将崩溃信息保存到本地文件
                // val crashInfo = CrashInfoCollector.collect(throwable)
                // CrashFileManager.saveCrashLog(crashInfo)

                // 3. 上传崩溃报告到第三方服务 (例如 Bugly, Firebase Crashlytics)
                // FirebaseCrashlytics.getInstance().recordException(throwable)
                // Bugly.postCatchedException(throwable)

                // 4. (可选) 引导用户重启应用或提供反馈
                // val intent = Intent(applicationContext, CrashActivity::class.java)
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // startActivity(intent)

                // 注意：不要在这里执行耗时操作，以免阻塞主线程过久。
                // 如果需要执行耗时操作，请将其放入后台线程。
            }
        }

        // 其他初始化代码...
    }
}
```

## 3. 基础用法

最简单的用法是使用默认配置进行初始化，此时模块会接管未捕获异常，但不会执行任何自定义回调。

```kotlin
// MyApplication.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashManager.init(this) // 使用默认配置初始化
    }
}
```
**注意**：在默认配置下，`CrashManager` 仅会设置一个默认的 `UncaughtExceptionHandler`，其具体行为取决于 `CrashHandlerImpl` 的实现（例如，Debug 版本可能直接让应用崩溃，Release 版本可能静默处理）。为了实现有意义的崩溃报告，**强烈建议**配置 `onCrash` 回调。

## 4. 高级用法：自定义崩溃回调

通过 `onCrash` 回调，你可以完全控制应用程序崩溃时的行为。

### 4.1 Kotlin DSL 风格 (推荐)

```kotlin
// MyApplication.kt
import android.app.Application
import com.demo.core.crash.CrashManager
import com.demo.core.logger.AppLogger
import android.util.Log // Android Logcat

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashManager.init(this) {
            onCrash { throwable ->
                // 打印到 Logcat
                Log.e("CrashHandler", "应用崩溃！", throwable)
                // 使用你的日志模块记录
                AppLogger.e(throwable) { "应用发生致命错误！" }

                // TODO: 在这里添加你的崩溃报告逻辑
                // 例如：
                // 1. 将崩溃信息写入本地文件
                // 2. 上传崩溃报告到服务器
                // 3. 启动一个友好的崩溃提示页面
            }
        }
    }
}
```

### 4.2 Java 风格

```java
// MyApplication.java
import android.app.Application;
import com.demo.core.crash.CrashConfig;
import com.demo.core.crash.CrashManager;
import kotlin.Unit; // Kotlin lambda 在 Java 中需要返回 Unit.INSTANCE

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashConfig config = new CrashConfig.Builder()
            .onCrash(throwable -> {
                System.out.println("App crashed: " + throwable.getMessage());
                // TODO: 在这里添加你的崩溃报告逻辑
                return Unit.INSTANCE;
            })
            .build();
        CrashManager.init(this, config);
    }
}
```

## 5. 注意事项

*   **调用时机**：`CrashManager.init()` 必须在 `Application.onCreate()` 中尽早调用，以确保能够捕获到尽可能多的未捕获异常。
*   **只调用一次**：`CrashManager.init()` 只能调用一次。重复调用将被忽略。
*   **避免耗时操作**：在 `onCrash` 回调中，应避免执行长时间运行的操作，因为此时应用程序处于不稳定状态。如果需要执行耗时操作（如网络上传），请将其放入单独的后台线程或服务中。
*   **默认行为**：如果没有配置 `onCrash` 回调，`CrashManager` 的默认行为将取决于 `CrashHandlerImpl` 的具体实现。在 Release 版本中，通常会尝试静默处理并退出应用，以避免系统弹出“应用程序无响应”对话框。
*   **测试崩溃**：在开发过程中，你可以通过故意制造一个未捕获异常来测试你的崩溃处理逻辑，例如：
    ```kotlin
    // 在某个按钮点击事件中
    findViewById<Button>(R.id.crashButton).setOnClickListener {
        throw RuntimeException("这是一个测试崩溃！")
    }
    ```

## 6. 总结

本崩溃处理模块提供了一个强大而灵活的框架，帮助你有效地管理应用程序的稳定性。通过简单的集成和配置，你可以确保在应用崩溃时，能够收集到关键信息，并提供更好的用户体验。
