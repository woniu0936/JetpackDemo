package com.demo.core.crash

/**
 * 崩溃处理程序实现的内部接口。
 * 此接口定义了未捕获异常处理器的契约，允许在调试（debug）和发布（release）构建中实现不同的行为。
 *
 * 实现此接口的类将作为 [Thread.UncaughtExceptionHandler] 的默认处理器，
 * 负责在应用程序发生未捕获异常时执行自定义逻辑。
 *
 * @see CrashManager
 * @see CrashHandlerImpl
 */
internal interface ICrashHandler : Thread.UncaughtExceptionHandler
