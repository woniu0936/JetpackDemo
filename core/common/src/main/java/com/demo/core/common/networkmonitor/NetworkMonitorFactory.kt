package com.novel.app.app.networkmonitor

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineScope

object NetworkMonitorFactory {

    @JvmStatic
    fun create(context: Context, scope: CoroutineScope): NetworkMonitor {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkMonitorImplApi24(context, scope)
        } else {
            NetworkMonitorImplApi21(context, scope)
        }
    }
}
