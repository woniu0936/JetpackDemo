package com.demo.core.crash

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
A utility object to safely retrieve application information like version name and code.
 */
object AppInfoUtils {
    /**
    Retrieves the application's version name.
    @param context The application context.
    @return The version name as a String, or "N/A" if it cannot be retrieved.
     */
    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName ?: "N/A"
    }

    /**
    Retrieves the application's version code, handling API level compatibility.
    On Android P (API 28) and above, it uses the modern longVersionCode.
    On older versions, it falls back to the deprecated versionCode.
    @param context The application context.
    @return The version code as a Long, or 0L if it cannot be retrieved.
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
    A private helper function to safely get the [PackageInfo] for the current app.
    It handles the potential [PackageManager.NameNotFoundException].
    @param context The application context.
    @return The [PackageInfo] object, or null if an error occurs.
     */
    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above, a flag is required.
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") // Suppress warning for older API levels
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
                // This should realistically never happen for the app's own package.
            null
        }
    }
}