好的，这是一份为您精心准备的 **`base-library` 日志模块 v1.1.0 使用文档**。

这份文档包含了从集成、配置到高级使用的所有内容，并附带一个完整的 Demo 示例，您可以将其收藏或保存在项目中作为速查手册。

---

## `base-library` 日志模块使用文档 (v1.1.0)

### 一、模块简介

这是一个生产级的、零开销的 Android 日志解决方案。它旨在提供极致的开发期便利性和发布期性能。

#### 核心特性

*   **极简 API**: 通过唯一的工厂 `LoggerFactory.get()` 获取实例，即刻使用。
*   **变种隔离**: Debug 构建包含完整功能（控制台、文件日志、崩溃捕获），Release 构建则为空实现，经 R8 优化后**代码和字符串完全抹除**，对性能和包体积零影响。
*   **每日文件日志**: (仅 Debug) 自动将日志按天写入独立文件（如 `tracker-2024-09-15.log`），方便追溯。
*   **自动崩溃捕获**: (仅 Debug) 自动捕获所有未处理的异常，在应用崩溃前将**所有日志**刷入磁盘，并生成独立的崩溃报告 (`crash-2024-09-15.log`)。
*   **一键日志分享**: (仅 Debug) 提供 API 将近期的日常日志和崩溃日志打包成**标准 ZIP 文件**，通过系统分享菜单轻松发送。

### 二、快速集成与配置

请遵循以下四个步骤将日志模块集成到您的项目中。

#### 步骤 1: 添加 Gradle 依赖

1.  在项目根目录的 `settings.gradle.kts` 中，确保 `base-library` 模块已被 `include`。
    ```kotlin
    include(":app", ":base-library")
    ```

2.  在您的主 App 模块（如 `app/build.gradle.kts`）的 `dependencies` 中添加对 `base-library` 的实现依赖。
    ```kotlin
    dependencies {
        implementation(project(":base-library"))
    }
    ```

#### 步骤 2: 初始化 Library

在你的自定义 `Application` 类的 `onCreate` 方法中，调用 `BaseLibrary.init()` 进行初始化。这是**必须**的步骤，因为它为日志模块提供了全局 `Context`。

**`MyApplication.kt`**
```kotlin
import android.app.Application
import com.base.lib.BaseLibrary

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 base-library
        BaseLibrary.init(this)
    }
}
```*别忘了在 `AndroidManifest.xml` 的 `<application>` 标签中注册你的 `MyApplication`。*

#### 步骤 3: 配置 FileProvider

为了能安全地分享日志文件，需要配置 `FileProvider`。

