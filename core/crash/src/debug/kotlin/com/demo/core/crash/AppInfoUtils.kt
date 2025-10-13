package com.demo.core.crash

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * `AppInfoUtils` 是一个工具对象，用于安全地检索应用程序信息，例如版本名称和版本代码。
 * 此工具类主要在调试（debug）版本中使用，以获取应用的相关信息用于崩溃报告或调试输出。
 */
object AppInfoUtils {
    /**
     * 检索应用程序的版本名称。
     *
     * @param context 应用程序上下文。
     * @return 应用程序的版本名称字符串，如果无法检索则返回 "N/A"。
     *
     * @example
     * ```kotlin
     * val versionName = AppInfoUtils.getVersionName(applicationContext)
     * println("应用版本名称: $versionName") // 输出: 应用版本名称: 1.0.0
     * ```
     */
    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName ?: "N/A"
    }

    /**
     * 检索应用程序的版本代码，并处理 API 级别兼容性。
     * 在 Android P (API 28) 及更高版本上，它使用现代的 `longVersionCode`。
     * 在旧版本上，它回退到已弃用的 `versionCode`。
     *
     * @param context 应用程序上下文。
     * @return 应用程序的版本代码（Long 类型），如果无法检索则返回 0L。
     *
     * @example
     * ```kotlin
     * val versionCode = AppInfoUtils.getVersionCode(applicationContext)
     * println("应用版本代码: $versionCode") // 输出: 应用版本代码: 10001
     * ```
     */
    fun getVersionCode(context: Context): Long {
        val packageInfo = getPackageInfo(context) ?: return 0L
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") // Suppress warning for older API levels
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * 一个私有的辅助函数，用于安全地获取当前应用程序的 [PackageInfo]。
     * 它处理潜在的 [PackageManager.NameNotFoundException] 异常。
     *
     * @param context 应用程序上下文。
     * @return [PackageInfo] 对象，如果发生错误则返回 `null`。
     */
    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 对于 Android 13 (API 33) 及更高版本，需要一个标志。
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") // Suppress warning for older API levels
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
                // 实际上，对于应用程序自己的包，这不应该发生。
            null
        }
    }
}