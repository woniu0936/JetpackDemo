package com.demo.core.logger

import java.util.concurrent.ConcurrentHashMap

/**
 * [内部工厂] 负责创建和缓存 [Logger] 实例。
 *
 * 这是一个内部使用的单例对象，确保每个唯一的标签只对应一个 [Logger] 实例，
 * 从而优化资源使用并保持日志行为的一致性。
 *
 * @see AppLogger
 * @see Logger
 */
internal object LoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    /**
     * 根据给定的标签获取一个 [Logger] 实例。
     * 如果该标签的 [Logger] 实例已存在于缓存中，则直接返回；否则，创建一个新的实例并缓存。
     *
     * @param tag 用于标识日志来源的字符串标签。
     * @return 对应标签的 [Logger] 实例。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * val myLogger = LoggerFactory.getLogger("MyFeature")
     * myLogger.d { "通过工厂获取的日志器。" }
     * ```
     */
    fun getLogger(tag: String): Logger {
        return loggers.computeIfAbsent(tag) { newTag ->
            val loggerImpl = LoggerImpl(newTag)
            Logger(loggerImpl)
        }
    }

    /**
     * 使用类的简单名称获取一个 [Logger] 实例。
     * 此方法会提取类的简单名称作为日志标签，并调用 [getLogger(tag: String)] 方法。
     *
     * @param clazz 要获取其日志器的类对象。
     * @return 对应类的 [Logger] 实例。
     *
     * @example
     * ```kotlin
     * // 内部使用示例：
     * class MyClass {
     *     companion object {
     *         val logger = LoggerFactory.getLogger(MyClass::class.java)
     *     }
     *     fun doSomething() {
     *         logger.i { "MyClass 正在执行操作。" }
     *     }
     * }
     * ```
     */
    fun getLogger(clazz: Class<*>): Logger {
        return getLogger(clazz.simpleName)
    }
}