1.  在 `app/src/main/res/xml/` 目录下创建 `file_paths.xml` 文件。
    **`file_paths.xml`**
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <!-- "logs" 路径必须与 LogFileManager 中定义的一致 -->
        <external-files-path name="logs" path="logs/" />
    </paths>
    ```

2.  在 `app/src/main/AndroidManifest.xml` 中注册 `FileProvider`。
    **`AndroidManifest.xml`**
    ```xml
    <manifest ...>
        <application ...>
            ...
            <provider
                android:name="androidx.core.content.FileProvider"
                android:authorities="${applicationId}.fileprovider"
                android:exported="false"
                android:grantUriPermissions="true">
                <meta-data
                    android:name="android.support.FILE_PROVIDER_PATHS"
                    android:resource="@xml/file_paths" />
            </provider>
            ...
        </application>
    </manifest>
    ```

#### 步骤 4: 添加 ProGuard/R8 规则

为了确保 `FileProvider` 在开启 `fullMode` 的 R8 优化后依然正常工作，请将以下规则添加到你的 App 模块的 `proguard-rules.pro` 文件中。

**`app/proguard-rules.pro`**
```proguard
# 为 R8 fullMode 保留 FileProvider
-keep class androidx.core.content.FileProvider { *; }
-keepresources xml/file_paths
```

---

### 三、API 使用指南

#### 1. 获取 Logger 实例

在你的 Kotlin/Java 类中，通过以下方式获取 `ILogger` 实例：
```kotlin
private val logger = LoggerFactory.get()
```

#### 2. 记录日志

*   **Kotlin (推荐)**: 使用 Lambda 表达式。这样做的好处是，在 Release 包中，Lambda 体内的字符串拼接和对象创建逻辑**永远不会执行**，实现了极致的性能。
    ```kotlin
    val userId = 1001
    val status = "active"

    logger.d("UserProfile") { "User data loaded: id=$userId, status='$status'" }
    logger.i("PaymentFlow") { "Payment process started." }
    logger.w("Network") { "API response took longer than 3s." }
    
    try {
        // ... some risky operation ...
    } catch (e: Exception) {
        logger.e("DataParsing", { "Failed to parse server response" }, e)
    }
    ```

*   **Java**: 调用静态方法。为了兼容 Java 并确保 Release 包的优化效果，请使用 `LoggerFactory` 提供的静态方法。
    ```java
    // import com.base.lib.log.LoggerFactory;
    
    String userId = "user-java-007";
    LoggerFactory.d("LoginJava", "Attempting login for user: " + userId);
    
    try {
        // ...
    } catch (Exception e) {
        LoggerFactory.e("LoginJava", "Login failed", e);
    }
    ```

#### 3. 日志文件管理 (仅 Debug 生效)

所有文件管理功能都通过 `LogFileManager` 对象调用。

*   **分享近期日常日志**: 将最近3天的日常日志打包成 ZIP 文件并分享。
    ```kotlin
    fun onShareDailyLogsClicked() {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        LogFileManager.shareRecentLogs(this, authority, 3)
    }
    ```

*   **分享崩溃报告**: 将最新的崩溃日志和近期的日常日志打包成 ZIP 文件并分享。
    ```kotlin
    fun onShareCrashReportClicked() {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        LogFileManager.shareCrashReport(this, authority)
    }
    ```

*   **手动刷盘 (`flushSync`)**: 在**即将崩溃**或**关键业务失败**时调用，确保内存中的日志被强制写入文件。
    ```kotlin
    fun performCriticalTask() {
        try {
            // ... very important logic ...
        } catch (e: Exception) {
            // 1. 记录下关键的失败日志
            logger.e("CriticalTask", { "A critical task has failed!" }, e)
            
            // 2. 立即将日志刷入磁盘，防止丢失
            LogFileManager.flushSync()
            
            // 3. 上报到远程监控平台
            // Crashlytics.recordException(e)
        }
    }
    ```

---

### 四、完整 Demo 示例

这是一个完整的 `MainActivity`，演示了所有核心功能。

#### 1. 布局文件 (`activity_main.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="16dp">

    <Button android:id="@+id/btnLogInfo" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Log Info Message"/>
    <Button android:id="@+id/btnLogError" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Log Error Message"/>
    <Button android:id="@+id/btnShareLogs" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Share Recent Logs (ZIP)"/>
    <Button android:id="@+id/btnShareCrash" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Share Crash Report (ZIP)"/>
    <Button android:id="@+id/btnForceCrash" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="Force a Crash"/>

</LinearLayout>
```

#### 2. `MainActivity.kt`
```kotlin
package com.your.app

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.base.lib.log.LogFileManager
import com.base.lib.log.LoggerFactory

class MainActivity : AppCompatActivity() {

    // 1. 获取 Logger 实例
    private val logger = LoggerFactory.get()
    private var eventCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. 记录一条生命周期日志
        logger.i("MainActivity") { "onCreate called. Instance state is ${if (savedInstanceState == null) "null" else "not null"}" }

        setupButtons()
    }

    private fun setupButtons() {
        // 获取 authority，推荐在需要时再获取
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"

        findViewById<Button>(R.id.btnLogInfo).setOnClickListener {
            eventCounter++
            logger.d("Interaction") { "Info button clicked. Count: $eventCounter" }
        }

        findViewById<Button>(R.id.btnLogError).setOnClickListener {
            try {
                throw IllegalStateException("This is a simulated error for logging.")
            } catch (e: IllegalStateException) {
                logger.e("Interaction", { "Error button clicked, simulated error caught." }, e)
            }
        }

        findViewById<Button>(R.id.btnShareLogs).setOnClickListener {
            // 3. 分享近期的日常日志
            LogFileManager.shareRecentLogs(this, authority, 3)
        }

        findViewById<Button>(R.id.btnShareCrash).setOnClickListener {
            // 4. 分享崩溃报告
            LogFileManager.shareCrashReport(this, authority)
        }
        
        findViewById<Button>(R.id.btnForceCrash).setOnClickListener {
            // 5. 模拟一个未捕获的异常来测试崩溃报告
            logger.w("CRITICAL") { "About to force a crash intentionally..." }
            throw RuntimeException("This is a deliberate crash to test the CrashFileTree handler.")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        logger.i("MainActivity") { "onDestroy called." }
    }
}```

#### 3. `MyApplication.kt` (含崩溃处理器)

为了让崩溃捕获生效，`CrashFileTree` 会自动注册。你无需在 `Application` 类中编写额外的崩溃处理代码，`base-library` 的 `debug` 版本已经为你处理好了。你只需要确保 `BaseLibrary.init(this)` 被调用即可。

**一句话总结：按照本文档配置后，您将拥有一个在 Debug 时功能强大、在 Release 时“隐形”的日志系统，可直接用于任何生产项目。**