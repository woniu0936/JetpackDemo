package com.demo.core.datastore.serializer

import android.os.Parcelable
import androidx.datastore.core.CorruptionException
import com.demo.core.datastore.extensions.FileSerializer
import com.demo.core.datastore.toParcelable
import com.demo.core.datastore.toParcelableByteArray
import java.io.InputStream
import java.io.OutputStream

/**
 * 一个通用的、适用于任意 Parcelable 类型的 DataStore Serializer。
 *
 * @param T 具体的 Parcelable 类型。
 * @param creator 目标 Parcelable 类型的 CREATOR 对象。
 * @param defaultValue 当文件为空或数据损坏时返回的默认实例。
 */
class ParcelableSerializer<T : Parcelable>(
    private val creator: Parcelable.Creator<T>,
    override val defaultValue: T
) : FileSerializer<T> {

    // Parcelable 对象通常存储为二进制文件
    override val suffix: String = ".parcel"

    override suspend fun readFrom(input: InputStream): T {
        return try {
            val bytes = input.readBytes()
            if (bytes.isEmpty()) {
                defaultValue
            } else {
                // 使用我们的工具函数从字节数组中恢复对象
                bytes.toParcelable(creator)
            }
        } catch (t: Throwable) {
            throw CorruptionException("Cannot read Parcelable from DataStore.", t)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        // 使用我们的工具函数将对象转换为字节数组并写入
        output.write(t.toParcelableByteArray())
        output.flush()
    }
}