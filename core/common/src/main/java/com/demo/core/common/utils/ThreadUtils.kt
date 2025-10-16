package com.demo.core.common.utils

import android.os.Looper

/**
 * 确保当前代码块在主线程上执行。
 *
 * 这是一个运行时检查，用于在开发阶段暴露错误的线程调用，保证UI操作的安全性。
 *
 * @param methodName (可选) 调用此方法的方法名，用于生成更精确、更易于排查的错误信息。
 * @throws IllegalStateException 如果当前线程不是主线程。
 *
 * @example
 * fun updateUi() {
 *     ensureOnMainThread("updateUi")
 *     // ... safe to update UI ...
 * }
 */
fun ensureOnMainThread(methodName: String = "") {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        val message = if (methodName.isNotEmpty()) {
            "$methodName() must be called on the main thread."
        } else {
            "This method must be called on the main thread."
        }
        throw IllegalStateException(message + " Current thread: ${Thread.currentThread().name}")
    }
}