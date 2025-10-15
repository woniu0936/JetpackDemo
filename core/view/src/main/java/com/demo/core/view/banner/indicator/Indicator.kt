package com.demo.core.view.banner.indicator

import android.view.View

/**
 * Banner 指示器的标准接口。
 * 任何自定义指示器都应实现此接口，以与 [com.yourcompany.banner.BannerView] 兼容。
 */
interface Indicator {
    /**
     * 获取指示器的实际视图 [android.view.View] 实例。
     * [com.yourcompany.banner.BannerView] 会将此视图添加到其布局中。
     * @return 指示器的视图。
     */
    fun getIndicatorView(): View

    /**
     * 当 Banner 页面发生变化时，由 [com.yourcompany.banner.BannerView] 调用。
     * 指示器应在此方法中更新其 UI，以反映新的页面状态。
     *
     * @param totalItemCount Banner 中真实数据项的总数。
     * @param currentPosition 当前选中的数据项的真实位置索引。
     */
    fun onPageChanged(totalItemCount: Int, currentPosition: Int)
}