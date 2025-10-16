# 崩溃处理模块设计文档

## 1. 引言：我们解决了什么问题？

在 Android 应用程序开发中，未捕获的异常（即崩溃）是影响用户体验和应用稳定性的主要因素。一个健壮的崩溃处理机制能够：
*   **提升用户体验**：在应用崩溃后，提供友好的提示，避免直接闪退，并引导用户进行反馈。
*   **辅助问题定位**：收集详细的崩溃信息（堆栈、设备信息、日志等），帮助开发者快速定位和修复问题。
*   **实现差异化处理**：在开发（Debug）和发布（Release）版本中采取不同的崩溃处理策略，例如 Debug 版本可能直接抛出异常以方便调试，而 Release 版本则可能静默收集并上传崩溃报告。

本模块旨在提供一个**统一、可配置且生命周期安全**的崩溃处理解决方案，简化开发者集成崩溃报告服务的流程，并确保在不同构建变体下都能灵活应对。

## 2. 核心设计思想

本崩溃处理模块的设计遵循以下核心原则：

*   **门面模式 (Facade Pattern)**：通过 `CrashManager` 单例作为整个崩溃处理系统的统一入口，隐藏内部复杂性，为开发者提供简洁的 API。
*   **建造者模式 (Builder Pattern)**：`CrashConfig` 使用建造者模式，允许开发者以链式调用的方式灵活配置崩溃处理行为，特别是自定义崩溃回调。
*   **面向接口编程 (Programming to an Interface)**：定义 `ICrashHandler` 接口，将崩溃处理的契约与具体实现解耦，便于在不同构建变体下提供不同的实现（例如 `CrashHandlerImpl`）。
*   **生命周期感知 (Lifecycle-Aware)**：虽然 `CrashManager` 本身不直接感知 Android 生命周期，但其内部的 `CrashHandlerImpl` 可以根据需要实现生命周期相关的逻辑（例如在应用启动时注册，在应用退出时清理）。
*   **构建变体差异化 (Build Variant Specific)**：通过 Gradle 的构建变体机制，可以为 Debug 和 Release 版本提供不同的 `CrashHandlerImpl` 实现，从而实现不同的崩溃处理策略。**具体而言，`src/debug` 和 `src/release` 目录分别包含针对调试和发布环境的 `CrashHandlerImpl` 实现，使得在不同阶段可以有针对性地处理崩溃，例如 Debug 版本可能提供更详细的日志和崩溃页面，而 Release 版本则专注于静默收集和上传崩溃报告。**

## 3. 模块结构与核心组件

本模块由以下核心组件构成，各司其职：

*   **`ICrashHandler.kt`**:
    *   **职责**：定义了未捕获异常处理器的核心契约。它继承自 `Thread.UncaughtExceptionHandler`，确保任何实现都能作为线程的默认异常处理器。
    *   **设计**：一个 `internal` 接口，强调其内部实现细节，对外通过 `CrashManager` 暴露功能。

*   **`CrashConfig.kt`**:
    *   **职责**：封装了崩溃处理系统的所有可配置项。目前主要包含一个 `onCrashCallback`，允许开发者自定义崩溃发生时的行为。
    *   **设计**：采用建造者模式 (`CrashConfig.Builder`)，提供流畅的 API 来构建不可变的配置对象。

*   **`CrashManager.kt`**:
    *   **职责**：整个崩溃处理模块的公共门面和单例入口。负责初始化崩溃处理系统，并设置默认的未捕获异常处理器。
    *   **设计**：一个 `object` 单例，确保全局只有一个实例。提供多种 `init` 方法（Kotlin DSL 风格、Java 友好型、简化版），以适应不同开发习惯。内部通过 `synchronized` 块保证初始化过程的线程安全和幂等性。

*   **`CrashHandlerImpl.kt` (构建变体实现)**:
    *   **职责**：`ICrashHandler` 接口的具体实现。它负责在捕获到未处理异常时，执行预定义的逻辑，包括调用 `CrashConfig` 中配置的 `onCrashCallback`。
    *   **设计**：这是一个**构建变体相关**的类。**其具体实现分别位于 `src/debug/kotlin/com/demo/core/crash/CrashHandlerImpl.kt` 和 `src/release/kotlin/com/demo/core/crash/CrashHandlerImpl.kt`。**
        *   **Debug 版本 (`src/debug`)**：通常包含更详细的日志输出、可能启动一个友好的崩溃提示页面 (`CrashActivity.kt`)，并提供崩溃信息分享功能 (`CrashFileProvider.kt`)，以辅助开发和测试。
        *   **Release 版本 (`src/release`)**：通常实现静默的崩溃数据收集（例如，将崩溃堆栈和设备信息保存到本地文件），并可能将数据上传到远程崩溃报告服务，以最小化对用户体验的影响。

## 4. 核心类与方法深度解析

### 4.1 `CrashManager` - 统一入口与初始化

`CrashManager` 是开发者与崩溃处理模块交互的唯一入口。

*   **`fun init(context: Context, block: CrashConfig.Builder.() -> Unit = {})` (Kotlin DSL)**
    *   **作用**：推荐在 Kotlin 项目中使用的初始化方法。通过一个 lambda 表达式，以 DSL 风格配置 `CrashConfig`。
    *   **内部机制**：在内部构建 `CrashConfig` 对象，并调用 `performInitialization`。

