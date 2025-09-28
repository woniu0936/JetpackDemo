package com.demo.core.datastore.json.gson

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