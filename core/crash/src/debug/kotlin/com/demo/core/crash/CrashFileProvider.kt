package com.demo.core.crash

import androidx.core.content.FileProvider

/**
 * `CrashFileProvider` 是一个自定义的 [FileProvider] 实现，用于安全地分享崩溃报告文件。
 *
 * 在 Android N (API 24) 及更高版本中，直接使用 `file://` URI 共享文件会抛出 `FileUriExposedException`。
 * [FileProvider] 提供了一种安全的方式，通过生成 `content://` URI 来共享文件，
 * 从而避免了文件路径的直接暴露。
 *
 * 此 `FileProvider` 需要在 `AndroidManifest.xml` 中进行配置，指定其授权和可访问的路径。
 * 此 `FileProvider` 仅在调试（debug）版本中启用。
 *
 * @see androidx.core.content.FileProvider
 * @see CrashHandlerImpl.shareCrashReport
 *
 * @example
 * ```xml
 * <!-- AndroidManifest.xml 配置示例 -->
 * <manifest>
 *     ...
 *     <application>
 *         ...
 *         <provider
 *             android:name="com.demo.core.crash.CrashFileProvider"
 *             android:authorities="${applicationId}.crash.fileprovider"
 *             android:exported="false"
 *             android:grantUriPermissions="true">
 *             <meta-data
 *                 android:name="android.support.FILE_PROVIDER_PATHS"
 *                 android:resource="@xml/crash_file_paths" />
 *         </provider>
 *         ...
 *     </application>
 * </manifest>
 *
 * <!-- res/xml/crash_file_paths.xml 配置示例 -->
 * <paths xmlns:android="http://schemas.android.com/apk/res/android">
 *     <cache-path name="crash_reports" path="crash_reports/" />
 * </paths>
 * ```
 */
class CrashFileProvider : FileProvider()