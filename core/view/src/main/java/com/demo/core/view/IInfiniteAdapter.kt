package com.demo.core.view

/**
 * 定义了无限循环适配器必须具备的核心能力。
 * 扩展函数将依赖此接口，而不是具体的实现类，以实现多态。
 */
interface IInfiniteAdapter<T> {
    /**
     * 获取真实数据的数量。
     * @return 列表中真实数据项的数量。
     */
    fun getRealCount(): Int

    /**
     * 安全地获取指定真实位置的数据项。
     * @param realPosition 在真实数据列表中的索引。
     * @return 如果位置有效，则返回数据项；否则返回 null。
     */
    fun getRealItem(realPosition: Int): T?

    /**
     * 获取当前真实数据列表的快照。
     * @return 一个包含当前所有真实数据的不可变列表。
     */
    fun getRealData(): List<T>
}