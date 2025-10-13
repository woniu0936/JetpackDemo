package com.demo.core.crash

import android.content.Context
import kotlin.system.exitProcess

/**
 * [发布专用] [ICrashHandler] 的发布（release）版本实现。
 * 此实现主要负责在发生未捕获异常时执行配置的回调，然后将控制权交还给默认的异常处理器。
 * 与调试版本不同，发布版本不会保存崩溃报告到文件或启动崩溃显示 Activity，以减少对用户体验的影响和应用体积。
 *
 * @property context 应用程序上下文。
 * @property config 崩溃配置对象，包含崩溃回调等信息。
 *
 * @see ICrashHandler
 * @see CrashManager
 */
internal class CrashHandlerImpl(
    private val context: Context,
    private val config: CrashConfig
) : ICrashHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    /**
     * 处理未捕获的异常。
     * 这是 [Thread.UncaughtExceptionHandler] 接口的实现，当任何线程发生未捕获异常时会被调用。
     * 它会执行配置的回调，然后将异常传递给默认的异常处理器。
     *
     * @param thread 发生未捕获异常的线程。
     * @param throwable 发生的 [Throwable] 异常对象。
     *
     * @example
     * ```kotlin
     * // 此方法由 Android 系统在发生未捕获异常时自动调用，无需手动调用。
     * // 通常在 CrashManager.init 中设置：
     * // Thread.setDefaultUncaughtExceptionHandler(CrashHandlerImpl(context, config))
     * ```
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 在发布版本中，主要执行配置的崩溃回调，例如上传到崩溃收集服务。
            config.onCrashCallback?.invoke(throwable)
        } finally {
            // 总是将控制权交还给默认的异常处理器，以确保系统能够正确处理进程终止。
            defaultHandler?.uncaughtException(thread, throwable)
            // 注意：在发布版本中，通常不在此处强制杀死进程或启动新的 Activity，
            // 而是依赖系统默认行为或崩溃收集服务来处理。
        }
    }
}