*   **`@JvmStatic fun init(context: Context, config: CrashConfig)` (Java 友好)**
    *   **作用**：为 Java 调用者提供的初始化方法，直接传入一个 `CrashConfig` 实例。
    *   **内部机制**：直接调用 `performInitialization`。

*   **`@JvmStatic fun init(context: Context)` (简化版)**
    *   **作用**：使用默认配置初始化崩溃处理系统。
    *   **内部机制**：构建一个空的 `CrashConfig` 对象，并调用 `performInitialization`。

*   **`private fun performInitialization(context: Context, config: CrashConfig)`**
    *   **作用**：实际执行初始化逻辑的核心私有方法。
    *   **代码解析**:
        ```kotlin
        synchronized(lock) {
            if (isInitialized) return // 确保只初始化一次
            val handler = CrashHandlerImpl(context.applicationContext, config) // 实例化具体处理器
            Thread.setDefaultUncaughtExceptionHandler(handler) // 设置为默认未捕获异常处理器
            isInitialized = true
        }
        ```
    *   **关键点**:
        *   **线程安全**：`synchronized(lock)` 确保在多线程环境下，初始化操作只执行一次。
        *   **处理器设置**：将 `CrashHandlerImpl` 实例设置为当前线程组的默认未捕获异常处理器。这意味着任何未被 `try-catch` 捕获的异常都将由 `CrashHandlerImpl` 来处理。

### 4.2 `CrashConfig.Builder` - 灵活的配置

`CrashConfig.Builder` 提供了配置崩溃处理行为的能力。

*   **`fun onCrash(callback: (throwable: Throwable) -> Unit)`**
    *   **作用**：设置一个回调函数，当应用程序发生未捕获异常时，此函数会被调用。
    *   **设计意图**：这是本模块提供给开发者自定义崩溃处理逻辑的主要扩展点。开发者可以在这里实现日志上传、错误报告、应用重启等逻辑。

### 4.3 `ICrashHandler` - 崩溃处理契约

*   **`internal interface ICrashHandler : Thread.UncaughtExceptionHandler`**
    *   **作用**：定义了崩溃处理器的接口。由于继承了 `Thread.UncaughtExceptionHandler`，其实现类必须重写 `uncaughtException` 方法。
    *   **`uncaughtException(t: Thread, e: Throwable)`**:
        *   **作用**：当线程 `t` 发生未捕获异常 `e` 时，系统会调用此方法。
        *   **实现细节**：`CrashHandlerImpl` 会在此方法中执行具体的崩溃处理逻辑，例如记录日志、调用 `CrashConfig` 中设置的 `onCrashCallback`，并可能触发应用重启或退出。

## 5. 核心代码总结

本模块的核心在于 `CrashManager` 对 `ICrashHandler` 的管理和 `CrashConfig` 的灵活配置，以及 `CrashHandlerImpl` 在不同构建变体下的具体实现。

*   **`CrashManager.init()`**: 这是整个模块的启动点。它负责创建 `CrashConfig`（通过 Builder 或 DSL），然后实例化对应构建变体的 `CrashHandlerImpl`，并将其注册为全局的未捕获异常处理器。其内部的 `synchronized` 块确保了初始化过程的原子性和幂等性。
*   **`CrashConfig.Builder.onCrash()`**: 提供了强大的扩展点，允许开发者在应用崩溃时执行自定义逻辑。这个回调是连接崩溃处理模块与外部崩溃报告服务或自定义处理流程的关键。
*   **`CrashHandlerImpl.uncaughtException()`**: 这是实际捕获和处理崩溃的方法。
    *   **Debug 版本**的 `CrashHandlerImpl` 可能包含额外的调试辅助功能，例如：
        *   `AppInfoUtils.kt`：用于收集应用和设备信息，方便调试。
        *   `CrashActivity.kt`：提供一个用户友好的崩溃提示界面，可能包含分享崩溃日志的选项。
        *   `CrashFileProvider.kt`：配合 `CrashActivity` 安全地分享崩溃日志文件。
    *   **Release 版本**的 `CrashHandlerImpl` 则会专注于静默、高效地收集崩溃信息，并可能将其保存到本地或上传到远程服务器，以最小化对用户体验的影响。

## 6. 设计模式运用

*   **门面模式 (Facade Pattern)**：`CrashManager` 作为门面，为复杂的崩溃处理子系统提供了一个简洁的接口。
*   **建造者模式 (Builder Pattern)**：`CrashConfig.Builder` 允许以可读性高、链式调用的方式构建配置对象。
*   **策略模式 (Strategy Pattern)**：`ICrashHandler` 及其不同的 `CrashHandlerImpl` 实现，体现了策略模式。开发者可以通过配置选择不同的崩溃处理策略（例如 Debug 和 Release 版本的不同行为）。

## 7. 优势总结

*   ✅ **统一入口**：`CrashManager` 提供单一入口，简化崩溃处理系统的初始化和管理。
*   ✅ **高度可配置**：通过 `CrashConfig` 及其 Builder，开发者可以灵活自定义崩溃发生时的行为。
*   ✅ **可扩展性强**：`ICrashHandler` 接口允许轻松替换或扩展崩溃处理的具体实现。
*   ✅ **构建变体支持**：能够根据 Debug/Release 版本提供不同的崩溃处理逻辑，满足不同环境的需求。
*   ✅ **线程安全**：初始化过程通过 `synchronized` 保证线程安全。
*   ✅ **易于集成**：提供 Kotlin DSL 和 Java 友好型 API，降低集成成本。