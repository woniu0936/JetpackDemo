package com.demo.core.datastore.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

/**
 * User 数据模型。
 *
 * 这是一个稳定的、可序列化的数据类，用于在应用各层之间传递用户信息。
 *
 * @property id 用户唯一标识。
 * @property name 用户名。
 * @property age 用户年龄。
 * @property createTime 记录创建时的时间戳。默认为对象实例化时的当前时间，确保数据从创建起就具有有效的创建时间。
 * @property modifyTime 记录最后修改时的时间戳。默认为创建时间，保证了新建对象的状态一致性。
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class User(
    val id: Int,
    val name: String,
    val age: Int,
    val createTime: Long = System.currentTimeMillis(),
    val modifyTime: Long = createTime
)