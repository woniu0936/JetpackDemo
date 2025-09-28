package com.demo.core.datastore

import android.os.Parcel
import android.os.Parcelable

/**
 * [核心工具] 将一个 Parcelable 对象转换为 ByteArray。
 *
 * @return 包含 Parcelable 数据的字节数组。
 */
fun <T : Parcelable> T.toParcelableByteArray(): ByteArray {
    // 创建一个 Parcel 对象，它是一个可以写入数据的内存缓冲区
    val parcel = Parcel.obtain()
    try {
        // 使用 writeToParcel 方法将对象的状态写入 Parcel
        this.writeToParcel(parcel, 0)
        // 将 Parcel 中的所有数据转换为一个字节数组
        return parcel.marshall()
    } finally {
        // 必须回收 Parcel 对象以避免内存泄漏
        parcel.recycle()
    }
}

/**
 * [核心工具] 将一个 ByteArray 转换回 Parcelable 对象。
 *
 * @param creator 目标 Parcelable 类型的 CREATOR 对象，用于从 Parcel 中重建实例。
 * @return 重建后的 Parcelable 对象实例。
 */
fun <T : Parcelable> ByteArray.toParcelable(creator: Parcelable.Creator<T>): T {
    // 创建一个 Parcel 对象
    val parcel = Parcel.obtain()
    try {
        // 将字节数组的数据“解封”到 Parcel 中
        parcel.unmarshall(this, 0, this.size)
        // 将 Parcel 的读取位置重置到开头
        parcel.setDataPosition(0)
        // 使用 CREATOR 从 Parcel 中创建对象实例
        return creator.createFromParcel(parcel)
    } finally {
        // 回收 Parcel 对象
        parcel.recycle()
    }
}