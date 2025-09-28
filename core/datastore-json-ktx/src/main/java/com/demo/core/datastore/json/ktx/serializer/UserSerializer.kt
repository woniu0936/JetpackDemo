package com.demo.core.datastore.json.ktx.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.demo.core.datastore.json.ktx.User
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * `User` 对象的 DataStore Serializer 实现。
 *
 * 负责将 `User` 对象与 `InputStream` / `OutputStream` 进行相互转换，
 * 实现了高效、健壮且向前兼容的数据持久化逻辑。
 */
object UserSerializer : Serializer<User> {

    /**
     * 当 DataStore 文件不存在或数据损坏无法解析时，提供的默认 `User` 对象。
     * 这确保了即使在初始状态或发生错误时，应用也能获得一个有效的、非空的 User 对象。
     */
    override val defaultValue: User = User(id = 0, name = "", age = 0)

    /**
     * 一个经过特殊配置的、可复用的 `Json` 实例，用于序列化和反序列化。
     * 这个实例被配置为容错模式，以应对 App 版本迭代和数据变化。
     */
    private val json = Json {
        // 忽略在 JSON 中存在但在数据类中不存在的字段。
        // 这对于向前兼容至关重要：当新版 App 读取由旧版 App 生成的数据时，
        // 如果旧版数据多了某些字段，此配置可防止应用崩溃。
        ignoreUnknownKeys = true

        // 强制将输入值转换为目标类型（如果可能），例如将 null 转换为默认值。
        // 这对于向后兼容很有帮助：当旧版 App 读取新版数据时，
        // 如果新版数据缺少某些字段，此配置可帮助使用数据类中的默认值，而不是抛出异常。
        coerceInputValues = true

        // 在生产环境中禁用 prettyPrint，以节省存储空间。
        prettyPrint = false
    }

    /**
     * 从 InputStream 中读取数据并反序列化为 User 对象。
     *
     * 此实现非常健壮，能够处理 I/O 错误、序列化错误和空文件等多种边界情况。
     *
     * 重要：此函数绝不关闭传入的 `input` stream，因为其生命周期由 DataStore 框架管理。
     */
    override suspend fun readFrom(input: InputStream): User {
        // [最终改进] 将所有可能失败的操作（包括 I/O 和解析）都置于一个 try-catch 块中，
        // 以确保任何 Throwable 都能被统一包装成 CorruptionException。
        return try {
            val bytes = input.readBytes()

            // 优雅地处理文件为空的场景，直接返回默认值。
            // 这是一个常见的初始状态，不应被视为错误。
            if (bytes.isEmpty()) {
                defaultValue
            } else {
                // Json 是 StringFormat，所以我们必须先将字节解码为 UTF-8 字符串，
                // 然后再让 Json 从字符串中解析对象。
                json.decodeFromString(
                    User.serializer(),
                    bytes.decodeToString()
                )
            }
        } catch (t: Throwable) {
            // [关键] 捕获所有可能的异常（IO, SerializationException, etc.），
            // 并将它们包装成 CorruptionException。
            // 这使得 DataStore 的数据损坏处理机制能够被触发，从而进行数据恢复或迁移。
            throw CorruptionException("Cannot read User from DataStore.", t)
        }
    }

    /**
     * 将 User 对象序列化并写入 OutputStream。
     *
     * 重要：此函数绝不关闭传入的 `output` stream，因为其生命周期由 DataStore 框架管理。
     */
    override suspend fun writeTo(t: User, output: OutputStream) {
        //  Json 是 StringFormat，所以我们先将对象编码为字符串，
        // 然后再将字符串编码为 UTF-8 字节并写入输出流。
        output.write(
            json.encodeToString(User.serializer(), t).encodeToByteArray()
        )

        // [关键] 显式调用 flush()，确保所有在内存缓冲区的数据被立即写入物理存储。
        // 这可以防止在应用进程被意外终止时，发生数据丢失。
        output.flush()
    }
}