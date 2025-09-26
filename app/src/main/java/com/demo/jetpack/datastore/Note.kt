package com.demo.jetpack.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.OutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Note 数据模型。
 *
 * @property createTime 记录创建时的时间戳。默认为对象实例化时的当前时间。
 * @property modifyTime 记录最后修改时的时间戳。默认为创建时间。
 */
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    // [改进] 使用有意义的默认时间戳，确保数据从创建起就是有效的。
    val createTime: Long = System.currentTimeMillis(),
    val modifyTime: Long = createTime
)

/**
 * `Note` 对象的 DataStore Serializer 实现，基于 Gson。
 *
 * 这是一个工业级的、健壮的实现，能够优雅地处理数据演进、I/O 错误和空文件等多种情况。
 */
object NoteSerializer : Serializer<Note> {

    override val defaultValue: Note = Note(id = 0, title = "", content = "")

    /**
     * [改进] 使用 GsonBuilder 创建一个可配置的 Gson 实例。
     * 虽然 Gson 默认会忽略未知的字段，但显式配置或了解其行为是更专业的做法。
     * 未来的复杂配置（如 TypeAdapter）都可以在这里添加。
     */
    private val gson: Gson = GsonBuilder().create()

    /**
     * 从 InputStream 中安全地读取数据并反序列化为 Note 对象。
     */
    override suspend fun readFrom(input: InputStream): Note {
        return try {
            // [改进] 为了可靠地处理空文件，我们先将流读入字节数组。
            // 这牺牲了一点流式解析的性能，但换来了处理空文件场景的绝对健壮性。
            // 对于 DataStore 存储单个对象的场景，文件通常不大，这种权衡是值得的。
            val bytes = input.readBytes()
            if (bytes.isEmpty()) {
                // [关键] 优雅地处理空文件场景，直接返回默认值。
                return defaultValue
            }

            // 使用 InputStreamReader 将字节流转为字符流供 Gson 解析。
            val reader = InputStreamReader(bytes.inputStream(), StandardCharsets.UTF_8)
            gson.fromJson(reader, Note::class.java) ?: defaultValue

        } catch (t: Throwable) {
            // [关键] 捕获所有可能的异常（IO, JsonSyntaxException, etc.），
            // 并将它们统一包装成 CorruptionException，以触发 DataStore 的数据损坏处理机制。
            throw CorruptionException("Cannot read Note from DataStore.", t)
        }
    }

    /**
     * 将 Note 对象序列化并写入 OutputStream，并确保数据持久化。
     */
    override suspend fun writeTo(t: Note, output: OutputStream) {
        // [改进] 为保持逻辑一致和清晰，我们采用“对象 -> 字符串 -> 字节”的路径，并手动 flush。
        // 这种方式与 kotlinx.serialization 和 protobuf 的实现模式完全对齐。
        output.write(
            gson.toJson(t).toByteArray(StandardCharsets.UTF_8)
        )

        // [关键] 显式调用 flush()，确保所有在内存缓冲区的数据被立即写入物理存储。
        output.flush()
    }
}