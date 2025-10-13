package com.demo.core.logger

import androidx.core.content.FileProvider

/**
 * `LogFileProvider` 是一个自定义的 [FileProvider] 实现，用于安全地分享日志文件。
 *
 * 在 Android N (API 24) 及更高版本中，直接使用 `file://` URI 共享文件会抛出 `FileUriExposedException`。
 * [FileProvider] 提供了一种安全的方式，通过生成 `content://` URI 来共享文件，
 * 从而避免了文件路径的直接暴露。
 *
 * 此 `FileProvider` 需要在 `AndroidManifest.xml` 中进行配置，指定其授权和可访问的路径。
 *
 * @see androidx.core.content.FileProvider
 * @see AppLogger.shareRecentLogs
 *
 * @example
 * ```xml
 * <!-- AndroidManifest.xml 配置示例 -->
 * <manifest>
 *     ...
 *     <application>
 *         ...
 *         <provider
 *             android:name="com.demo.core.logger.LogFileProvider"
 *             android:authorities="${applicationId}.log.fileprovider"
 *             android:exported="false"
 *             android:grantUriPermissions="true">
 *             <meta-data
 *                 android:name="android.support.FILE_PROVIDER_PATHS"
 *                 android:resource="@xml/log_file_paths" />
 *         </provider>
 *         ...
 *     </application>
 * </manifest>
 *
 * <!-- res/xml/log_file_paths.xml 配置示例 -->
 * <paths xmlns:android="http://schemas.com/apk/res/android">
 *     <files-path name="log_files" path="logs/" />
 * </paths>
 * ```
 */
class LogFileProvider : FileProvider()