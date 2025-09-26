package com.demo.jetpack.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream


/**
 * `Task` 对象的 DataStore Serializer 实现，基于 Protobuf。
 *
 * 这是一个工业级的、教科书式的实现，充分利用了 Protobuf 的健壮性和性能，
 * 并遵循了 DataStore 的所有最佳实践。
 */
object TaskSerializer : Serializer<Task> {

    /**
     * 当 DataStore 文件损坏或首次创建时，返回的默认实例。
     * 直接使用 Protobuf 生成的单例 `defaultInstance`。
     */
    override val defaultValue: Task = Task.getDefaultInstance()

    /**
     * 从 InputStream 中高效、安全地读取并解析 Task 对象。
     */
    override suspend fun readFrom(input: InputStream): Task =
        try {
            // [最终优化] Protobuf 的 parseFrom 方法本身就能高效处理流，
            // 且在遇到空输入流时，会直接返回 `getDefaultInstance()`，与我们的 `defaultValue` 是同一个单例。
            // 因此无需任何额外的判空或字节数组转换。
            Task.parseFrom(input)
        } catch (t: Throwable) {
            // [最终优化] 捕获所有可能的异常（包括 InvalidProtocolBufferException 和底层的 IOException），
            // 将它们统一包装成 CorruptionException，以触发 DataStore 的数据损坏处理机制。
            throw CorruptionException("Cannot read proto.", t)
        }

    /**
     * 将 Task 对象高效地写入 OutputStream，并确保数据持久化。
     */
    override suspend fun writeTo(t: Task, output: OutputStream) {
        // 直接调用 Protobuf 生成的 writeTo 方法，它会处理所有写入逻辑。
        // 绝不关闭由 DataStore 管理的 output 流。
        t.writeTo(output)

        // 确保所有在内存缓冲区的数据被立即写入物理存储，防止数据丢失。
        output.flush()
    }
}